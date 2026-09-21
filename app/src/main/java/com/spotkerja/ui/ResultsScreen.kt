package com.spotkerja.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotkerja.data.Analysis
import com.spotkerja.data.Insights
import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotResult
import com.spotkerja.data.SunPosition
import com.spotkerja.data.WorkMode
import com.spotkerja.export.ExportFormat
import com.spotkerja.ui.components.*
import com.spotkerja.ui.theme.*

@Composable
fun ResultsScreen(
    spots: List<SpotResult>,
    mode: WorkMode,
    onExport: (ExportFormat) -> Unit,
    onNewScan: () -> Unit,
    onBack: (() -> Unit)? = null,
    exporting: Boolean = false,
    bannerAd: (@Composable () -> Unit)? = null,
    /** Sesi penuh — untuk glare forecast, blind test, room map & profil ruangan. */
    session: ScanSession? = null,
    /** "Focus here" → FocusScreen dengan label spot (masked bila blind). */
    onFocusSpot: ((String) -> Unit)? = null,
    /** Assign spot ke sel floor plan (null = hapus). */
    onCellAssign: ((String, Int?, Int?) -> Unit)? = null,
    /** Blind test: user memilih spot yang terasa terbaik (label asli). */
    onPickFavorite: ((String) -> Unit)? = null,
    /** Set/hapus nama ruangan sesi ini. */
    onAssignRoom: ((String?) -> Unit)? = null,
    /** Buka posture check (kamera depan + MediaPipe Pose). */
    onPosture: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    val sorted = spots.sortedByDescending { it.totalScore }
    val best = sorted.firstOrNull()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Blind test: label asli disamarkan "Spot N" (urutan input) sampai user
    // memilih favorit — feel vs data dibandingkan di history.
    val blindActive = session?.blind == true && session.userPickLabel == null
    fun label(s: SpotResult): String = if (blindActive && session != null)
        "Spot ${session.spots.indexOfFirst { it.label == s.label } + 1}" else s.label

    var editingRoom by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        if (onBack != null || onAssignRoom != null) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Back")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (onAssignRoom != null) {
                        Surface(
                            onClick = { editingRoom = true },
                            shape = RoundedCornerShape(10.dp),
                            color = p.high,
                            border = BorderStroke(1.dp, p.border),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.MeetingRoom, null, Modifier.size(14.dp),
                                    tint = p.accent)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    session?.room ?: "Assign room",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.text, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        best?.let { b ->
            item {
                AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 4 }) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Box(Modifier.background(heroBrush()).padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = p.gold.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, p.gold.copy(alpha = 0.5f)),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Default.EmojiEvents, null,
                                                Modifier.size(13.dp), tint = p.gold)
                                            Spacer(Modifier.width(4.dp))
                                            Text("BEST SPOT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = p.gold)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(label(b), style = MaterialTheme.typography.headlineMedium,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (ScoreEngine.isCloseCall(spots)) {
                                        Text("Very close with the runner-up",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = p.textDim)
                                    }
                                }
                                ScoreRing(b.totalScore, "of 100", sizeDp = 100)
                            }
                        }
                    }
                }
            }
        }

        // Verdict untuk mode yang dipilih — menjelaskan apa arti skor best spot.
        best?.let { b ->
            item {
                val analysis = remember(b, mode) { Analysis.forMode(mode, b) }
                SectionHeader("Analysis — ${mode.label} mode")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, null, Modifier.size(20.dp),
                                tint = p.accent)
                            Spacer(Modifier.width(8.dp))
                            Text(analysis.headline,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold, color = p.accent)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(analysis.summary, style = MaterialTheme.typography.bodySmall,
                            color = p.textDim)
                        if (analysis.bullets.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            analysis.bullets.forEach {
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Icon(Icons.Default.ChevronRight, null,
                                        Modifier.size(14.dp).padding(top = 2.dp),
                                        tint = p.accentDim)
                                    Spacer(Modifier.width(5.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = p.text)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Score comparison")
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    CompareChart(sorted.map { label(it) to it.totalScore })
                }
            }
        }

        // Radar overlay metrik per-spot (maks 3 teratas). Sumbu yang tidak
        // punya data sama sekali (semua spot null) tidak ditampilkan.
        val allAxes = listOf("Wi-Fi", "Ping", "Jitter", "Loss", "Light",
            "Noise", "Facing", "Cell")
        val allSeries = sorted.take(3).map { s ->
            label(s) to listOf(s.scores.wifi, s.scores.ping, s.scores.jitter,
                s.scores.packetLoss, s.scores.light, s.scores.noise,
                s.scores.orientation, s.scores.cellular)
        }
        val usedIdx = allAxes.indices.filter { i ->
            allSeries.any { (_, vals) -> vals[i] != null }
        }
        val radarAxes = usedIdx.map { allAxes[it] }
        val radarSeries = allSeries.map { (l, v) -> l to usedIdx.map { v[it] } }
        if (radarAxes.size >= 3) {
            item {
                SectionHeader("Metric radar")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RadarChart(radarSeries, radarAxes)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            radarSeries.forEachIndexed { i, (label, _) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                                        .background(
                                            listOf(p.accent, p.gold, p.accent2)[i % 3]))
                                    Spacer(Modifier.width(6.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (sorted.any { it.confidencePct != null && it.confidencePct!! < 100 }) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Dim axes with no data are hidden. \"—\" in a spot card " +
                                    "means that metric wasn't measured.",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textDim.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Blind pick: pilih spot favorit berdasar feel — label masih tersembunyi.
        if (blindActive && onPickFavorite != null && session != null) {
            item {
                SectionHeader("Blind test")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Which spot felt best?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Labels stay hidden — pick after actually working there. " +
                            "We'll compare your feel with the data.",
                            style = MaterialTheme.typography.bodySmall, color = p.textDim)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            session.spots.forEachIndexed { i, s ->
                                Surface(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPickFavorite(s.label)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = p.accent.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.5f)),
                                ) {
                                    Text("Spot ${i + 1}",
                                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold, color = p.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (session?.blind == true && session.userPickLabel != null) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (session.userPickLabel == session.bestSpotLabel)
                                Icons.Default.Handshake else Icons.Default.Compare,
                            null, Modifier.size(18.dp),
                            tint = if (session.userPickLabel == session.bestSpotLabel)
                                p.accent else p.gold)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (session.userPickLabel == session.bestSpotLabel)
                                "Your pick matches the data — ${session.userPickLabel} wins."
                            else "You picked ${session.userPickLabel}; data scored " +
                                "${session.bestSpotLabel} highest.",
                            style = MaterialTheme.typography.bodySmall, color = p.textDim,
                        )
                    }
                }
            }
        }

        // Glare forecast: kapan matahari menyilaukan tiap spot (24 jam ke depan).
        session?.let { sess -> Insights.glareWindows(sess).takeIf { it.isNotEmpty() }?.let { sess to it } }
            ?.let { (sess, windows) ->
                item {
                    SectionHeader("Glare forecast")
                    Spacer(Modifier.height(10.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            windows.forEach { w ->
                                Row(Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbSunny, null, Modifier.size(14.dp),
                                        tint = p.gold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label(sess.spots.first { it.label == w.spotLabel }),
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold, color = p.text,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("glare likely ${w.text()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = p.textDim)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Estimated from the sun's path at this location.",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textDim.copy(alpha = 0.7f))
                        }
                    }
                }
            }

        // Mini floor plan: letakkan spot pada grid — peta skor ruangan.
        if (session != null && onCellAssign != null && session.spots.isNotEmpty()) {
            item {
                SectionHeader("Room map")
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        val cols = 6
                        val rows = 6
                        val placed = session.spots.filter { it.mapX != null && it.mapY != null }
                        val next = sorted.firstOrNull { it.mapX == null }
                        Text(
                            if (placed.size < session.spots.size)
                                "Tap a cell to place ${next?.let { label(it) } ?: "a spot"} " +
                                    "· tap a placed cell to clear"
                            else "Tap a placed cell to clear",
                            style = MaterialTheme.typography.bodySmall, color = p.textDim)
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (y in 0 until rows) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (x in 0 until cols) {
                                        val occ = session.spots.firstOrNull {
                                            it.mapX == x && it.mapY == y }
                                        Box(
                                            Modifier.weight(1f).aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (occ != null)
                                                        sc(occ.totalScore).copy(alpha = 0.35f)
                                                    else p.high.copy(alpha = 0.5f))
                                                .clickable {
                                                    if (occ != null)
                                                        onCellAssign(occ.label, null, null)
                                                    else next?.let {
                                                        onCellAssign(it.label, x, y)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (occ != null) {
                                                Text(
                                                    "${occ.totalScore.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold, color = p.text)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (placed.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            placed.forEach { s ->
                                Row(Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp))
                                        .background(sc(s.totalScore)))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${label(s)} — ${s.totalScore.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hint rescan bila ada risiko silau — posisi matahari bergeser tiap jam.
        if (sorted.any { it.metrics.glareRisk == true }) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, null, Modifier.size(18.dp), tint = p.gold)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Sun position shifts every hour — rescan in a few hours for a " +
                                "clearer glare picture.",
                            style = MaterialTheme.typography.bodySmall, color = p.textDim,
                        )
                    }
                }
            }
        }

        item { SectionHeader("Spot details") }
        itemsIndexed(sorted) { rank, spot -> SpotDetailCard(spot, rank, label(spot)) }

        item {
            SectionHeader("Export results")
            Spacer(Modifier.height(10.dp))
            if (exporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportChip(Icons.Default.Image, "PNG") { onExport(ExportFormat.PNG) }
                ExportChip(Icons.Default.PictureAsPdf, "PDF") { onExport(ExportFormat.PDF) }
                ExportChip(Icons.Default.Description, "Word") { onExport(ExportFormat.DOCX) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportChip(Icons.Default.TableChart, "CSV") { onExport(ExportFormat.CSV) }
                ExportChip(Icons.AutoMirrored.Filled.TextSnippet, "Text") { onExport(ExportFormat.TEXT) }
                ExportChip(Icons.Default.Share, "Card") { onExport(ExportFormat.CARD) }
            }
        }

        item {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNewScan()
                },
                Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.accent, contentColor = p.bg),
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Scan", fontWeight = FontWeight.Bold)
            }
            if (onFocusSpot != null && best != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFocusSpot(best.label)
                    },
                    Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Default.Timer, null, Modifier.size(18.dp), tint = p.accent)
                    Spacer(Modifier.width(6.dp))
                    Text("Focus at ${label(best)}", fontWeight = FontWeight.SemiBold,
                        color = p.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (onPosture != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPosture,
                    Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Default.AccessibilityNew, null,
                        Modifier.size(18.dp), tint = p.accent)
                    Spacer(Modifier.width(6.dp))
                    Text("Check posture", fontWeight = FontWeight.SemiBold,
                        color = p.accent)
                }
            }
            Text(
                "Work Spot Score combines Wi-Fi, ping/jitter/loss, light, noise, orientation " +
                    "& cellular — weights adapt to ${mode.label} mode. " +
                    "Practical estimate, not a scientific measurement.",
                style = MaterialTheme.typography.labelSmall,
                color = p.textDim.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        bannerAd?.let { item { it() } }
    }

    if (editingRoom) {
        var roomText by remember { mutableStateOf(session?.room ?: "") }
        AlertDialog(
            onDismissRequest = { editingRoom = false },
            title = { Text("Room profile") },
            text = {
                OutlinedTextField(
                    value = roomText, onValueChange = { roomText = it },
                    placeholder = { Text("e.g. Home, Office, Café") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAssignRoom?.invoke(roomText.ifBlank { null })
                    editingRoom = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingRoom = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun RowScope.ExportChip(icon: androidx.compose.ui.graphics.vector.ImageVector,
                                label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = p.accent)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = p.text)
        }
    }
}

/** Grid 4×3 luminansi kamera belakang — makin terang sel makin accent. */
@Composable
private fun LightMapGrid(cells: List<Int>) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 4) {
                    val v = (cells[r * 4 + c].coerceIn(0, 255)) / 255f
                    Box(
                        Modifier.size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(p.accent.copy(alpha = 0.08f + v * 0.85f)),
                    )
                }
            }
        }
        Text("Light map (camera)", style = MaterialTheme.typography.labelSmall,
            color = p.textDim.copy(alpha = 0.7f))
    }
}

