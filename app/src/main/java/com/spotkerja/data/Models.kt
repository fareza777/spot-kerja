package com.spotkerja.data

import kotlinx.serialization.Serializable

/** Mode penilaian — tiap mode punya bobot dan rentang cahaya ideal berbeda. */
enum class WorkMode(val label: String, val description: String) {
    WORK("Work", "Kerja produktif: koneksi stabil, cahaya cukup, suasana tenang"),
    STUDY("Study", "Belajar fokus: cahaya dan keheningan paling penting"),
    GAMING("Gaming", "Gaming: latency, jitter, dan packet loss paling kritikal"),
    VIDEO_CALL("Video Call", "Video call: koneksi realtime, cahaya wajah, minim noise"),
}

/** Bobot tiap metrik per mode. Total tidak harus 1 — selalu dinormalisasi. */
@Serializable
data class MetricWeights(
    val wifi: Float = 0f,
    val ping: Float = 0f,
    val jitter: Float = 0f,
    val packetLoss: Float = 0f,
    val light: Float = 0f,
    val noise: Float = 0f,
    val orientation: Float = 0f,
    val cellular: Float = 0f,
)

/** Metrik mentah hasil scan satu spot (rata-rata selama durasi scan). */
@Serializable
data class SpotMetrics(
    val wifiRssiDbm: Int? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val wifiBand: String? = null,
    val pingAvgMs: Float? = null,
    val pingJitterMs: Float? = null,
    val packetLossPct: Float? = null,
    val luxAvg: Float? = null,
    val luxStdDev: Float? = null,
    val noiseDbAvg: Float? = null,
    val azimuthDeg: Float? = null,
    val lightDirectionDeg: Float? = null,
    val sunAzimuthDeg: Float? = null,
    val glareRisk: Boolean? = null,
    val cellularDbm: Int? = null,
    val wifiSamples: Int = 0,
    val pingSamples: Int = 0,
    val luxSamples: Int = 0,
    val noiseSamples: Int = 0,
)

/** Sub-skor 0–100 per metrik. null = metrik tidak tersedia di device. */
@Serializable
data class MetricScores(
    val wifi: Float? = null,
    val ping: Float? = null,
    val jitter: Float? = null,
    val packetLoss: Float? = null,
    val light: Float? = null,
    val noise: Float? = null,
    val orientation: Float? = null,
    val cellular: Float? = null,
)

@Serializable
data class SpotResult(
    val label: String,
    val durationSec: Int,
    val metrics: SpotMetrics,
    val scores: MetricScores,
    val totalScore: Float,
    val notes: List<String> = emptyList(),
)

@Serializable
data class ScanSession(
    val id: String,
    val createdAtEpochMs: Long,
    val mode: String,
    val spots: List<SpotResult>,
    val bestSpotLabel: String?,
)
