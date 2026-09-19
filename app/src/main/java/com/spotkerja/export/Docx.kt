package com.spotkerja.export

import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Penulis .docx minimal (OOXML) — bisa dibuka Word/Google Docs. */
object Docx {

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun p(text: String, bold: Boolean = false, sizeHalfPt: Int = 24) =
        "<w:p><w:r><w:rPr>${if (bold) "<w:b/>" else ""}<w:sz w:val=\"$sizeHalfPt\"/></w:rPr>" +
            "<w:t xml:space=\"preserve\">${esc(text)}</w:t></w:r></w:p>"

    /** Paragraf berisi gambar PNG inline (lebar 6 inci). */
    private fun imagePara(cxEmu: Long, cyEmu: Long) =
        """<w:p><w:r><w:drawing><wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" distT="0" distB="0" distL="0" distR="0"><wp:extent cx="$cxEmu" cy="$cyEmu"/><wp:docPr id="1" name="Infographic"/><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="0" name="infographic.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" r:embed="rIdImg1"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$cxEmu" cy="$cyEmu"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"""

    fun write(session: ScanSession, out: File, infographicPng: File? = null) {
        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(session.createdAtEpochMs))
        val best = ScoreEngine.bestSpot(session.spots)

        val doc = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
            append(p("SpotWise — Scan Report", bold = true, sizeHalfPt = 56))
            append(p("${session.mode} mode • $date"))
            append(p(""))
            best?.let { append(p("BEST SPOT: ${it.label} — ${"%.0f".format(it.totalScore)}/100", bold = true, sizeHalfPt = 36)) }
            if (ScoreEngine.isCloseCall(session.spots)) append(p("(Top spots are nearly tied)"))
            if (infographicPng != null) {
                append(p(""))
                // 1080x1520 px -> 6in lebar, aspek tetap (1 EMU = 1/914400 in).
                append(imagePara(5486400, 7722667))
            }
            append(p(""))
            session.spots.sortedByDescending { it.totalScore }.forEachIndexed { i, s ->
                append(p("${i + 1}. ${s.label} — ${"%.0f".format(s.totalScore)}/100", bold = true, sizeHalfPt = 30))
                val m = s.metrics
                m.wifiRssiDbm?.let { append(p("   Wi-Fi: $it dBm")) }
                m.pingAvgMs?.let {
                    append(p("   Ping: ≈${it.toInt()} ms, jitter ≈${"%.0f".format(m.pingJitterMs ?: 0f)} ms, loss ${"%.1f".format(m.packetLossPct ?: 0f)}%"))
                }
                m.luxAvg?.let { append(p("   Light: ≈${it.toInt()} lux")) }
                m.noiseDbAvg?.let { append(p("   Noise: ≈${it.toInt()} dB (est.)")) }
                m.cellularDbm?.let { append(p("   Cellular: $it dBm")) }
                s.notes.forEach { append(p("   • $it")) }
                append(p(""))
            }
            append(p("Practical estimates from phone sensors — not scientific measurements.", sizeHalfPt = 20))
            append("</w:body></w:document>")
        }

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>${if (infographicPng != null) """<Default Extension="png" ContentType="image/png"/>""" else ""}<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

        val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdImg1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/infographic.png"/></Relationships>"""

        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { z ->
            z.putNextEntry(ZipEntry("[Content_Types].xml"))
            z.write(contentTypes.toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("_rels/.rels"))
            z.write(rels.toByteArray()); z.closeEntry()
            if (infographicPng != null) {
                z.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
                z.write(docRels.toByteArray()); z.closeEntry()
                z.putNextEntry(ZipEntry("word/media/infographic.png"))
                infographicPng.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
            z.putNextEntry(ZipEntry("word/document.xml"))
            z.write(doc.toByteArray()); z.closeEntry()
        }
    }
}
