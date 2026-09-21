package com.spotkerja.sense

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/** Snapshot metrik live untuk UI selama scan berjalan. */
data class LiveMetrics(
    val wifiRssiDbm: Int? = null,
    val pingMs: Float? = null,
    val lux: Float? = null,
    val noiseDb: Float? = null,
    val azimuthDeg: Float? = null,
    val cellularDbm: Int? = null,
    val wifiBand: String? = null,
    val linkSpeedMbps: Int? = null,
)

/** Akumulator sampel selama satu scan spot. */
class ScanAccumulator {
    val rssi = mutableListOf<Int>()
    val pings = mutableListOf<Float>()
    var pingAttempts = 0
    val lux = mutableListOf<Float>()
    val noise = mutableListOf<Float>()
    var azimuthDeg: Float? = null
    var linkSpeed: Int? = null
    var wifiBand: String? = null
    var cellularDbm: Int? = null
    var luxAtMaxAzimuth: Pair<Float, Float>? = null // lux to azimuth pairing
    var lastAzimuth: Float? = null
    var wifiCongestion: Int? = null
    var pingTargetMissing: Boolean = false

    // --- Mesh / radio ---
    var ssid: String? = null
    var bssid: String? = null
    var channelWidthMhz: Int? = null
    var meshApCount: Int? = null
    var roamCount: Int = 0
    private var lastBssid: String? = null
    var txLink: Int? = null
    var rxLink: Int? = null
    var internetState: String? = null
    private val _routeHops = mutableListOf<String>()
    val routeHops: List<String> get() = synchronized(_routeHops) { _routeHops.toList() }
    var routeTarget: String? = null
    fun setHops(h: List<String>) = synchronized(_routeHops) { _routeHops.clear(); _routeHops.addAll(h) }
    fun addHop(h: String) = synchronized(_routeHops) { _routeHops += h }

    // --- Sound events ---
    val soundCounts = mutableMapOf<String, Int>()

    // --- Environment ---
    var pressureHpa: Float? = null
    var humidityPct: Float? = null
    var ambientTempC: Float? = null
    var magneticUt: Float? = null
    val magValues = mutableListOf<Float>()
    private var stepBase: Float? = null
    var stepsDuringScan: Int? = null
    var sensorsFound: List<String> = emptyList()

    // --- Network diagnostics ---
    var dnsMs: Float? = null
    var tcpMs: Float? = null
    var tcpPort: Int? = null
    var tlsMs: Float? = null
    var udpState: String? = null

    // --- Speech distraction (dari YAMNet windows) ---
    var speechWindows: Int = 0
    var soundWindows: Int = 0
    @Synchronized fun onSoundWindow(speech: Boolean) {
        soundWindows++; if (speech) speechWindows++
    }

    // --- Camera light map ---
    var camLumaAvg: Float? = null
    var camLumaStd: Float? = null
    var camHotspot: String? = null
    var lightMap: List<Int> = emptyList()

    // --- Thermal ---
    var thermalStatus: Int? = null
    var thermalHeadroom: Float? = null
    var batteryTempC: Float? = null

    @Synchronized fun addRssi(v: Int) { rssi += v }
    @Synchronized fun addPing(v: Float?) { pingAttempts++; v?.let { pings += it } }

    /** Perubahan BSSID saat scan = roaming antar AP mesh. */
    @Synchronized fun trackBssid(b: String?) {
        if (b == null || b == "02:00:00:00:00:00") return
        if (lastBssid != null && lastBssid != b) roamCount++
        lastBssid = b
        bssid = b
    }

    @Synchronized fun addSound(label: String) {
        soundCounts[label] = (soundCounts[label] ?: 0) + 1
    }

    @Synchronized fun onStepCounter(v: Float) {
        if (stepBase == null) stepBase = v
        stepsDuringScan = (v - (stepBase ?: v)).toInt()
    }

    @Synchronized fun topSounds(n: Int = 3): List<String> =
        soundCounts.entries.sortedByDescending { it.value }.take(n).map { it.key }

    @Synchronized fun addLux(v: Float) {
        lux += v
        lastAzimuth?.let { az -> if (luxAtMaxAzimuth == null || v > luxAtMaxAzimuth!!.first) luxAtMaxAzimuth = v to az }
    }
    @Synchronized fun addNoise(v: Float) { noise += v }
    @Synchronized fun setAzimuth(v: Float) { azimuthDeg = v; lastAzimuth = v }
    @Synchronized fun setLink(speed: Int?, band: String?) { linkSpeed = speed; wifiBand = band }
    @Synchronized fun setCell(v: Int?) { if (v != null) cellularDbm = v }

