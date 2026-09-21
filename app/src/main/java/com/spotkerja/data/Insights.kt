package com.spotkerja.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Agregasi history → insight: glare forecast, best-by-hour, busy hours, badges. */
object Insights {

    // ---- Glare forecast -----------------------------------------------------
    // Untuk tiap spot dengan azimuth terukur: jam-jam dalam 24 jam ke depan di
    // mana matahari berada dalam 45° dari arah hadap → potensi silau layar.
    // Butuh lokasi tersimpan di sesi; null-safe: hasil kosong bila tak ada.

    data class GlareWindow(val spotLabel: String, val fromHour: Int, val toHour: Int) {
        fun text(): String = "%02d:00–%02d:00".format(fromHour, toHour)
    }

    fun glareWindows(session: ScanSession): List<GlareWindow> {
        val lat = session.latDeg ?: return emptyList()
        val lon = session.lonDeg ?: return emptyList()
        // Ramalan dihitung dari sekarang — azimuth terukur sesi tetap dipakai,
        // jadi sesi lama pun tetap menampilkan jendela silau ke depan.
        val now = System.currentTimeMillis()
        return session.spots.mapNotNull { s ->
            val az = s.metrics.azimuthDeg ?: return@mapNotNull null
            // Window: jam-jam berurutan dengan sunAzimuth dalam 45° azimuth hadap.
            var start: Int? = null
            val windows = mutableListOf<GlareWindow>()
            for (h in 0..25) {
                val t = now + h * 3600_000L
                val sunAz = SunPosition.azimuthDeg(t, lat, lon)
                val glare = sunAz != null && SunPosition.angularDiff(az, sunAz) < 45f
                if (glare && start == null) start = h
                if ((!glare || h == 25) && start != null) {
                    windows += GlareWindow(s.label, hourAt(now, start!!), hourAt(now, h))
                    start = null
                }
            }
            windows.maxByOrNull { it.toHour - it.fromHour } // tampilkan window terpanjang saja
        }
    }

    private fun hourAt(epochMs: Long, plusHours: Int): Int =
        Instant.ofEpochMilli(epochMs + plusHours * 3600_000L)
            .atZone(ZoneId.systemDefault()).hour

    // ---- Best spot by hour ---------------------------------------------------
    // Bucket jam: Morning 5-11, Midday 11-15, Afternoon 15-19, Evening 19-24, Night 0-5.
    val HOUR_BUCKETS = listOf(
        "Morning" to (5..10), "Midday" to (11..14), "Afternoon" to (15..18),
        "Evening" to (19..23), "Night" to (0..4),
    )

