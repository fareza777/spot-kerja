package com.spotkerja.data

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Mesin skor murni-Kotlin (tanpa dependensi Android) supaya mudah diuji.
 * Semua nilai adalah ESTIMASI praktis berbasis sensor HP — bukan pengukuran ilmiah.
 */
object ScoreEngine {

    fun weightsFor(mode: WorkMode): MetricWeights = when (mode) {
        WorkMode.WORK -> MetricWeights(
            wifi = 0.17f, ping = 0.10f, jitter = 0.05f, packetLoss = 0.10f,
            light = 0.22f, noise = 0.23f, orientation = 0.05f, cellular = 0.05f,
            wifiCongestion = 0f,
        )
        WorkMode.STUDY -> MetricWeights(
            wifi = 0.10f, ping = 0.04f, jitter = 0.02f, packetLoss = 0.05f,
            light = 0.35f, noise = 0.34f, orientation = 0.03f, cellular = 0.05f,
            wifiCongestion = 0f,
        )
        WorkMode.GAMING -> MetricWeights(
            wifi = 0.15f, ping = 0.25f, jitter = 0.18f, packetLoss = 0.22f,
            light = 0.05f, noise = 0.05f, orientation = 0.00f, cellular = 0.05f,
            wifiCongestion = 0f,
        )
        WorkMode.VIDEO_CALL -> MetricWeights(
            wifi = 0.18f, ping = 0.15f, jitter = 0.13f, packetLoss = 0.15f,
            light = 0.18f, noise = 0.10f, orientation = 0.04f, cellular = 0.03f,
            wifiCongestion = 0f,
        )
    }

    /** Rentang lux ideal per mode (perkiraan praktis, bukan standar resmi). */
    fun idealLuxBand(mode: WorkMode): ClosedFloatingPointRange<Float> = when (mode) {
        WorkMode.WORK -> 300f..550f
        WorkMode.STUDY -> 400f..650f
        WorkMode.GAMING -> 80f..300f
        WorkMode.VIDEO_CALL -> 250f..500f
    }

    private fun clampScore(v: Float): Float = min(100f, max(0f, v))

    /** RSSI -30 dBm ≈ sempurna, -90 dBm ≈ tidak terpakai. */
    fun wifiScore(rssiDbm: Int?): Float? = rssiDbm?.let {
        clampScore((it + 90f) / 60f * 100f)
    }

    /** Ping ke gateway lokal: ≤5 ms bagus, ≥100 ms buruk. */
    fun pingScore(avgMs: Float?): Float? = avgMs?.let {
        clampScore(100f - (it - 5f) * (100f / 95f))
    }

    /** Jitter: 0 ms ideal, ≥30 ms buruk. */
    fun jitterScore(jitterMs: Float?): Float? = jitterMs?.let {
        clampScore(100f - it * (100f / 30f))
    }

    /** Packet loss: 0% ideal, ≥10% buruk. */
    fun packetLossScore(lossPct: Float?): Float? = lossPct?.let {
        clampScore(100f - it * 10f)
    }

    /**
     * Skor cahaya: 100 di dalam rentang ideal, meluruh gaussian di luar.
     * Lux sangat tinggi (>1500) juga dihukum — potensi silau/panas.
     */
    fun lightScore(lux: Float?, mode: WorkMode, luxStdDev: Float? = null): Float? = lux?.let { l ->
        val band = idealLuxBand(mode)
        val base = when {
            l < band.start -> 100f * exp(-sq((band.start - l) / max(band.start, 1f)) * 3f)
            l > band.endInclusive -> 100f * exp(-sq((l - band.endInclusive) / band.endInclusive) * 2.2f)
            else -> 100f
        }
        val glarePenalty = if (l > 1500f) min(30f, (l - 1500f) / 100f) else 0f
        // Cahaya tidak stabil (flicker/lalu-lalang) mengurangi skor — maks -15.
        val stabilityPenalty = luxStdDev?.let { min(15f, max(0f, (it - 80f) / 40f) * 3f) } ?: 0f
        clampScore(base - glarePenalty - stabilityPenalty)
    }

    /** Level noise relatif: ≤30 dB(est) hening, ≥75 dB(est) bising. */
    fun noiseScore(db: Float?): Float? = db?.let {
        clampScore(100f - (it - 30f) * (100f / 45f))
    }