    @Synchronized fun packetLossPct(): Float? =
        if (pingAttempts == 0) null else (pingAttempts - pings.size) * 100f / pingAttempts

    @Synchronized fun snapshot() = Triple(rssi.toList(), pings.toList(), lux.toList())
    @Synchronized fun noiseSnapshot() = noise.toList()
}

fun List<Float>.avgOrNull() = if (isEmpty()) null else sum() / size
fun List<Float>.stdDevOrNull(): Float? {
    if (isEmpty()) return null
    if (size < 2) return 0f
    val m = avgOrNull() ?: return null
    return sqrt(map { (it - m).pow(2) }.sum() / size)
}
fun List<Int>.avgOrNullI() = if (isEmpty()) null else sum() / size

/** Sampler Wi-Fi RSSI dari koneksi aktif (hemat baterai, ~1 Hz). */
class WifiSampler(private val ctx: Context) {
    private val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun sample(acc: ScanAccumulator): Int? {
        val info = runCatching { wifi?.connectionInfo }.getOrNull() ?: return null
        val rssi = info.rssi.takeIf { it in -100..0 }
        if (rssi != null) {
            acc.addRssi(rssi)
            val band = if (android.os.Build.VERSION.SDK_INT >= 30) {
                when (info.frequency) {
                    in 2400..2500 -> "2.4 GHz"
                    in 5000..5900 -> "5 GHz"
                    in 5925..7125 -> "6 GHz"
                    else -> null
                }
            } else null
            acc.setLink(info.linkSpeed.takeIf { it > 0 }, band)
            // Mesh tracking: SSID + BSSID + link speed TX/RX (API 29+).
            acc.ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"")
                ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            acc.trackBssid(info.bssid)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                acc.txLink = info.txLinkSpeedMbps.takeIf { it > 0 }
                acc.rxLink = info.rxLinkSpeedMbps.takeIf { it > 0 }
            }
        }
        return rssi
    }
}

/** Ping ke host terkonfigurasi (default: gateway Wi-Fi — tetap bekerja tanpa internet). */
class PingSampler(private val ctx: Context, private val host: String? = null) {

    fun target(): String? = host?.takeIf { it.isNotBlank() } ?: gatewayIp()

    fun gatewayIp(): String? {
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val dhcp = runCatching { wifi?.dhcpInfo }.getOrNull() ?: return null
        val gw = dhcp.gateway
        if (gw == 0) return null
        return "%d.%d.%d.%d".format(gw and 0xff, gw shr 8 and 0xff, gw shr 16 and 0xff, gw shr 24 and 0xff)
    }

    /** Satu ping via binary /system/bin/ping; return RTT ms atau null (loss).
     *  Jika tidak ada target sama sekali, attempt tidak dihitung (bukan loss). */
    suspend fun pingOnce(acc: ScanAccumulator): Float? = withContext(Dispatchers.IO) {
        val t = target()
        if (t == null) return@withContext null
        val rtt = runCatching {
            val p = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "2", t)
                .redirectErrorStream(true).start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            p.waitFor()
            Regex("time=([0-9.]+)\\s*ms").find(out)?.groupValues?.get(1)?.toFloat()
        }.getOrNull()
        acc.addPing(rtt)
        rtt
    }
}

/** Listener sensor cahaya + rotasi (azimuth) selama scan — tiap sumber bisa dimatikan. */
class AmbientSampler(
    ctx: Context,
    private val acc: ScanAccumulator,
    private val wantLight: Boolean = true,
    private val wantRotation: Boolean = true,
) : SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val light = if (wantLight) sm.getDefaultSensor(Sensor.TYPE_LIGHT) else null
    private val rotation = if (wantRotation)
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    else null

    private val rotMat = FloatArray(9)
    private val orient = FloatArray(3)

    fun hasLightSensor() = light != null
    fun hasAny() = light != null || rotation != null

    fun start() {
        light?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        rotation?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_LIGHT -> acc.addLux(e.values[0])
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotMat, e.values)
                SensorManager.getOrientation(rotMat, orient)
                var az = Math.toDegrees(orient[0].toDouble()).toFloat()
                if (az < 0) az += 360f
                acc.setAzimuth(az)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

