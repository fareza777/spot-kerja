package com.spotkerja.sense

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.roundToInt

/**
 * DNS / TCP / TLS / UDP(QUIC) responsiveness — sekali per scan, paralel di IO.
 * Target mengikuti ping host (default gateway); DNS/TLS mencoba 1.1.1.1/dns.google
 * bila internet tervalidasi — semua hasil dicatat apa adanya.
 */
class NetworkProber {

    suspend fun probe(acc: ScanAccumulator, tcpTarget: String?,
                      internetOk: Boolean) = withContext(Dispatchers.IO) {
        // --- TCP connect ke target ping (router/host custom) -----------------
        tcpTarget?.let { host ->
            for (port in listOf(443, 80, 53)) {
                val t0 = System.nanoTime()
                val ok = runCatching {
                    Socket().use { s ->
                        s.connect(InetSocketAddress(host, port), 1500)
                        s.isConnected
                    }
                }.getOrDefault(false)
                if (ok) {
                    acc.tcpMs = (System.nanoTime() - t0) / 1_000_000f
                    acc.tcpPort = port
                    break
                }
            }
        }

        // --- DNS resolve timing (sistem resolver — ikut jaringan aktif) -------
        if (internetOk) {
            val d0 = System.nanoTime()
            val ok = runCatching {
                InetAddress.getAllByName("dns.google").isNotEmpty()
            }.getOrDefault(false)
            if (ok) acc.dnsMs = (System.nanoTime() - d0) / 1_000_000f

            // --- TLS handshake ke 1.1.1.1:443 ----------------------------------
            val t0 = System.nanoTime()
            runCatching {
                val s = SSLSocketFactory.getDefault().createSocket() as javax.net.ssl.SSLSocket
                s.soTimeout = 2500
                s.connect(InetSocketAddress("1.1.1.1", 443), 2500)
                s.startHandshake()
                acc.tlsMs = (System.nanoTime() - t0) / 1_000_000f
                s.close()
            }

            // --- UDP/QUIC reachability: kirim datagram ke 1.1.1.1:443, tunggu
            // ICMP port-unreachable. Timeout = "filtered/open" (tak bisa dibedakan
            // tanpa server), socket error = diblokir lokal.
            acc.udpState = runCatching {
                val ds = DatagramSocket()
                ds.soTimeout = 400
                val payload = byteArrayOf(0)
                val dest = InetSocketAddress("1.1.1.1", 443)
                repeat(2) {
                    ds.send(DatagramPacket(payload, 1, dest))
                    runCatching { ds.receive(DatagramPacket(ByteArray(8), 8)) }
                }
                ds.close()
                "open" // tak ada ICMP unreachable → UDP keluar
            }.getOrElse { "blocked" }
        }
    }
}

/** Thermal exposure: status termal OS + headroom + suhu baterai (sticky intent). */
class ThermalSampler(private val ctx: Context) {

    private val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var listener: PowerManager.OnThermalStatusChangedListener? = null

    @SuppressLint("NewApi") // dijaga oleh cek SDK di dalam
    fun start(acc: ScanAccumulator) {
        // Suhu baterai — proxy panas perangkat (matahari langsung ikut memanaskan HP).
        val bat: Intent? = ctx.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        bat?.let { i ->
            val t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (t != Int.MIN_VALUE) acc.batteryTempC = t / 10f
        }
        if (Build.VERSION.SDK_INT >= 29) {
            runCatching { acc.thermalStatus = pm?.currentThermalStatus }
            if (Build.VERSION.SDK_INT >= 30) {
                runCatching {
                    val h = pm?.getThermalHeadroom(10) // estimasi 10 dtk ke depan
                    if (h != null && !h.isNaN()) acc.thermalHeadroom = h
                }
            }
            runCatching {
                val l = PowerManager.OnThermalStatusChangedListener { st ->
                    acc.thermalStatus = st
                }
                listener = l
                pm?.addThermalStatusListener(ContextCompat.getMainExecutor(ctx), l)
            }
        }
    }

