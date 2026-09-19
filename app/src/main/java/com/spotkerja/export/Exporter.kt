package com.spotkerja.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val label: String, val mime: String, val ext: String) {
    PNG("PNG infographic", "image/png", "png"),
    PDF("PDF report", "application/pdf", "pdf"),
    DOCX("Word document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    TEXT("Text summary", "text/plain", "txt"),
}

/** Export/share hasil scan: teks, PNG infografis, PDF, atau DOCX. */
object Exporter {

    fun summaryText(session: ScanSession): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(session.createdAtEpochMs))
        sb.appendLine("Spotkerja — scan report (${session.mode})")
        sb.appendLine(date)
        sb.appendLine()
        ScoreEngine.bestSpot(session.spots)?.let {
            sb.appendLine("Best spot: ${it.label} (${"%.0f".format(it.totalScore)}/100)")
        }
        if (ScoreEngine.isCloseCall(session.spots)) sb.appendLine("(Top spots are nearly tied)")
        sb.appendLine()
        session.spots.sortedByDescending { it.totalScore }.forEach { s ->
            sb.appendLine("== ${s.label} — ${"%.0f".format(s.totalScore)}/100 ==")
            val m = s.metrics
            m.wifiRssiDbm?.let { sb.appendLine("  Wi-Fi: $it dBm" + (m.wifiBand?.let { b -> " ($b)" } ?: "")) }
            m.pingAvgMs?.let {
                sb.appendLine("  Ping: ≈${"%.0f".format(it)} ms, jitter ≈${"%.0f".format(m.pingJitterMs ?: 0f)} ms, loss ${"%.1f".format(m.packetLossPct ?: 0f)}%")
            }
            m.luxAvg?.let { sb.appendLine("  Light: ≈${it.toInt()} lux") }
            m.noiseDbAvg?.let { sb.appendLine("  Noise: ≈${it.toInt()} dB (est.)") }
            m.cellularDbm?.let { sb.appendLine("  Cellular: $it dBm") }
            s.notes.forEach { sb.appendLine("  - $it") }
            sb.appendLine()
        }
        sb.appendLine("— Practical estimates from phone sensors, not scientific measurements.")
        return sb.toString()
    }

    suspend fun export(ctx: Context, session: ScanSession, format: ExportFormat) =
        withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
            val base = "spotkerja-${session.id}"
            val file = File(dir, "$base.${format.ext}")
            when (format) {
                ExportFormat.PNG -> Infographic.writePng(session, file)
                ExportFormat.PDF -> Infographic.writePdf(session, file)
                ExportFormat.DOCX -> Docx.write(session, file)
                ExportFormat.TEXT -> file.writeText(summaryText(session))
            }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mime
                putExtra(Intent.EXTRA_SUBJECT, "Spotkerja — ${session.mode} ${session.bestSpotLabel ?: ""}")
                if (format == ExportFormat.TEXT) {
                    putExtra(Intent.EXTRA_TEXT, summaryText(session))
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                ctx.startActivity(
                    Intent.createChooser(intent, "Share results")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}