/** Sampler noise via mikrofon — dBFS dikonversi ke level relatif (estimasi).
 *  PCM yang sama ikut diumpankan ke SoundClassifier bila tersedia. */
class NoiseSampler(private val ctx: Context, private val acc: ScanAccumulator,
                   private val classifier: SoundClassifier? = null) {
    private var record: AudioRecord? = null
    private var job: Job? = null

    fun permitted() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (!permitted()) return
        val rate = 16000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = runCatching {
            AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        }.getOrNull() ?: return
        if (rec.state != AudioRecord.STATE_INITIALIZED) return
        record = rec
        rec.startRecording()
        job = scope.launch(Dispatchers.IO) {
            val buf = ShortArray(rate / 5) // 200 ms
            while (true) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    var sum = 0.0
                    for (i in 0 until n) sum += buf[i] * buf[i]
                    val rms = sqrt(sum / n)
                    val dbfs = if (rms > 0) 20 * log10(rms / 32767.0) else -100.0
                    // dBFS(-60..-10) → level relatif ~30..80. Estimasi, bukan SPL terkalibrasi.
                    acc.addNoise((dbfs + 90).toFloat().coerceIn(0f, 120f))
                    classifier?.onPcm(buf, n, acc)
                }
                delay(50)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        record?.runCatching { stop(); release() }
        record = null
    }
}

/** Crowding Wi-Fi: jumlah AP lain pada kanal/band yang sama (perlu izin lokasi). */
class WifiScanSampler(private val ctx: Context) {
    private val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /** Satu scan pass — panggil sekali di awal scan spot (hasil async tapi cache cukup).
     *  Juga mengumpulkan detail radio: kanal lebar, AP mesh dengan SSID sama. */
    fun radio(acc: ScanAccumulator): Int? {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val w = wifi ?: return null
        @SuppressLint("MissingPermission")
        val results = runCatching {
            w.startScan()
            w.scanResults
        }.getOrNull() ?: return null
        val mine = runCatching { w.connectionInfo }.getOrNull() ?: return null
        val myFreq = if (android.os.Build.VERSION.SDK_INT >= 30) mine.frequency else -1
        if (myFreq <= 0) return null
        // Kanal padat: AP lain dalam ±30 MHz dari frekuensi sendiri.
        acc.wifiCongestion = results.count {
            it.BSSID != mine.bssid && kotlin.math.abs(it.frequency - myFreq) <= 30
        }
        // Mesh: BSSID lain yang menyiarkan SSID yang sama.
        val mySsid = mine.ssid?.removePrefix("\"")?.removeSuffix("\"")
        if (!mySsid.isNullOrBlank() && mySsid != "<unknown ssid>") {
            acc.meshApCount = results.count {
                it.BSSID != mine.bssid && it.SSID == mySsid
            }
        }
        // Lebar kanal dari ScanResult milik BSSID yang sedang terhubung.
        results.firstOrNull { it.BSSID == mine.bssid }?.let { sr ->
            acc.channelWidthMhz = when (sr.channelWidth) {
                android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> 20
                android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
                else -> null
            }
        }
        return acc.wifiCongestion
    }
}

/** Internet readiness: divalidasi sistem / captive portal / terbatas / mati. */
class InternetChecker(private val ctx: Context) {
    fun check(acc: ScanAccumulator): String? {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val state = when {
            caps == null -> "none"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) -> "captive"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "ok"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "limited"
            else -> "none"
        }
        acc.internetState = state
        return state
    }
}

/** Traceroute murah: ping dengan TTL menaik, parse balasan ICMP TTL-exceeded.
 *  Berjalan sekali per scan di thread IO — bukan untuk UI live. */