    /**
     * Orientasi kontinu: makin menghadap matahari (siang) makin rendah — silau layar.
     * Bonus kecil bila menghadap arah sumber cahaya terukur (lightDirectionDeg).
     */
    fun orientationScore(
        azimuthDeg: Float?,
        sunAzimuthDeg: Float?,
        lightDirectionDeg: Float? = null,
    ): Float? {
        if (azimuthDeg == null) return null
        var score: Float? = null
        if (sunAzimuthDeg != null) {
            val diff = SunPosition.angularDiff(azimuthDeg, sunAzimuthDeg)
            score = when {
                diff < 30f -> 35f
                diff < 60f -> 55f
                diff < 120f -> 80f
                else -> 95f
            }
        }
        if (lightDirectionDeg != null) {
            // Menghadap arah sumber cahaya terkuat yang terukur → cahaya muka
            // lebih baik; jadi basis penilaian bila posisi matahari tak tersedia
            // (lokasi dimatikan / izin lokasi ditolak).
            val ld = SunPosition.angularDiff(azimuthDeg, lightDirectionDeg)
            val lightScore = when {
                ld < 30f -> 70f
                ld < 60f -> 80f
                ld < 120f -> 88f
                else -> 92f
            }
            score = score?.let { if (ld < 45f) min(100f, it + 5f) else it } ?: lightScore
        }
        return score
    }

    /** Kanal Wi-Fi padat: 0 AP lain ideal, ≥10 buruk. */
    fun congestionScore(nearbyAps: Int?): Float? = nearbyAps?.let {
        clampScore(100f - it * 9f)
    }

    /** Sinyal seluler -50 dBm ≈ penuh, -115 dBm ≈ hilang. */
    fun cellularScore(dbm: Int?): Float? = dbm?.let {
        clampScore((it + 115f) / 65f * 100f)
    }

    private fun sq(x: Float) = x * x

    fun scoreSpot(metrics: SpotMetrics, mode: WorkMode): Pair<MetricScores, Float> {
        val scores = MetricScores(
            wifi = wifiScore(metrics.wifiRssiDbm),
            ping = pingScore(metrics.pingAvgMs),
            jitter = jitterScore(metrics.pingJitterMs),
            packetLoss = packetLossScore(metrics.packetLossPct),
            light = lightScore(metrics.luxAvg, mode, metrics.luxStdDev),
            noise = noiseScore(metrics.noiseDbAvg),
            orientation = orientationScore(
                metrics.azimuthDeg, metrics.sunAzimuthDeg, metrics.lightDirectionDeg),
            cellular = cellularScore(metrics.cellularDbm),
            wifiCongestion = congestionScore(metrics.wifiCongestion),
        )
        return scores to combine(scores, weightsFor(mode))
    }

    /** Gabungkan sub-skor dengan bobot; metrik yang null dilewati (renormalisasi). */
    fun combine(scores: MetricScores, w: MetricWeights): Float {
        var acc = 0f
        var wsum = 0f
        fun add(s: Float?, weight: Float) {
            if (s != null && weight > 0f) { acc += s * weight; wsum += weight }
        }
        add(scores.wifi, w.wifi); add(scores.ping, w.ping)
        add(scores.jitter, w.jitter); add(scores.packetLoss, w.packetLoss)
        add(scores.light, w.light); add(scores.noise, w.noise)
        add(scores.orientation, w.orientation); add(scores.cellular, w.cellular)
        add(scores.wifiCongestion, w.wifiCongestion)
        return if (wsum > 0f) acc / wsum else 0f
    }

    /** Short per-spot notes explaining what raised/lowered the score. */
    fun notesFor(metrics: SpotMetrics, scores: MetricScores): List<String> {
        val notes = mutableListOf<String>()
        if (metrics.pingUnreachable) notes += "Ping target unreachable — check the host/Wi-Fi"
        metrics.lightDirectionDeg?.let { dir ->
            metrics.azimuthDeg?.let { az ->
                if (SunPosition.angularDiff(az, dir) < 45f)
                    notes += "Facing the strongest measured light source"
            }
        }
        metrics.wifiRssiDbm?.let {
            when {
                it >= -55 -> notes += "Excellent Wi-Fi signal ($it dBm)"
                it >= -67 -> notes += "Good Wi-Fi signal ($it dBm)"
                it >= -75 -> notes += "Fair Wi-Fi signal ($it dBm)"
                else -> notes += "Weak Wi-Fi ($it dBm) — far from router"
            }
        }
        metrics.pingAvgMs?.let { if (it > 40f) notes += "High ping to router (≈${it.toInt()} ms)" }
        metrics.packetLossPct?.let { if (it > 1f) notes += "Packet loss detected (≈${"%.1f".format(it)}%)" }
        metrics.luxAvg?.let {
            when {
                it < 80 -> notes += "Too dark (≈${it.toInt()} lux)"
                it > 1000 -> notes += "Very bright (≈${it.toInt()} lux) — glare risk"
            }
        }
        metrics.luxStdDev?.let { if (it > 120f) notes += "Lighting fluctuated during scan" }
        metrics.noiseDbAvg?.let { if (it > 55f) notes += "Noisy environment (≈${it.toInt()} dB est.)" }
        if (metrics.glareRisk == true) notes += "Facing the sun — possible screen glare"
        if (metrics.wifiRssiDbm == null) notes += "Wi-Fi not connected/detected"
        if (metrics.noiseDbAvg == null) notes += "Noise not measured (mic permission off)"
        metrics.internetState?.let { st ->
            when (st) {
                "captive" -> notes += "Captive portal — sign in before internet works"
                "limited" -> notes += "Connected but internet unverified"
                "none" -> notes += "No internet — scores reflect local link quality"
            }
        }
        metrics.meshApCount?.let { if (it > 0) notes += "Mesh network: $it other AP(s) share this SSID" }
        metrics.roamCount?.let { if (it > 0) notes += "Roamed between APs ${it}× during scan" }
        metrics.soundTop?.let { notes += "Dominant sound: $it" }
        return notes
    }

