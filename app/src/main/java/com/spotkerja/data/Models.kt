package com.spotkerja.data

import kotlinx.serialization.Serializable
import com.spotkerja.settings.ScanOptions

/** Evaluation mode — each mode has its own metric weights and ideal light band. */
enum class WorkMode(val label: String, val description: String) {
    WORK("Work", "Productive work: stable connection, good light, quiet room"),
    STUDY("Study", "Deep focus: lighting and quietness matter most"),
    GAMING("Gaming", "Latency, jitter and packet loss are critical"),
    VIDEO_CALL("Video Call", "Realtime connection, face lighting, low noise"),
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
    val wifiCongestion: Float = 0f,
)

/** Metrik mentah hasil scan satu spot (rata-rata selama durasi scan). */
@Serializable
data class SpotMetrics(
    val wifiRssiDbm: Int? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val wifiBand: String? = null,
    val wifiCongestion: Int? = null,  // jumlah AP lain di kanal yang sama
    val pingAvgMs: Float? = null,
    val pingJitterMs: Float? = null,
    val packetLossPct: Float? = null,
    val pingUnreachable: Boolean = false,  // target diset tapi tak pernah menjawab
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

    // --- Mesh / radio detail (wifi-analyzer style) ---
    val wifiSsid: String? = null,
    val bssid: String? = null,
    val channelWidthMhz: Int? = null,
    /** Jumlah AP lain yang menyiarkan SSID sama (mesh/extender). */
    val meshApCount: Int? = null,
    /** Berapa kali BSSID berpindah selama scan — roaming aktif. */
    val roamCount: Int? = null,
    val rssiMinDbm: Int? = null,
    val rssiMaxDbm: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val rxLinkSpeedMbps: Int? = null,
    /** Estimasi throughput TCP ≈ 55% dari link PHY — kasar, bukan iperf. */
    val estThroughputMbps: Float? = null,

    // --- Internet readiness ---
    /** "ok" (validated) | "captive" | "limited" | "none". null = tidak dicek. */
    val internetState: String? = null,

    // --- Route quality ---
    /** Hop dari ping TTL traceroute, dipakai display; format "ip · ms". */
    val routeHops: List<String> = emptyList(),
    val routeTarget: String? = null,

    // --- Sound events (YAMNet) ---
    /** Label suara dominan selama scan (YAMNet top-1 tally, Silence diabaikan). */
    val soundTop: String? = null,
    val soundLabels: List<String> = emptyList(),

    // --- Environment sensors (fusion + auto-discovery) ---
    val pressureHpa: Float? = null,
    val altitudeM: Float? = null,
    val humidityPct: Float? = null,
    val ambientTempC: Float? = null,
    val magneticUt: Float? = null,
    val stepsDuringScan: Int? = null,
    /** Nama sensor opsional yang terbaca di device ini. */
    val sensorsFound: List<String> = emptyList(),
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
    val wifiCongestion: Float? = null,
)

@Serializable
data class SpotResult(
    val label: String,
    val durationSec: Int,
    val metrics: SpotMetrics,
    val scores: MetricScores,
    val totalScore: Float,
    val notes: List<String> = emptyList(),
    /** Keyakinan hasil 0–100: cakupan metrik yang berhasil diukur. */
    val confidencePct: Int? = null,
    /** Posisi di mini floor plan (grid), null = belum ditempatkan. */
    val mapX: Int? = null,
    val mapY: Int? = null,
)

@Serializable
data class ScanSession(
    val id: String,
    val createdAtEpochMs: Long,
    val mode: String,
    val spots: List<SpotResult>,
    val bestSpotLabel: String?,
    /** Opsi scan yang dipakai saat sesi ini — agar hasil lama tetap interpretable. */
    val optionsUsed: ScanOptions? = null,
    /** Profil ruangan opsional ("Home", "Kantor") — diisi manual dari history. */
    val room: String? = null,
    /** Blind test: label spot disembunyikan sampai user memilih favorit. */
    val blind: Boolean = false,
    /** Label spot yang dipilih user sebagai favorit (feel) pada blind test. */
    val userPickLabel: String? = null,
    /** Lokasi saat scan (dibulatkan ~1 km) — untuk glare forecast offline. */
    val latDeg: Double? = null,
    val lonDeg: Double? = null,
)
