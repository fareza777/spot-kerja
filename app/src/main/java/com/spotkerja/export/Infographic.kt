package com.spotkerja.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Infographic renderer — satu code path menggambar ke Bitmap (PNG) atau
 * halaman PdfDocument (PDF). Ukuran canvas 1080x1520.
 */
object Infographic {

    const val W = 1080
    const val H = 1520

    private val BG = Color.parseColor("#070D0B")
    private val CARD = Color.parseColor("#101A16")
    private val BORDER = Color.parseColor("#223330")
    private val ACCENT = Color.parseColor("#4ED8C3")
    private val ACCENT2 = Color.parseColor("#5CC8FF")
    private val GOLD = Color.parseColor("#F2B84B")
    private val TEXT = Color.parseColor("#EFF7F3")
    private val DIM = Color.parseColor("#93A8A0")
    private val BAD = Color.parseColor("#E5655F")

    private fun scoreColor(s: Float) = when {
        s >= 70f -> ACCENT
        s >= 45f -> GOLD
        else -> BAD
    }

    private fun paint(color: Int, size: Float = 32f, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        isFakeBoldText = bold
    }

    fun render(canvas: Canvas, session: ScanSession) {
        val sorted = session.spots.sortedByDescending { it.totalScore }
        val best = sorted.firstOrNull()

        canvas.drawColor(BG)
        val title = paint(TEXT, 64f, bold = true)
        val dim = paint(DIM, 30f)
        val dimSmall = paint(DIM, 26f)

        // Header
        var y = 96f
        canvas.drawRoundRect(RectF(60f, 40f, 120f, 100f), 16f, 16f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT; alpha = 60 })
        canvas.drawText("S", 82f, 92f, paint(ACCENT, 46f, bold = true))
        canvas.drawText("SpotWise", 148f, y, title)
        y += 40f
        canvas.drawText(
            "${session.mode} mode • " +
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(session.createdAtEpochMs)),
            148f, y, dim,
        )
        canvas.drawLine(60f, y + 28f, W - 60f, y + 28f, paint(BORDER, 2f))
        y += 92f

        // Best spot hero
        best?.let { b ->
            val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(60f, y, W - 60f, y + 190f,
                    Color.parseColor("#123B33"), Color.parseColor("#0D2A33"), Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(60f, y, W - 60f, y + 190f), 28f, 28f, heroPaint)
            canvas.drawRoundRect(RectF(84f, y + 24f, 236f, y + 64f), 12f, 12f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD; alpha = 50 })
            canvas.drawText("BEST SPOT", 104f, y + 53f, paint(GOLD, 28f, bold = true))
            canvas.drawText(b.label.take(20), 84f, y + 136f, paint(TEXT, 58f, bold = true))

            // Score ring
            val cx = W - 200f; val cy = y + 95f; val r = 68f
            canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BORDER; style = Paint.Style.STROKE; strokeWidth = 12f })
            val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = scoreColor(b.totalScore); style = Paint.Style.STROKE
                strokeWidth = 12f; strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r),
                -90f, 360f * b.totalScore / 100f, false, arc)
            val sp = paint(scoreColor(b.totalScore), 52f, bold = true)
            val tw = sp.measureText("%.0f".format(b.totalScore))
            canvas.drawText("%.0f".format(b.totalScore), cx - tw / 2, cy + 18f, sp)
            y += 230f
        }

        // Comparison bars
        canvas.drawText("SCORE COMPARISON", 60f, y, paint(DIM, 26f, bold = true))
        y += 44f
        sorted.forEachIndexed { i, s ->
            val barY = y
            canvas.drawText("${i + 1}", 60f, barY + 34f, paint(DIM, 34f, bold = true))
            canvas.drawText(s.label.take(16), 104f, barY + 34f, paint(TEXT, 32f, bold = true))
            val barLeft = 380f; val barRight = W - 180f; val barH = 30f
            canvas.drawRoundRect(RectF(barLeft, barY + 8f, barRight, barY + 8f + barH), 14f, 14f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BORDER })
            canvas.drawRoundRect(
                RectF(barLeft, barY + 8f, barLeft + (barRight - barLeft) * s.totalScore / 100f,
                    barY + 8f + barH), 14f, 14f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(barLeft, barY, barRight, barY,
                        scoreColor(s.totalScore), ACCENT2, Shader.TileMode.CLAMP)
                })
            canvas.drawText("%.0f".format(s.totalScore), W - 140f, barY + 36f,
                paint(scoreColor(s.totalScore), 36f, bold = true))
            y += 60f
        }
        y += 40f

        // Metric detail per spot (top 3 max to fit)
        canvas.drawText("SPOT BREAKDOWN", 60f, y, paint(DIM, 26f, bold = true))
        y += 40f
        sorted.take(3).forEach { s ->
            val cardH = 218f
            canvas.drawRoundRect(RectF(60f, y, W - 60f, y + cardH), 20f, 20f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
            val path = Path().apply {
                addRoundRect(RectF(60f, y, W - 60f, y + cardH), 20f, 20f, Path.Direction.CW)
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BORDER; style = Paint.Style.STROKE; strokeWidth = 2f })
            canvas.drawText(s.label.take(22), 84f, y + 50f, paint(TEXT, 38f, bold = true))
            canvas.drawText("%.0f / 100".format(s.totalScore), 84f, y + 88f,
                paint(scoreColor(s.totalScore), 34f, bold = true))
            val m = s.metrics
            val rows = listOf(
                "Wi-Fi" to (m.wifiRssiDbm?.let { "$it dBm" } ?: "—"),
                "Ping" to (m.pingAvgMs?.let { "≈${it.toInt()} ms" } ?: "—"),
                "Light" to (m.luxAvg?.let { "≈${it.toInt()} lx" } ?: "—"),
                "Noise" to (m.noiseDbAvg?.let { "≈${it.toInt()} dB" } ?: "—"),
            )
            var rx = 84f
            rows.forEach { (k, v) ->
                canvas.drawText(k, rx, y + 140f, dimSmall)
                canvas.drawText(v, rx, y + 178f, paint(TEXT, 32f, bold = true))
                rx += 240f
            }
            // mini score bars
            val bars = listOfNotNull(
                s.scores.wifi, s.scores.light, s.scores.noise, s.scores.ping
            ).take(4)
            var bx = 84f
            bars.forEach { sc ->
                canvas.drawRoundRect(RectF(bx, y + 192f, bx + 200f, y + 202f), 5f, 5f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BORDER })
                canvas.drawRoundRect(RectF(bx, y + 192f, bx + 200f * sc / 100f, y + 202f),
                    5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = scoreColor(sc) })
                bx += 220f
            }
            y += cardH + 18f
        }

        // Footer
        canvas.drawText(
            "Practical estimate from phone sensors — not a scientific measurement.",
            60f, H - 60f, dimSmall,
        )
        canvas.drawText("Generated by SpotWise", W - 320f, H - 60f, dimSmall)
    }

    fun toBitmap(session: ScanSession): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        render(Canvas(bmp), session)
        return bmp
    }

    fun writePng(session: ScanSession, out: File) {
        FileOutputStream(out).use { toBitmap(session).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun writePdf(session: ScanSession, out: File) {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(W, H, 1).create()
        val page = doc.startPage(info)
        render(page.canvas, session)
        doc.finishPage(page)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
    }
}
