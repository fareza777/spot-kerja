package com.spotkerja.data

/**
 * Mode-specific verdict for the winning spot: a headline plus plain-language
 * bullets explaining what the measured values mean for that use case.
 * Thresholds mirror ScoreEngine's scoring bands so words match the numbers.
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

    /** Weakest score among the metrics that matter for the mode; null → total. */
    private fun weakest(total: Float, vararg scores: Float?): Float {
        val present = scores.filterNotNull()
        return if (present.isEmpty()) total else present.min()
    }

    private fun fmt(v: Float): String = if (v == v.toLong().toFloat())
        "${v.toLong()}" else "%.1f".format(v)

    private fun gaming(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.pingAvgMs?.let {
            bullets += "Ping ≈${it.toInt()} ms to router — " + when {
                it < 15f -> "competitive-grade latency"
                it < 35f -> "smooth for most online games"
                it < 70f -> "playable; shooters/racing will feel sluggish"
                else -> "too slow — actions register noticeably late"
            }
        }
        m.pingJitterMs?.let {
            bullets += "Jitter ≈${it.toInt()} ms — " + when {
                it < 8f -> "timing stays consistent frame to frame"
                it < 20f -> "mildly uneven, acceptable for casual play"
                else -> "spiky — sudden lag bursts mid-match"
            }
        }
        m.packetLossPct?.let {
            bullets += when {
                it <= 0.5f -> "≈${fmt(it)}% packet loss — traffic is clean"
                it <= 3f -> "≈${fmt(it)}% packet loss — occasional stutter or teleport"
                else -> "≈${fmt(it)}% packet loss — rubber-banding is likely"
            }
        }
        m.wifiRssiDbm?.let {
            bullets += "Wi-Fi $it dBm — " + when {
                it >= -55 -> "strong signal headroom"
                it >= -70 -> {
                    if (m.pingAvgMs != null && m.pingAvgMs < 35f)
                        "latency fine now, but the signal margin is thin"
                    else "decent signal"
                }
                else -> "weak — latency spikes are likely under load"
            }
        }
        m.noiseDbAvg?.let {
            if (it > 60f) bullets += "≈${it.toInt()} dB ambient noise — a headset is advised"
        }
        if (m.pingAvgMs == null && m.packetLossPct == null)
            bullets += "Latency wasn't measured — verdict leans on signal strength only"

        val w = weakest(total, s.ping, s.jitter, s.packetLoss)
        val headline = when {
            m.pingAvgMs == null && s.wifi == null -> "Not enough data to judge"
            w >= 75f -> "Ranked-ready"
            w >= 55f -> "Good for casual gaming"
            w >= 35f -> "Playable, but compromised"
            else -> "Rough spot for gaming"
        }
        return Result(headline, "What this spot means for online play:", bullets)
    }

    private fun videoCall(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 150f -> "dim; your face will look underexposed on camera"
                it <= 600f -> "well-lit face for video calls"
                else -> "very bright — risk of washed-out or backlit look"
            }
        }
        if (m.azimuthDeg != null && m.lightDirectionDeg != null) {
            val d = SunPosition.angularDiff(m.azimuthDeg, m.lightDirectionDeg)
            if (d < 45f) bullets += "Facing the light source — your face stays lit"
            else if (d > 135f) bullets += "Back to the light source — camera may silhouette you"
        }
        if (m.glareRisk == true) bullets += "Facing the sun — glare can wash out the screen"
        m.pingAvgMs?.let {
            val jit = m.pingJitterMs
            bullets += "Ping ≈${it.toInt()} ms" + (jit?.let { " · jitter ≈${it.toInt()} ms" } ?: "") +
                " — " + when {
                    it < 35f && (jit ?: 0f) < 15f -> "audio/video should stay in sync"
                    it < 80f -> "usable; brief stutters possible"
                    else -> "freezing or dropped audio likely"
                }
        }
        m.packetLossPct?.let {
            if (it > 2f) bullets += "≈${fmt(it)}% packet loss — calls may cut out intermittently"
        }
        m.noiseDbAvg?.let {
            if (it > 55f) bullets += "≈${it.toInt()} dB background noise — mute between turns"
        }
        if (m.pingAvgMs == null && m.packetLossPct == null)
            bullets += "Connection latency wasn't measured — call verdict is partial"

        val w = weakest(total, s.light, s.ping, s.jitter)
        val headline = when {
            w >= 75f -> "Call-ready"
            w >= 55f -> "Calls will work, with quirks"
            w >= 35f -> "Borderline for video calls"
            else -> "Choppy calls likely here"
        }
        return Result(headline, "What this spot means for video calls:", bullets)
    }

    private fun work(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.noiseDbAvg?.let {
            bullets += "Noise ≈${it.toInt()} dB — " + when {
                it < 40f -> "quiet enough for deep focus"
                it < 55f -> "calm; fine for most work"
                it < 70f -> "busy soundscape — focus will leak"
                else -> "loud — sustained concentration is unlikely"
            }
        }
        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 250f -> "dim for long desk sessions"
                it <= 700f -> "comfortable working brightness"
                else -> "very bright — screen glare possible"
            }
        }
        m.luxStdDev?.let {
            if (it > 120f) bullets += "Light kept shifting during the scan — mildly distracting"
        }
        m.wifiRssiDbm?.let {
            bullets += "Wi-Fi $it dBm" + (m.pingAvgMs?.let { " · ping ≈${it.toInt()} ms" } ?: "") +
                " — " + when {
                    it >= -60 -> "solid for docs, calls and cloud tools"
                    it >= -72 -> "workable; heavy sync jobs may crawl"
                    else -> "weak — cloud apps will lag"
                }
        }
        if (m.wifiRssiDbm == null)
            bullets += "Wi-Fi wasn't detected — connection quality unknown here"

        val w = weakest(total, s.noise, s.light, s.wifi)
        val headline = when {
            w >= 75f -> "Deep-work ready"
            w >= 55f -> "Workable spot"
            w >= 35f -> "Okay for light tasks only"
            else -> "Distracting for focused work"
        }
        return Result(headline, "What this spot means for focused work:", bullets)
    }

    private fun study(m: SpotMetrics, s: MetricScores, total: Float): Result {
        val bullets = mutableListOf<String>()

        m.noiseDbAvg?.let {
            bullets += "Noise ≈${it.toInt()} dB — " + when {
                it < 38f -> "quiet enough to read without drifting"
                it < 52f -> "mild ambient noise"
                else -> "loud — hard to hold attention"
            }
        }
        m.luxAvg?.let {
            bullets += "Light ≈${it.toInt()} lux — " + when {
                it < 300f -> "below comfortable reading range"
                it <= 800f -> "good for reading and note-taking"
                else -> "harsh — eyes will tire over a session"
            }
        }
        m.luxStdDev?.let {
            if (it > 120f) bullets += "Light fluctuated during the scan — subtle distraction"
        }
        m.wifiRssiDbm?.let {
            if (it < -72f) bullets += "Wi-Fi $it dBm — weak for online materials"
        }

        val w = weakest(total, s.noise, s.light)
        val headline = when {
            w >= 75f -> "Focus-friendly"
            w >= 55f -> "Decent for studying"
            w >= 35f -> "Usable with effort"
            else -> "Hard to focus here"
        }
        return Result(headline, "What this spot means for a study session:", bullets)
    }
}
