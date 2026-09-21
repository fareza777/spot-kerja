package com.spotkerja.sense

import android.content.Context
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotMetrics
import com.spotkerja.data.SpotResult
import com.spotkerja.data.SunPosition
import com.spotkerja.data.WorkMode
import com.spotkerja.settings.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ScanProgress(
    val running: Boolean = false,
    val elapsedSec: Int = 0,
    val totalSec: Int = 30,
    val live: LiveMetrics = LiveMetrics(),
    val luxSeries: List<Float> = emptyList(),
    val noiseSeries: List<Float> = emptyList(),
)

/** Mengorkestrasi satu scan spot: semua sampler berjalan, lalu hasil diagregasi. */
class ScanEngine(private val ctx: Context) {

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress

    private var job: Job? = null
    private var ambient: AmbientSampler? = null
    private var noise: NoiseSampler? = null
    private var env: EnvSampler? = null
    private var classifier: SoundClassifier? = null
    private var thermal: ThermalSampler? = null
    private var camLight: CameraLightSampler? = null

    /** True bila scan selesai penuh (bukan dihentikan manual) — untuk auto-advance. */
    var onFinished: ((SpotResult) -> Unit)? = null

    fun isRunning() = _progress.value.running

    fun start(
        scope: CoroutineScope,
        spotLabel: String,
        durationSec: Int,
        mode: WorkMode,
        opts: ScanOptions = ScanOptions(),
        onDone: (SpotResult) -> Unit,
    ) {
        if (isRunning()) return
        val acc = ScanAccumulator()
        currentAcc = acc
        val wifi = if (opts.wifi) WifiSampler(ctx) else null
        val ping = if (opts.ping) PingSampler(ctx, opts.pingHost) else null
        val cell = if (opts.cellular) CellSampler(ctx) else null
        val radio = if (opts.wifi) WifiScanSampler(ctx) else null
        val internet = InternetChecker(ctx)
        ambient = AmbientSampler(ctx, acc, opts.light, opts.orientation)
            .takeIf { it.hasAny() }?.also { it.start() }
        // Sensor lingkungan jarang — pasif & murah; gated oleh toggle Extended.
        env = if (opts.extended) EnvSampler(ctx, acc).also { it.start() } else null
        // YAMNet sound events ikut stream mic — gated oleh toggle Noise.
        classifier = if (opts.noise && opts.extended) SoundClassifier.create(ctx) else null
        noise = if (opts.noise) NoiseSampler(ctx, acc, classifier).also { it.start(scope) } else null
        // Thermal — listener OS murah; bagian dari extended metrics.
        thermal = if (opts.extended) ThermalSampler(ctx).also { it.start(acc) } else null
        // Light map kamera — butuh izin CAMERA; gated oleh extended.
        val camLight = if (opts.extended) CameraLightSampler(ctx).also {
            this.camLight = it } else null

        job = scope.launch(Dispatchers.Default) {
            _progress.value = ScanProgress(running = true, totalSec = durationSec)
            val deadline = System.currentTimeMillis() + durationSec * 1000L
            var tick = 0
            radio?.radio(acc)
            if (opts.wifi || opts.ping) internet.check(acc)
            // Traceroute paralel — TTL-exceeded hops ke target ping (max ~8 hop).
            val routeJob = if (opts.ping && opts.extended)
                launch(Dispatchers.IO) { RouteTracer().trace(acc, ping?.target()) }
            else null
            // DNS/TCP/TLS/UDP probes + kamera light map — paralel, sekali per scan.
            val probeJob = if (opts.extended)
                launch(Dispatchers.IO) {
                    delay(1500) // beri internet.check waktu jalan dulu
                    NetworkProber().probe(acc, ping?.target(),
                        acc.internetState == "ok")
                }
            else null
            launch(Dispatchers.IO) { camLight?.start() }
            while (isActive && System.currentTimeMillis() < deadline) {
                val rssi = wifi?.sample(acc)
                val cellDbm = cell?.sample(acc)
                if (ping != null) launch { ping.pingOnce(acc) }
                val (_, pingsNow, luxNow) = acc.snapshot()
                val live = LiveMetrics(
                    wifiRssiDbm = rssi,
                    pingMs = pingsNow.lastOrNull(),
                    lux = luxNow.lastOrNull(),
                    noiseDb = acc.noiseSnapshot().lastOrNull(),
                    azimuthDeg = acc.azimuthDeg,
                    cellularDbm = cellDbm,
                    wifiBand = acc.wifiBand,
                    linkSpeedMbps = acc.linkSpeed,
                )
                _progress.value = _progress.value.copy(
                    elapsedSec = ((durationSec * 1000L - (deadline - System.currentTimeMillis())) / 1000)
                        .toInt().coerceIn(0, durationSec),
                    live = live,
                    luxSeries = luxNow.takeLast(30),
                    noiseSeries = acc.noiseSnapshot().takeLast(30),
                )
                tick++
                delay(1000)
            }
            _progress.value = _progress.value.copy(live = _progress.value.live, elapsedSec = durationSec)
            routeJob?.join()
            probeJob?.join()
            finish(acc, spotLabel, durationSec, mode, opts, onDone)
        }
    }