/** Baris info kecil label→nilai untuk pembacaan ekstra (mesh, env, dst). */
@Composable
private fun InfoRow(label: String, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(110.dp),
            style = MaterialTheme.typography.labelSmall, color = p.textDim)
        Text(value, Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall, color = p.text,
            fontWeight = FontWeight.SemiBold)
    }
}

/** Pembacaan ekstra hasil deep metrics — hanya baris yang punya data tampil. */
@Composable
private fun ExtraReadings(m: com.spotkerja.data.SpotMetrics) {
    val p = LocalPalette.current
    val rows = buildList<Pair<String, String>> {
        m.internetState?.let {
            add("Internet" to when (it) {
                "ok" -> "online"
                "captive" -> "captive portal"
                "limited" -> "unverified"
                else -> "offline"
            })
        }
        if (m.wifiSsid != null || m.channelWidthMhz != null || m.wifiBand != null) {
            add("Radio" to listOfNotNull(
                m.wifiSsid, m.wifiBand,
                m.channelWidthMhz?.let { "$it MHz" }).joinToString(" · "))
        }
        if (m.rxLinkSpeedMbps != null || m.txLinkSpeedMbps != null) {
            add("Link speed" to
                "↓${m.rxLinkSpeedMbps ?: "—"} / ↑${m.txLinkSpeedMbps ?: "—"} Mbps" +
                (m.estThroughputMbps?.let { " · est. ≈${it.toInt()} Mbps" } ?: ""))
        }
        if (m.meshApCount != null || m.roamCount != null) {
            add("Mesh" to listOfNotNull(
                m.meshApCount?.let { "${it + 1} APs share SSID" },
                m.roamCount?.let { "roamed $it×" }).joinToString(" · "))
        }
        if (m.rssiMinDbm != null && m.rssiMaxDbm != null &&
            m.rssiMinDbm != m.rssiMaxDbm) {
            add("RSSI range" to "${m.rssiMinDbm}…${m.rssiMaxDbm} dBm")
        }
        if (m.routeHops.isNotEmpty()) {
            add("Route" to "${m.routeHops.size} hop(s) to ${m.routeTarget ?: "target"}")
        }
        val net = listOfNotNull(
            m.dnsMs?.let { "DNS ${it.toInt()} ms" },
            m.tcpMs?.let { "TCP${m.tcpPort?.let { p -> ":$p" } ?: ""} ${it.toInt()} ms" },
            m.tlsMs?.let { "TLS ${it.toInt()} ms" },
            m.udpState?.let { "QUIC $it" },
        )
        if (net.isNotEmpty()) add("Net probes" to net.joinToString(" · "))
        m.speechPct?.let {
            add("Speech" to "${it.toInt()}% of windows have voices")
        }
        if (m.camLumaAvg != null) {
            add("Light map" to listOfNotNull(
                "avg ${m.camLumaAvg.toInt()}/255",
                m.camLumaStd?.let { "uneven ±${it.toInt()}" },
                m.camHotspot?.let { "hotspot $it" }).joinToString(" · "))
        }
        m.magneticStdDevUt?.let {
            add("Magnetic" to "%.0f µT · ±%.1f µT".format(
                m.magneticUt ?: 0f, it) +
                if (it > 12f) " (unstable)" else "")
        }
        val therm = listOfNotNull(
            m.thermalStatus?.let { listOf("normal", "light", "moderate", "severe",
                "critical", "emergency", "shutdown").getOrElse(it) { "$it" } },
            m.thermalHeadroom?.let { "headroom %.0f%%".format(it * 100f) },
            m.batteryTempC?.let { "battery %.0f °C".format(it) },
        )
        if (therm.isNotEmpty()) add("Thermal" to therm.joinToString(" · "))
        if (m.soundLabels.isNotEmpty()) {
            add("Soundscape" to m.soundLabels.joinToString(" · "))
        }
        val env = listOfNotNull(
            m.ambientTempC?.let { "%.1f °C".format(it) },
            m.humidityPct?.let { "%.0f%% RH".format(it) },
            m.pressureHpa?.let { "%.0f hPa".format(it) },
            m.altitudeM?.let { "≈${it.toInt()} m alt." },
            m.magneticUt?.let { "%.0f µT".format(it) },
            m.stepsDuringScan?.let { "$it steps" },
        )
        if (env.isNotEmpty()) add("Environment" to env.joinToString(" · "))
        if (m.sensorsFound.isNotEmpty()) {
            add("Sensors" to "${m.sensorsFound.size} readable")
        }
    }
    if (rows.isEmpty() && m.routeHops.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = p.border)
    Spacer(Modifier.height(10.dp))
    Column {
        rows.forEach { (l, v) -> InfoRow(l, v) }
        if (m.routeHops.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            m.routeHops.forEachIndexed { i, hop ->
                Text("${i + 1}. $hop",
                    Modifier.padding(start = 14.dp, top = 1.dp),
                    style = MaterialTheme.typography.labelSmall, color = p.textDim)
            }
        }
        if (m.lightMap.size == 12) {
            Spacer(Modifier.height(6.dp))
            LightMapGrid(m.lightMap)
        }
        if (m.sensorsFound.isNotEmpty()) {
            Text(m.sensorsFound.joinToString(" · "),
                Modifier.padding(start = 14.dp, top = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = p.textDim.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SpotDetailCard(spot: SpotResult, rank: Int, displayLabel: String = spot.label) {
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    var expanded by remember { mutableStateOf(rank == 0) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayLabel, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${spot.durationSec}s • ${spot.metrics.wifiSamples +
                        spot.metrics.pingSamples + spot.metrics.luxSamples +
                        spot.metrics.noiseSamples} samples" +
                        (spot.confidencePct?.let { " • confidence $it%" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim)
                }
                Text("%.0f".format(spot.totalScore),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = sc(spot.totalScore))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        "Details", tint = p.textDim,
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    val m = spot.metrics
                    val s = spot.scores
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricBar("Wi-Fi", s.wifi, m.wifiRssiDbm?.let { "$it dBm" } ?: "—")
                        MetricBar("Ping", s.ping, m.pingAvgMs?.let { "≈${it.toInt()} ms" } ?: "—")
                        MetricBar("Jitter", s.jitter, m.pingJitterMs?.let { "≈${it.toInt()} ms" } ?: "—")
                        MetricBar("Packet loss", s.packetLoss,
                            m.packetLossPct?.let { "%.1f%%".format(it) } ?: "—")
                        MetricBar("Light", s.light, m.luxAvg?.let { "≈${it.toInt()} lux" } ?: "—")
                        MetricBar("Noise", s.noise, m.noiseDbAvg?.let { "≈${it.toInt()} dB" } ?: "—")
                        MetricBar("Orientation", s.orientation,
                            when {
                                m.glareRisk == true -> "glare risk"
                                m.azimuthDeg != null && m.lightDirectionDeg != null &&
                                    SunPosition.angularDiff(m.azimuthDeg!!,
                                        m.lightDirectionDeg!!) < 45f -> "facing light"
                                s.orientation != null -> "ok"
                                else -> "—"
                            })
                        MetricBar("Cellular", s.cellular, m.cellularDbm?.let { "$it dBm" } ?: "—")
                    }
                    ExtraReadings(m)
                    if (spot.notes.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = p.border)
                        Spacer(Modifier.height(10.dp))
                        spot.notes.forEach {
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp),
                                    tint = p.accentDim)
                                Spacer(Modifier.width(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = p.textDim)
                            }
                        }
                    }
                }
            }
        }
    }
}