class RouteTracer {
    suspend fun trace(acc: ScanAccumulator, target: String?) = withContext(Dispatchers.IO) {
        val t = target ?: return@withContext
        acc.routeTarget = t
        for (ttl in 1..8) {
            val out = runCatching {
                val p = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "1", "-t", "$ttl", t)
                    .redirectErrorStream(true).start()
                val s = BufferedReader(InputStreamReader(p.inputStream)).readText()
                p.waitFor()
                s
            }.getOrNull() ?: break
            // Format toybox/iputils: "From 1.2.3.4 ... Time to live exceeded" atau
            // langsung "... time=0.42 ms" bila hop menjawab.
            val ip = Regex("(?i)from\\s+([0-9a-f.:]+)").find(out)?.groupValues?.get(1)
            val rtt = Regex("time=([0-9.]+)\\s*ms").find(out)?.groupValues?.get(1)
            val reached = out.contains("bytes from", ignoreCase = true) &&
                !out.contains("exceeded", ignoreCase = true)
            when {
                reached -> {
                    acc.addHop(if (rtt != null) "$ip · ${rtt} ms" else (ip ?: t)); break }
                ip != null -> acc.addHop(if (rtt != null) "$ip · ${rtt} ms" else ip)
                else -> { /* hop diam saja — lanjut TTL berikutnya */ }
            }
            if (acc.routeHops.size >= 8) break
        }
    }
}

/** Sensor lingkungan yang jarang dimanfaatkan: tekanan, kelembapan, suhu ambient,
 *  medan magnet, step counter. Semua pasif — auto-discovery via SensorManager. */
class EnvSampler(ctx: Context, private val acc: ScanAccumulator) : SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val ctxA = ctx.applicationContext

    private val watched = mapOf(
        Sensor.TYPE_PRESSURE to "Barometer",
        Sensor.TYPE_RELATIVE_HUMIDITY to "Humidity",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "Ambient temp",
        Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer",
        Sensor.TYPE_STEP_COUNTER to "Step counter",
        Sensor.TYPE_LIGHT to "Light",
        Sensor.TYPE_PROXIMITY to "Proximity",
        Sensor.TYPE_GYROSCOPE to "Gyroscope",
        Sensor.TYPE_ACCELEROMETER to "Accelerometer",
        Sensor.TYPE_GRAVITY to "Gravity",
        Sensor.TYPE_ROTATION_VECTOR to "Rotation vector",
        Sensor.TYPE_HEART_RATE to "Heart rate",
        Sensor.TYPE_SIGNIFICANT_MOTION to "Significant motion",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "Geomagnetic rotation",
    )

    private val active = mutableListOf<Sensor>()

    fun hasAny(): Boolean = active.isNotEmpty()

    fun start() {
        val canSteps = android.os.Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(ctxA, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        acc.sensorsFound = watched.mapNotNull { (type, name) ->
            sm.getDefaultSensor(type)?.let { s ->
                // Step counter butuh izin ACTIVITY_RECOGNITION di API 29+.
                if (type == Sensor.TYPE_STEP_COUNTER && !canSteps) return@mapNotNull null
                if (sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)) {
                    active += s
                    name
                } else null
            }
        }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_PRESSURE -> {
                acc.pressureHpa = e.values[0]
            }
            Sensor.TYPE_RELATIVE_HUMIDITY -> acc.humidityPct = e.values[0]
            Sensor.TYPE_AMBIENT_TEMPERATURE -> acc.ambientTempC = e.values[0]
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val (x, y, z) = e.values
                val mag = sqrt(x * x + y * y + z * z)
                acc.magneticUt = mag
                synchronized(acc.magValues) { acc.magValues += mag }
            }
            Sensor.TYPE_STEP_COUNTER -> acc.onStepCounter(e.values[0])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
class CellSampler(private val ctx: Context) {
    private val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    fun sample(acc: ScanAccumulator): Int? {
        val dbm = if (android.os.Build.VERSION.SDK_INT >= 29) {
            val strength: SignalStrength? = runCatching { tm?.signalStrength }.getOrNull()
            strength?.cellSignalStrengths?.maxByOrNull { it.dbm }?.dbm
        } else null
        val valid = dbm?.takeIf { it < 0 && it > -140 }
        acc.setCell(valid)
        return valid
    }
}

/** Posisi kasar (untuk azimuth matahari) — last known location, maks 24 jam.
 *  Lebih tua dari itu dianggap tidak andal → orientasi diskip daripada salah arah. */
fun lastKnownLocation(ctx: Context, maxAgeMs: Long = 24L * 60 * 60 * 1000): Pair<Double, Double>? {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return null
    @SuppressLint("MissingPermission")
    val loc = runCatching {
        lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }
            .filter { System.currentTimeMillis() - it.time < maxAgeMs }
            .maxByOrNull { it.time }
    }.getOrNull() ?: return null
    return loc?.let { it.latitude to it.longitude }
}

/** Cek apakah Wi-Fi saat ini adalah transport aktif. */
fun wifiActive(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}
