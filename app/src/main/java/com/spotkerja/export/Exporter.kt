package com.spotkerja.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Export/share hasil scan sebagai teks + file CSV. */
object Exporter {

    fun summaryText(session: ScanSession): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(session.createdAtEpochMs))
        sb.appendLine("Spotkerja — hasil scan (${session.mode})")
        sb.appendLine(date)
        sb.appendLine()
        val best = ScoreEngine.bestSpot(session.spots)
        best?.let { sb.appendLine("Best Spot: ${it.label} (${"%.0f".format(it.totalScore)}/100)") }
        if (ScoreEngine.isCloseCall(session.spots)) sb.appendLine("(Selisih skor tipis — spot teratas hampir imbang)")
        sb.appendLine()
        session.spots.sortedByDescending { it.totalScore }.forEach { s ->
            sb.appendLine("== ${s.label} — ${"%.0f".format(s.totalScore)}/100 ==")
            val m = s.metrics
            m.wifiRssiDbm?.let { sb.appendLine("  Wi-Fi: $it dBm" + (m.wifiBand?.let { b -> " ($b)" } ?: "")) }
            m.pingAvgMs?.let { sb.appendLine("  Ping router: ≈${"%.0f".format(it)} ms, jitter ${"%.0f".format(m.pingJitterMs ?: 0f)} ms, loss ${"%.1f".format(m.packetLossPct ?: 0f)}%") }
            m.luxAvg?.let { sb.appendLine("  Cahaya: ≈${it.toInt()} lux") }
            m.noiseDbAvg?.let { sb.appendLine("  Noise: ≈${it.toInt()} dB (est.)") }
            m.cellularDbm?.let { sb.appendLine("  Seluler: $it dBm") }
            s.notes.forEach { sb.appendLine("  - $it") }
            sb.appendLine()
        }
        sb.appendLine("— Estimasi praktis berbasis sensor HP, bukan pengukuran ilmiah.")
        return sb.toString()
    }

    fun csv(session: ScanSession): String {
        val sb = StringBuilder()
        sb.appendLine("spot,score,wifi_rssi_dbm,ping_ms,jitter_ms,packet_loss_pct,lux,noise_db,cellular_dbm")
        session.spots.forEach { s ->
            val m = s.metrics
            sb.appendLine(listOf(
                s.label, "%.1f".format(s.totalScore),
                m.wifiRssiDbm?.toString() ?: "",
                m.pingAvgMs?.let { "%.1f".format(it) } ?: "",
                m.pingJitterMs?.let { "%.1f".format(it) } ?: "",
                m.packetLossPct?.let { "%.1f".format(it) } ?: "",
                m.luxAvg?.let { "%.0f".format(it) } ?: "",
                m.noiseDbAvg?.let { "%.0f".format(it) } ?: "",
                m.cellularDbm?.toString() ?: "",
            ).joinToString(","))
        }
        return sb.toString()
    }

    suspend fun share(ctx: Context, session: ScanSession) = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(dir, "spotkerja-${session.id}.csv").apply { writeText(csv(session)) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", csvFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Spotkerja — ${session.mode} ${session.bestSpotLabel ?: ""}")
            putExtra(Intent.EXTRA_TEXT, summaryText(session))
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            ctx.startActivity(Intent.createChooser(intent, "Bagikan hasil").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
