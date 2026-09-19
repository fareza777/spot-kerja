package com.spotkerja

import com.spotkerja.data.MetricScores
import com.spotkerja.data.MetricWeights
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotMetrics
import com.spotkerja.data.SunPosition
import com.spotkerja.data.WorkMode
import org.junit.Assert.*
import org.junit.Test

class ScoreEngineTest {

    @Test
    fun `wifi score maps rssi range`() {
        assertEquals(100f, ScoreEngine.wifiScore(-30)!!, 0.01f)
        assertEquals(0f, ScoreEngine.wifiScore(-90)!!, 0.01f)
        assertEquals(50f, ScoreEngine.wifiScore(-60)!!, 0.5f)
        assertEquals(100f, ScoreEngine.wifiScore(-10)!!, 0.01f) // clamp atas
        assertNull(ScoreEngine.wifiScore(null))
    }

    @Test
    fun `ping score degrades with latency`() {
        assertEquals(100f, ScoreEngine.pingScore(5f)!!, 0.01f)
        assertEquals(0f, ScoreEngine.pingScore(100f)!!, 0.01f)
        assertTrue(ScoreEngine.pingScore(20f)!! > ScoreEngine.pingScore(80f)!!)
    }

    @Test
    fun `light score peaks inside ideal band`() {
        val inBand = ScoreEngine.lightScore(400f, WorkMode.WORK)!!
        val tooLow = ScoreEngine.lightScore(50f, WorkMode.WORK)!!
        val tooHigh = ScoreEngine.lightScore(2000f, WorkMode.WORK)!!
        assertEquals(100f, inBand, 0.01f)
        assertTrue(tooLow < 60f)
        assertTrue(tooHigh < 80f)
        // Gaming lebih toleran terhadap cahaya rendah
        assertTrue(ScoreEngine.lightScore(100f, WorkMode.GAMING)!! >
            ScoreEngine.lightScore(100f, WorkMode.STUDY)!!)
    }

    @Test
    fun `noise score range`() {
        assertEquals(100f, ScoreEngine.noiseScore(30f)!!, 0.01f)
        assertEquals(0f, ScoreEngine.noiseScore(80f)!!, 0.01f)
    }

    @Test
    fun `combine renormalizes when metrics missing`() {
        val scores = MetricScores(wifi = 80f, light = 60f) // lainnya null
        val w = MetricWeights(wifi = 1f, light = 1f, noise = 5f)
        assertEquals(70f, ScoreEngine.combine(scores, w), 0.01f)
    }

    @Test
    fun `combine returns 0 when nothing scored`() {
        assertEquals(0f, ScoreEngine.combine(MetricScores(), MetricWeights(wifi = 1f)), 0.01f)
    }

    @Test
    fun `total score of great metrics beats poor metrics`() {
        val great = SpotMetrics(
            wifiRssiDbm = -45, pingAvgMs = 8f, pingJitterMs = 2f, packetLossPct = 0f,
            luxAvg = 450f, noiseDbAvg = 32f, cellularDbm = -70,
        )
        val poor = SpotMetrics(
            wifiRssiDbm = -85, pingAvgMs = 90f, pingJitterMs = 25f, packetLossPct = 8f,
            luxAvg = 30f, noiseDbAvg = 70f, cellularDbm = -110,
        )
        val (_, good) = ScoreEngine.scoreSpot(great, WorkMode.WORK)
        val (_, bad) = ScoreEngine.scoreSpot(poor, WorkMode.WORK)
        assertTrue(good > 75f)
        assertTrue(bad < 35f)
        assertTrue(good > bad)
    }

    @Test
    fun `mode changes ranking for same metrics`() {
        val m = SpotMetrics(
            wifiRssiDbm = -40, pingAvgMs = 5f, pingJitterMs = 1f, packetLossPct = 0f,
            luxAvg = 100f, noiseDbAvg = 30f,
        )
        val (_, gaming) = ScoreEngine.scoreSpot(m, WorkMode.GAMING)
        val (_, study) = ScoreEngine.scoreSpot(m, WorkMode.STUDY)
        // Sama-sama bagus untuk gaming (network sempurna, cahaya 100 lux masih ok),
        // tapi study lebih berat ke cahaya → skor lebih rendah di 100 lux.
        assertTrue(study < gaming)
    }

    @Test
    fun `notes mention weak wifi`() {
        val m = SpotMetrics(wifiRssiDbm = -82)
        val (scores, _) = ScoreEngine.scoreSpot(m, WorkMode.WORK)
        val notes = ScoreEngine.notesFor(m, scores)
        assertTrue(notes.any { "lemah" in it })
    }

    @Test
    fun `sun azimuth daylight for known location`() {
        // Jakarta, siang ~05:00 UTC — matahari harus di atas horizon
        val epoch = 1758264000000L // 2025-09-19 05:00 UTC
        val az = SunPosition.azimuthDeg(epoch, -6.2, 106.8)
        assertNotNull(az)
        assertTrue(az!! in 0f..360f)
        // Malam hari harus null
        val night = SunPosition.azimuthDeg(epoch + 12 * 3600_000L, -6.2, 106.8)
        // 17:00 UTC = ~00:00 WIB → null atau masih ada? diharapkan null
        assertNull(night)
    }

    @Test
    fun `angular diff wraps correctly`() {
        assertEquals(10f, SunPosition.angularDiff(5f, 355f), 0.01f)
        assertEquals(180f, SunPosition.angularDiff(0f, 180f), 0.01f)
    }
}