    fun bestSpot(spots: List<SpotResult>): SpotResult? = spots.maxByOrNull { it.totalScore }

    /** true bila dua spot teratas hampir imbang (selisih < 5 poin). */
    fun isCloseCall(spots: List<SpotResult>): Boolean {
        val sorted = spots.sortedByDescending { it.totalScore }
        return sorted.size >= 2 && (sorted[0].totalScore - sorted[1].totalScore) < 5f
    }
}

/** Posisi matahari aproksimasi (algoritme NOAA) — offline, tanpa presisi astronomi. */
object SunPosition {

    /** Azimuth matahari dalam derajat (0=Utara, 90=Timur). null bila malam/tidak tersedia. */
    fun azimuthDeg(epochMs: Long, lat: Double, lon: Double): Float? {
        val elev = elevationDeg(epochMs, lat, lon) ?: return null
        if (elev <= 0.0) return null
        return azimuth(epochMs, lat, lon)
    }

    private fun julianDay(epochMs: Long) = epochMs / 86400000.0 + 2440587.5

    private fun solarParams(epochMs: Long): Triple<Double, Double, Double> {
        val d = julianDay(epochMs) - 2451545.0
        val g = (357.529 + 0.98560028 * d) % 360.0
        val q = (280.459 + 0.98564736 * d) % 360.0
        val l = q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g)
        val e = 23.439 - 0.00000036 * d
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val gmst = (18.697374558 + 24.06570982441908 * (julianDay(epochMs) - 2451545.0)) % 24.0
        return Triple(decl, gmst, ra)
    }

    private fun azimuth(epochMs: Long, lat: Double, lon: Double): Float {
        val (decl, gmst, ra) = solarParams(epochMs)
        val lst = (gmst + lon / 15.0) * 15.0
        val ha = lst - ra * 15.0
        val sinAlt = sinDeg(decl) * sinDeg(lat) + cosDeg(decl) * cosDeg(lat) * cosDeg(ha)
        val alt = asinDeg(sinAlt)
        val cosAz = (sinDeg(decl) - sinAlt * sinDeg(lat)) / (cosDeg(alt) * cosDeg(lat))
        val az = acosDeg(cosAz.coerceIn(-1.0, 1.0))
        return ((if (sinDeg(ha) > 0) 360.0 - az else az).toFloat() + 360f) % 360f
    }

    fun elevationDeg(epochMs: Long, lat: Double, lon: Double): Double? {
        val (decl, gmst, ra) = solarParams(epochMs)
        val lst = (gmst + lon / 15.0) * 15.0
        val ha = lst - ra * 15.0
        return asinDeg(sinDeg(decl) * sinDeg(lat) + cosDeg(decl) * cosDeg(lat) * cosDeg(ha))
    }

    private fun sinDeg(d: Double) = kotlin.math.sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = kotlin.math.cos(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(kotlin.math.asin(x.coerceIn(-1.0, 1.0)))
    private fun acosDeg(x: Double) = Math.toDegrees(kotlin.math.acos(x.coerceIn(-1.0, 1.0)))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(kotlin.math.atan2(y, x))

    /** Selisih sudut terkecil antara dua bearing. */
    fun angularDiff(a: Float, b: Float): Float {
        var d = abs((a - b) % 360f)
        if (d > 180f) d = 360f - d
        return d
    }
}