    private fun bucketOf(epochMs: Long): Int {
        val h = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).hour
        return HOUR_BUCKETS.indexOfFirst { h in it.second }
    }

    /** spot label → avg skor per bucket (null bila tak ada data). */
    fun bestByHour(sessions: List<ScanSession>): List<Pair<String, List<Float?>>> {
        val bySpot = mutableMapOf<String, MutableMap<Int, MutableList<Float>>>()
        sessions.forEach { sess ->
            val b = bucketOf(sess.createdAtEpochMs)
            if (b < 0) return@forEach
            sess.spots.forEach { spot ->
                bySpot.getOrPut(spot.label) { mutableMapOf() }
                    .getOrPut(b) { mutableListOf() } += spot.totalScore
            }
        }
        return bySpot.map { (label, buckets) ->
            label to HOUR_BUCKETS.indices.map { i -> buckets[i]?.average()?.toFloat() }
        }.sortedByDescending { it.second.filterNotNull().maxOrNull() ?: 0f }
            .take(6)
    }

    // ---- Congestion forecast ---------------------------------------------------
    // Avg ping + packet loss per bucket jam → jam-jam jaringan ramai.

    data class BusyHour(val label: String, val pingMs: Float?, val lossPct: Float?, val samples: Int)

    fun busyHours(sessions: List<ScanSession>): List<BusyHour> {
        return HOUR_BUCKETS.indices.map { i ->
            val pings = mutableListOf<Float>()
            val losses = mutableListOf<Float>()
            var n = 0
            sessions.forEach { sess ->
                if (bucketOf(sess.createdAtEpochMs) != i) return@forEach
                sess.spots.forEach { s ->
                    s.metrics.pingAvgMs?.let { pings += it }
                    s.metrics.packetLossPct?.let { losses += it }
                    if (s.metrics.pingAvgMs != null) n++
                }
            }
            BusyHour(HOUR_BUCKETS[i].first, pings.average().toFloat().takeIf { pings.isNotEmpty() },
                losses.average().toFloat().takeIf { losses.isNotEmpty() }, n)
        }
    }

    // ---- Badges / streak ---------------------------------------------------------

    data class Badge(val name: String, val desc: String, val unlocked: Boolean)

    fun badges(sessions: List<ScanSession>, focusMinutes: Map<String, Int>): List<Badge> {
        val top = sessions.flatMap { it.spots }.maxByOrNull { it.totalScore }
        val distinctSpots = sessions.flatMap { it.spots.map { s -> s.label } }.distinct().size
        val nightScans = sessions.count { bucketOf(it.createdAtEpochMs) == 4 }
        val totalFocus = focusMinutes.values.sum()
        val streakNow = liveStreak(sessions)

        return listOf(
            Badge("On a roll", "Scan 3+ days in a row (streak: $streakNow)", streakNow >= 3),
            Badge("Ninety club", "Found a spot scoring 90+", (top?.totalScore ?: 0f) >= 90f),
            Badge("Room mapper", "Scanned 5+ distinct spots", distinctSpots >= 5),
            Badge("Night owl", "Scanned between 00:00–05:00", nightScans >= 1),
            Badge("Deep worker", "Logged 60+ focus minutes", totalFocus >= 60),
            Badge("Data hoarder", "Saved 10+ scan sessions", sessions.size >= 10),
        )
    }

    /** Streak hari berurutan yang masih hidup (berakhir hari ini/kemarin). */
    fun liveStreak(sessions: List<ScanSession>): Int {
        val days = sessions.map {
            Instant.ofEpochMilli(it.createdAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        }.distinct().sortedDescending()
        var streak = 0
        var expect = LocalDate.now()
        days.forEach { d ->
            if (d == expect || (streak == 0 && d == expect.minusDays(1))) {
                if (streak == 0) expect = d
                streak++; expect = expect.minusDays(1)
            } else if (d < expect) return streak
        }
        return streak
    }

    // ---- Blind A/B: feel vs data ---------------------------------------------------

    /** Persen sesi blind di mana pilihan user = pemenang data; null bila belum ada. */
    fun feelMatch(sessions: List<ScanSession>): Pair<Int, Int>? {
        val done = sessions.filter { it.blind && it.userPickLabel != null }
        if (done.isEmpty()) return null
        val hits = done.count { it.userPickLabel == it.bestSpotLabel }
        return (hits * 100 / done.size) to done.size
    }

    // ---- Before/After delta ------------------------------------------------------

    /** Untuk tiap spot label dengan ≥2 sesi: skor terbaru, sebelumnya, dan delta. */
    data class Delta(val label: String, val first: Float, val latest: Float) {
        val change: Float get() = latest - first
        fun changeText(): String = (if (change >= 0) "+" else "") + change.roundToInt().toString()
    }

    fun deltas(sessions: List<ScanSession>): List<Delta> {
        return sessions.sortedBy { it.createdAtEpochMs }
            .flatMap { s -> s.spots.map { it.label to it.totalScore } }
            .groupBy({ it.first }, { it.second })
            .filter { it.value.size >= 2 }
            .map { (label, scores) -> Delta(label, scores.first(), scores.last()) }
            .sortedByDescending { kotlin.math.abs(it.change) }
            .take(4)
    }
}