    /** Stop lebih awal — hasil tetap dihitung dari sampel yang terkumpul. */
    fun stop(spotLabel: String, durationSec: Int, mode: WorkMode,
             opts: ScanOptions = ScanOptions(), onDone: (SpotResult) -> Unit) {
        val acc = currentAcc ?: return
        job?.cancel()
        finish(acc, spotLabel, durationSec, mode, opts, onDone)
    }

    private var currentAcc: ScanAccumulator? = null

    private fun finish(
        acc: ScanAccumulator,
        spotLabel: String,
        durationSec: Int,
        mode: WorkMode,
        opts: ScanOptions,
        onDone: (SpotResult) -> Unit,
    ) {
        ambient?.stop(); ambient = null
        noise?.stop(); noise = null
        env?.stop(); env = null
        classifier?.close(); classifier = null
        thermal?.stop(); thermal = null
        job?.cancel(); job = null

        val (_, pings, _) = acc.snapshot()
        // Jitter = standar deviasi RTT — lebih stabil daripada selisih berurutan.
        val jitter = pings.stdDevOrNull()
        // Ping target diisi tapi tak pernah menjawab → unreachable, bukan packet loss biasa.
        val pingUnreachable = acc.pingAttempts > 0 && pings.isEmpty()
        val packetLoss = if (pingUnreachable) null else acc.packetLossPct()

        val loc = lastKnownLocation(ctx)
        lastLoc = loc
        val sunAz = loc?.let { SunPosition.azimuthDeg(System.currentTimeMillis(), it.first, it.second) }
        val glare = if (sunAz != null && acc.azimuthDeg != null) {
            SunPosition.angularDiff(acc.azimuthDeg!!, sunAz) < 45f
        } else null
        // Jika orientasi dimatikan, azimuth tidak dikumpulkan → metrik jadi null dan
        // ScoreEngine menormalkan ulang bobotnya.

        val (rssiL, pings2, luxL) = acc.snapshot()
        val noiseL = acc.noiseSnapshot()
        // Light map kamera — unbind di-post ke main, aman dari thread manapun.
        camLight?.finish(acc)
        camLight = null
        val sounds = acc.topSounds(3)
        val speechPct = if (acc.soundWindows > 0)
            acc.speechWindows * 100f / acc.soundWindows else null
        val magStd = synchronized(acc.magValues) { acc.magValues.toList() }
            .stdDevOrNull()?.takeIf { acc.magValues.size >= 5 }
        val metrics = SpotMetrics(
            wifiRssiDbm = rssiL.avgOrNullI(),
            wifiLinkSpeedMbps = acc.linkSpeed,
            wifiBand = acc.wifiBand,
            wifiCongestion = acc.wifiCongestion,
            pingAvgMs = pings2.avgOrNull(),
            pingJitterMs = jitter,
            packetLossPct = packetLoss,
            pingUnreachable = pingUnreachable,
            luxAvg = luxL.avgOrNull(),
            luxStdDev = luxL.stdDevOrNull(),
            noiseDbAvg = noiseL.avgOrNull(),
            azimuthDeg = acc.azimuthDeg,
            lightDirectionDeg = acc.luxAtMaxAzimuth?.second,
            sunAzimuthDeg = sunAz,
            glareRisk = glare,
            cellularDbm = acc.cellularDbm,
            wifiSamples = acc.rssi.size,
            pingSamples = acc.pingAttempts,
            luxSamples = luxL.size,
            noiseSamples = noiseL.size,
            wifiSsid = acc.ssid,
            bssid = acc.bssid,
            channelWidthMhz = acc.channelWidthMhz,
            meshApCount = acc.meshApCount,
            roamCount = acc.roamCount.takeIf { it > 0 },
            rssiMinDbm = rssiL.minOrNull(),
            rssiMaxDbm = rssiL.maxOrNull(),
            txLinkSpeedMbps = acc.txLink,
            rxLinkSpeedMbps = acc.rxLink,
            estThroughputMbps = acc.linkSpeed?.let { it * 0.55f },
            internetState = acc.internetState,
            routeHops = acc.routeHops,
            routeTarget = acc.routeTarget,
            soundTop = sounds.firstOrNull(),
            soundLabels = sounds,
            pressureHpa = acc.pressureHpa,
            altitudeM = acc.pressureHpa?.let {
                android.hardware.SensorManager.getAltitude(1013.25f, it) },
            humidityPct = acc.humidityPct,
            ambientTempC = acc.ambientTempC,
            magneticUt = acc.magneticUt,
            stepsDuringScan = acc.stepsDuringScan,
            sensorsFound = acc.sensorsFound,
            dnsMs = acc.dnsMs,
            tcpMs = acc.tcpMs,
            tcpPort = acc.tcpPort,
            tlsMs = acc.tlsMs,
            udpState = acc.udpState,
            speechPct = speechPct,
            camLumaAvg = acc.camLumaAvg,
            camLumaStd = acc.camLumaStd,
            camHotspot = acc.camHotspot,
            lightMap = acc.lightMap,
            magneticStdDevUt = magStd,
            thermalStatus = acc.thermalStatus,
            thermalHeadroom = acc.thermalHeadroom,
            batteryTempC = acc.batteryTempC,
        )
        val (scores, total) = ScoreEngine.scoreSpot(metrics, mode)
        val result = SpotResult(
            label = spotLabel,
            durationSec = durationSec,
            metrics = metrics,
            scores = scores,
            totalScore = total,
            notes = ScoreEngine.notesFor(metrics, scores),
            confidencePct = confidence(metrics, opts),
        )
        _progress.value = ScanProgress(running = false, totalSec = durationSec)
        currentAcc = null
        onDone(result)
    }