    @SuppressLint("NewApi")
    fun stop() {
        if (Build.VERSION.SDK_INT >= 29) {
            listener?.let { runCatching { pm?.removeThermalStatusListener(it) } }
        }
        listener = null
    }
}

/** LifecycleOwner minimal agar CameraX bisa dipakai tanpa Activity. */
private class ScanLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    fun open() {
        registry.currentState = Lifecycle.State.RESUMED
    }
    fun close() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
    override val lifecycle: Lifecycle get() = registry
}

/**
 * Camera light map: grid 4×3 luminance dari Y-plane kamera belakang selama
 * scan — uneven lighting + arah hotspot (mis. jendela dari kiri).
 */
class CameraLightSampler(private val ctx: Context) {

    private var provider: ProcessCameraProvider? = null
    private val lifecycle = ScanLifecycle()

    // 12 sel (4 kolom × 3 baris), tiap sel menampung jumlah luma terakumulasi.
    private val cellSum = DoubleArray(12)
    private val cellN = IntArray(12)
    private var frames = 0

    fun permitted() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    suspend fun start() {
        if (!permitted()) return
        val p = withContext(Dispatchers.IO) {
            runCatching { ProcessCameraProvider.getInstance(ctx).get() }.getOrNull()
        } ?: return
        provider = p
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { img ->
            analyze(img)
            img.close()
        }
        withContext(Dispatchers.Main) {
            runCatching {
                lifecycle.open()
                p.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
            }
        }
    }

    private fun analyze(img: ImageProxy) {
        val plane = img.planes.getOrNull(0) ?: return // Y plane
        val buf = plane.buffer
        val rowStride = plane.rowStride
        val pixStride = plane.pixelStride
        val w = img.width; val h = img.height
        // Sampling sparse: tiap sel ambil ~24 titik sampel agar murah.
        for (cy in 0 until 3) {
            for (cx in 0 until 4) {
                val cell = cy * 4 + cx
                val x0 = w * cx / 4; val x1 = w * (cx + 1) / 4
                val y0 = h * cy / 3; val y1 = h * (cy + 1) / 3
                var sum = 0L; var n = 0
                var yy = y0
                while (yy < y1) {
                    var xx = x0
                    while (xx < x1) {
                        val idx = yy * rowStride + xx * pixStride
                        if (idx < buf.capacity()) {
                            sum += buf.get(idx).toInt() and 0xFF
                            n++
                        }
                        xx += maxOf(1, (x1 - x0) / 5)
                    }
                    yy += maxOf(1, (y1 - y0) / 5)
                }
                if (n > 0) { cellSum[cell] += sum.toDouble() / n; cellN[cell]++ }
            }
        }
        frames++
    }

    /** Tulis hasil ke accumulator. Panggil setelah scan selesai — aman dari
     *  thread manapun; unbind di-post ke main (bindToLifecycle wajib main). */
    fun finish(acc: ScanAccumulator) {
        Handler(Looper.getMainLooper()).post {
            runCatching { provider?.unbindAll() }
            lifecycle.close()
        }
        provider = null
        if (frames == 0) return
        val cells = (0 until 12).map { c ->
            if (cellN[c] > 0) (cellSum[c] / cellN[c]).roundToInt() else 0
        }
        acc.lightMap = cells
        val avg = cells.average().toFloat()
        acc.camLumaAvg = avg
        acc.camLumaStd = cells.map { it.toFloat() }.stdDevOrNull()
        val hot = cells.indices.maxByOrNull { cells[it] } ?: return
        if (cells[hot] > avg * 1.25f) {
            val col = hot % 4; val row = hot / 4
            val v = when (row) { 0 -> "upper " ; 2 -> "lower "; else -> "" }
            val hoz = when (col) { 0 -> "left"; 3 -> "right"; else -> "center" }
            acc.camHotspot = (v + hoz).trim()
        }
    }
}
