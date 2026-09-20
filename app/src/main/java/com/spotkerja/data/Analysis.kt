package com.spotkerja.data

import kotlin.math.min

/**
 * Mode-specific verdict for the winning spot: a headline plus plain-language
 * bullets explaining what the measured values mean for that use case.
 * All heuristics are practical estimates — same spirit as the scores.
 */
object Analysis {

    data class Result(val headline: String, val summary: String, val bullets: List<String>)

    fun forMode(mode: WorkMode, best: SpotResult): Result {
        val m = best.metrics
        val s = best.scores
        return when (mode) {
            WorkMode.GAMING -> gaming(m, s, best.totalScore)
            WorkMode.VIDEO_CALL -> videoCall(m, s, best.totalScore)
            WorkMode.STUDY -> study(m, s, best.totalScore)
            WorkMode.WORK -> work(m, s, best.totalScore)
        }
    }

    private fun grade(score: Float): Int = when {
        score >= 75f -> 2   // good
        score >= 50f -> 1   // mid
        else -> 0           // bad
    }

    private fun gaming(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.pingAvgMs?.let {
            bullets += "Ping ≈${it.toInt()} ms to router — " + when {
                it < 15f -> "excellent for competitive play"
                it < 35f -> "fine for most online games"
                it < 70f -> "playable, but twitchy titles may feel laggy"
                else -> "high — expect noticeable delay"
            }
        }
        m.pingJitterMs?.let {
            bullets += "Jitter ≈${it.toInt()} ms — " + when {
                it < 8f -> "very stable connection"
                it < 20f -> "stable enough for casual play"
                else -> "unstable — spikes will hurt online play"
            }
        }
        m.packetLossPct?.let {
            if (it > 0.5f) bullets += "Packet loss ≈${"%.1f".format(it)}% — " +
                if (it > 3f) "frequent stutters/rubber-banding likely"
                else "occasional hiccups possible"
            else bullets += "No meaningful packet loss — smooth traffic"
        }
        m.wifiRssiDbm?.let {
            bullets += "Wi-Fi $it dBm — " + when {
                it >= -55 -> "strong signal, good pick for gaming"
                it >= -70 -> "decent signal"
                else -> "weak signal — latency spikes more likely"
            }
        }
        m.noiseDbAvg?.let {
            if (it > 60f) bullets += "Noisy spot (≈${it.toInt()} dB) — consider a headset"
        }

        val headline = when {
            s.ping != null && s.packetLoss != null &&
                min(s.ping, s.packetLoss) >= 70 -> "Ranked-ready"
            total >= 60 -> "Good for casual gaming"
            else -> "Rough spot for gaming"
        }
        return Result(headline, "How this spot treats online play:", bullets)
    }

    private fun videoCall(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 150f -> "dim, your face may look underexposed on camera"
                it <= 600f -> "nicely lit for video calls"
                else -> "very bright — watch for backlight/glow"
            }
        }
        if (m.azimuthDeg != null && m.lightDirectionDeg != null &&
            SunPosition.angularDiff(m.azimuthDeg, m.lightDirectionDeg) < 45f) {
            bullets += "Facing the light source — face well-lit on camera"
        }
        m.pingAvgMs?.let {
            bullets += "Ping ≈${it.toInt()} ms" + (m.pingJitterMs?.let { " · jitter ≈${it.toInt()} ms" } ?: "") +
                " — " + when {
                    it < 35f && (m.pingJitterMs ?: 0f) < 15f -> "calls should stay smooth"
                    it < 80f -> "usable, occasional stutter possible"
                    else -> "calls may freeze or drop"
                }
        }
        m.noiseDbAvg?.let {
            if (it > 55f) bullets += "Background noise ≈${it.toInt()} dB — mute between turns helps"
        }

        val headline = when {
            s.light != null && s.ping != null && min(s.light, s.ping) >= 70 -> "Call-ready"
            total >= 55 -> "Calls will work, with quirks"
            else -> "Choppy calls likely here"
        }
        return Result(headline, "How this spot treats your video calls:", bullets)
    }

    private fun work(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.noiseDbAvg?.let {
            bullets += "Noise ≈${it.toInt()} dB — " + when {
                it < 40f -> "very quiet, easy to focus"
                it < 55f -> "calm enough for most work"
                else -> "busy soundscape — focus may suffer"
            }
        }
        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 250f -> "a bit dim for long desk work"
                it <= 700f -> "comfortable working brightness"
                else -> "very bright — possible screen glare"
            }
        }
        m.wifiRssiDbm?.let {
            bullets += "Wi-Fi $it dBm" + (m.pingAvgMs?.let { " · ping ≈${it.toInt()} ms" } ?: "") +
                " — " + when {
                    it >= -60 -> "solid for docs, calls and browsing"
                    it >= -72 -> "fine for most work"
                    else -> "weak — cloud tools may lag"
                }
        }

        val headline = when {
            s.noise != null && s.light != null && min(s.noise, s.light) >= 70 -> "Deep-work ready"
            total >= 55 -> "Workable spot"
            else -> "Distracting for focused work"
        }
        return Result(headline, "How this spot treats focused work:", bullets)
    }

    private fun study(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.noiseDbAvg?.let {
            bullets += "Noise ≈${it.toInt()} dB — " + when {
                it < 38f -> "quiet enough for reading"
                it < 52f -> "mild ambient noise"
                else -> "loud — concentration will be hard"
            }
        }
        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 300f -> "below reading-comfort range"
                it <= 800f -> "good for reading and notes"
                else -> "harsh brightness — eyes may tire"
            }
        }
        m.wifiRssiDbm?.let {
            if (it < -72f) bullets += "Wi-Fi $it dBm — weak for online materials"
        }

        val headline = when {
            s.noise != null && s.light != null && min(s.noise, s.light) >= 72 -> "Focus-friendly"
            total >= 55 -> "Decent for studying"
            else -> "Hard to focus here"
        }
        return Result(headline, "How this spot treats a study session:", bullets)
    }
}
