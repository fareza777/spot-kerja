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
)

/** Mengorkestrasi satu scan spot: semua sampler berjalan, lalu hasil diagregasi. */
class ScanEngine(private val ctx: Context) {

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress

    private var job: Job? = null
    private var ambient: AmbientSampler? = null
    private var noise: NoiseSampler? = null

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
        ambient = AmbientSampler(ctx, acc, opts.light, opts.orientation)
            .takeIf { it.hasAny() }?.also { it.start() }
        noise = if (opts.noise) NoiseSampler(ctx, acc).also { it.start(scope) } else null

        job = scope.launch(Dispatchers.Default) {
            _progress.value = ScanProgress(running = true, totalSec = durationSec)
            val deadline = System.currentTimeMillis() + durationSec * 1000L
            var tick = 0
            while (isActive && System.currentTimeMillis() < deadline) {
                val rssi = wifi?.sample(acc)
                val cellDbm = cell?.sample(acc)
                // Ping tiap ~2 detik agar jumlah sampel wajar
                if (ping != null && tick % 2 == 0) launch { ping.pingOnce(acc) }
                val live = LiveMetrics(
                    wifiRssiDbm = rssi,
                    pingMs = acc.pings.lastOrNull(),
                    lux = acc.lux.lastOrNull(),
                    noiseDb = acc.noise.lastOrNull(),
                    azimuthDeg = acc.azimuthDeg,
                    cellularDbm = cellDbm,
                    wifiBand = acc.wifiBand,
                    linkSpeedMbps = acc.linkSpeed,
                )
                _progress.value = _progress.value.copy(
                    elapsedSec = ((durationSec * 1000L - (deadline - System.currentTimeMillis())) / 1000)
                        .toInt().coerceIn(0, durationSec),
                    live = live,
                )
                tick++
                delay(1000)
            }
            _progress.value = _progress.value.copy(live = _progress.value.live, elapsedSec = durationSec)
            finish(acc, spotLabel, durationSec, mode, onDone)
        }
    }

    /** Stop lebih awal — hasil tetap dihitung dari sampel yang terkumpul. */
    fun stop(spotLabel: String, durationSec: Int, mode: WorkMode, onDone: (SpotResult) -> Unit) {
        val acc = currentAcc ?: return
        job?.cancel()
        finish(acc, spotLabel, durationSec, mode, onDone)
    }

    private var currentAcc: ScanAccumulator? = null

    private fun finish(
        acc: ScanAccumulator,
        spotLabel: String,
        durationSec: Int,
        mode: WorkMode,
        onDone: (SpotResult) -> Unit,
    ) {
        ambient?.stop(); ambient = null
        noise?.stop(); noise = null
        job?.cancel(); job = null

        val pings = acc.pings
        val jitter = if (pings.size >= 2) {
            pings.zipWithNext { a, b -> kotlin.math.abs(b - a) }.average().toFloat()
        } else 0f.takeIf { pings.isNotEmpty() }

        val loc = lastKnownLocation(ctx)
        val sunAz = loc?.let { SunPosition.azimuthDeg(System.currentTimeMillis(), it.first, it.second) }
        val glare = if (sunAz != null && acc.azimuthDeg != null) {
            SunPosition.angularDiff(acc.azimuthDeg!!, sunAz) < 45f
        } else null
        // Jika orientasi dimatikan, azimuth tidak dikumpulkan → metrik jadi null dan
        // ScoreEngine menormalkan ulang bobotnya.

        val metrics = SpotMetrics(
            wifiRssiDbm = acc.rssi.avgOrNullI().takeIf { acc.rssi.isNotEmpty() },
            wifiLinkSpeedMbps = acc.linkSpeed,
            wifiBand = acc.wifiBand,
            pingAvgMs = pings.avgOrNull(),
            pingJitterMs = jitter,
            packetLossPct = acc.packetLossPct(),
            luxAvg = acc.lux.avgOrNull(),
            luxStdDev = acc.lux.stdDevOrNull(),
            noiseDbAvg = acc.noise.avgOrNull(),
            azimuthDeg = acc.azimuthDeg,
            lightDirectionDeg = acc.luxAtMaxAzimuth?.second,
            sunAzimuthDeg = sunAz,
            glareRisk = glare,
            cellularDbm = acc.cellularDbm,
            wifiSamples = acc.rssi.size,
            pingSamples = acc.pingAttempts,
            luxSamples = acc.lux.size,
            noiseSamples = acc.noise.size,
        )
        val (scores, total) = ScoreEngine.scoreSpot(metrics, mode)
        val result = SpotResult(
            label = spotLabel,
            durationSec = durationSec,
            metrics = metrics,
            scores = scores,
            totalScore = total,
            notes = ScoreEngine.notesFor(metrics, scores),
        )
        _progress.value = ScanProgress(running = false, totalSec = durationSec)
        currentAcc = null
        onDone(result)
    }

    fun attachAcc(acc: ScanAccumulator) { currentAcc = acc }

    fun cancel() {
        job?.cancel(); job = null
        ambient?.stop(); ambient = null
        noise?.stop(); noise = null
        currentAcc = null
        _progress.value = ScanProgress()
    }
}