    /** Keyakinan 0–100: proporsi metrik yang diaktifkan dan berhasil menghasilkan data. */
    private fun confidence(m: SpotMetrics, o: ScanOptions): Int {
        var enabled = 0
        var produced = 0
        fun need(on: Boolean, ok: Boolean) { if (on) { enabled++; if (ok) produced++ } }
        need(o.wifi, m.wifiRssiDbm != null)
        need(o.ping, m.pingAvgMs != null && !m.pingUnreachable)
        need(o.light, m.luxAvg != null)
        need(o.noise, m.noiseDbAvg != null)
        need(o.orientation, m.azimuthDeg != null)
        need(o.cellular, m.cellularDbm != null)
        return if (enabled == 0) 0 else produced * 100 / enabled
    }

    fun attachAcc(acc: ScanAccumulator) { currentAcc = acc }

    /** Lokasi terakhir yang dipakai scan (dibulatkan ~1 km) — untuk glare forecast. */
    var lastLoc: Pair<Double, Double>? = null
        private set

    fun cancel() {
        job?.cancel(); job = null
        ambient?.stop(); ambient = null
        noise?.stop(); noise = null
        env?.stop(); env = null
        classifier?.close(); classifier = null
        thermal?.stop(); thermal = null
        camLight?.finish(ScanAccumulator())
        camLight = null
        currentAcc = null
        _progress.value = ScanProgress()
    }
}
