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
import androidx.compose.ui.graphics.Color
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

/** Judul grup kecil: icon + label caps. */
@Composable
private fun DeepGroupTitle(icon: androidx.compose.ui.graphics.vector.ImageVector,
                           title: String, trailing: (@Composable () -> Unit)? = null) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = p.accent)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = p.textDim,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) { Spacer(Modifier.weight(1f)); trailing() }
    }
}

/** Tile stat kecil — angka besar + label. */
@Composable
private fun RowScope.SmallTile(value: String, label: String) {
    val p = LocalPalette.current
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        color = p.high, border = BorderStroke(1.dp, p.border),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold, color = p.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Pill status berwarna (good/mid/bad). */
@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Track bar tipis untuk gauge/persen. */
@Composable
private fun GaugeBar(frac: Float, color: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier.fillMaxWidth().height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(p.border.copy(alpha = 0.4f)),
    ) {
        Box(
            Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(3.dp)).background(color),
        )
    }
}

/** Pembacaan ekstra deep metrics — grup visual, bukan daftar teks mentah. */
@Composable
private fun ExtraReadings(m: com.spotkerja.data.SpotMetrics) {
    val p = LocalPalette.current

    val hasNet = m.internetState != null || m.dnsMs != null || m.tcpMs != null ||
        m.tlsMs != null || m.udpState != null
    val hasAudio = m.speechPct != null || m.soundLabels.isNotEmpty()
    val hasLight = m.lightMap.size == 12 && m.camLumaAvg != null
    val hasThermal = m.batteryTempC != null || m.thermalStatus != null ||
        m.thermalHeadroom != null
    val envTiles = listOfNotNull(
        m.ambientTempC?.let { "%.1f°" to "Ambient" },
        m.humidityPct?.let { "%.0f%%" to "Humidity" },
        m.pressureHpa?.let { "%.0f" to "hPa" },
        m.altitudeM?.let { "${it.toInt()}m" to "Altitude" },
        m.stepsDuringScan?.let { "$it" to "Steps" },
    )
    val hasMag = m.magneticUt != null || m.magneticStdDevUt != null
    val hasRadio = m.wifiSsid != null || m.channelWidthMhz != null ||
        m.wifiBand != null || m.rxLinkSpeedMbps != null || m.txLinkSpeedMbps != null ||
        m.meshApCount != null || m.roamCount != null ||
        (m.rssiMinDbm != null && m.rssiMaxDbm != null && m.rssiMinDbm != m.rssiMaxDbm)
    val hasRoute = m.routeHops.isNotEmpty()

    if (!hasNet && !hasAudio && !hasLight && !hasThermal && envTiles.isEmpty() &&
        !hasMag && !hasRadio && !hasRoute && m.sensorsFound.isEmpty()) return

    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = p.border)
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ---- Network diagnostics ---------------------------------------------
        if (hasNet) {
            Column {
                DeepGroupTitle(Icons.Default.NetworkPing, "NETWORK") {
                    m.internetState?.let { st ->
                        val (txt, col) = when (st) {
                            "ok" -> "online" to p.good
                            "captive" -> "captive portal" to p.bad
                            "limited" -> "unverified" to p.gold
                            else -> "offline" to p.textDim
                        }
                        StatusPill(txt, col)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val tiles = listOfNotNull(
                    m.dnsMs?.let { "${it.toInt()}ms" to "DNS" },
                    m.tcpMs?.let { "${it.toInt()}ms" to "TCP${m.tcpPort?.let { pt -> ":$pt" } ?: ""}" },
                    m.tlsMs?.let { "${it.toInt()}ms" to "TLS" },
                    m.udpState?.let { it to "QUIC" },
                )
                if (tiles.isNotEmpty()) {
                    tiles.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (v, l) -> SmallTile(v, l) }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // ---- Soundscape --------------------------------------------------------
        if (hasAudio) {
            Column {
                DeepGroupTitle(Icons.Default.Mic, "SOUNDSCAPE") {
                    m.speechPct?.let { sp ->
                        val (txt, col) = when {
                            sp < 15f -> "quiet" to p.good
                            sp < 35f -> "noticeable" to p.gold
                            else -> "distracting" to p.bad
                        }
                        StatusPill(txt, col)
                    }
                }
                m.speechPct?.let { sp ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Speech", Modifier.width(52.dp),
                            style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        GaugeBar(sp / 100f, when {
                            sp < 15f -> p.good; sp < 35f -> p.gold; else -> p.bad
                        }, Modifier.weight(1f))
                        Text("${sp.toInt()}%", Modifier.padding(start = 8.dp).width(30.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = p.text,
                            textAlign = TextAlign.End)
                    }
                }
                if (m.soundLabels.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        m.soundLabels.take(3).forEach {
                            StatusPill(it, p.accent2)
                        }
                    }
                }
            }
        }

        // ---- Camera light map --------------------------------------------------
        if (hasLight) {
            Column {
                DeepGroupTitle(Icons.Default.Lightbulb, "LIGHT MAP") {
                    m.camHotspot?.let { StatusPill("hotspot $it", p.gold) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LightMapGrid(m.lightMap)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallTile("${m.camLumaAvg!!.toInt()}", "Avg /255")
                            m.camLumaStd?.let { SmallTile("±${it.toInt()}", "Uneven") }
                        }
                    }
                }
            }
        }

        // ---- Thermal ------------------------------------------------------------
        if (hasThermal) {
            Column {
                DeepGroupTitle(Icons.Default.Thermostat, "THERMAL") {
                    m.thermalStatus?.let { st ->
                        val label = listOf("normal", "light", "moderate", "severe",
                            "critical", "emergency", "shutdown").getOrElse(st) { "level $st" }
                        StatusPill(label, when {
                            st <= 1 -> p.good; st == 2 -> p.gold; else -> p.bad
                        })
                    }
                }
                Spacer(Modifier.height(8.dp))
                m.batteryTempC?.let { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Battery", Modifier.width(52.dp),
                            style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        GaugeBar(((t - 25f) / 20f), when {
                            t < 35f -> p.good; t < 40f -> p.gold; else -> p.bad
                        }, Modifier.weight(1f))
                        Text("%.0f°C".format(t),
                            Modifier.padding(start = 8.dp).width(38.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = p.text,
                            textAlign = TextAlign.End)
                    }
                }
                m.thermalHeadroom?.let { h ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Headroom", Modifier.width(52.dp),
                            style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        GaugeBar(h, when {
                            h > 0.5f -> p.good; h > 0.25f -> p.gold; else -> p.bad
                        }, Modifier.weight(1f))
                        Text("%.0f%%".format(h * 100f),
                            Modifier.padding(start = 8.dp).width(38.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = p.text,
                            textAlign = TextAlign.End)
                    }
                }
            }
        }

        // ---- Magnetic + environment ---------------------------------------------
        if (hasMag || envTiles.isNotEmpty()) {
            Column {
                DeepGroupTitle(Icons.Default.Explore, "ENVIRONMENT")
                Spacer(Modifier.height(8.dp))
                val tiles = (envTiles + listOfNotNull(
                    m.magneticUt?.let { "%.0fµT".format(it) to "Magnetic" },
                    m.magneticStdDevUt?.let {
                        "±%.1f".format(it) to if (it > 12f) "Unstable" else "Stable"
                    },
                )).chunked(4)
                tiles.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (v, l) -> SmallTile(v, l) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // ---- Radio / route (teknis, tetap baris ringkas) --------------------------
        if (hasRadio || hasRoute || m.sensorsFound.isNotEmpty()) {
            Column {
                DeepGroupTitle(Icons.Default.Wifi, "RADIO & ROUTE")
                Spacer(Modifier.height(4.dp))
                if (m.wifiSsid != null || m.channelWidthMhz != null || m.wifiBand != null) {
                    InfoRow("Radio", listOfNotNull(
                        m.wifiSsid, m.wifiBand,
                        m.channelWidthMhz?.let { "$it MHz" }).joinToString(" · "))
                }
                if (m.rxLinkSpeedMbps != null || m.txLinkSpeedMbps != null) {
                    InfoRow("Link", "↓${m.rxLinkSpeedMbps ?: "—"} / ↑${m.txLinkSpeedMbps ?: "—"} Mbps" +
                        (m.estThroughputMbps?.let { " · est. ≈${it.toInt()}" } ?: ""))
                }
                if (m.meshApCount != null || m.roamCount != null) {
                    InfoRow("Mesh", listOfNotNull(
                        m.meshApCount?.let { "${it + 1} APs" },
                        m.roamCount?.let { "roamed $it×" }).joinToString(" · "))
                }
                if (m.rssiMinDbm != null && m.rssiMaxDbm != null &&
                    m.rssiMinDbm != m.rssiMaxDbm) {
                    InfoRow("RSSI", "${m.rssiMinDbm}…${m.rssiMaxDbm} dBm")
                }
                if (hasRoute) {
                    InfoRow("Route", "${m.routeHops.size} hop(s) to ${m.routeTarget ?: "target"}")
                    m.routeHops.forEachIndexed { i, hop ->
                        Text("${i + 1}. $hop",
                            Modifier.padding(start = 14.dp, top = 1.dp),
                            style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    }
                }
                if (m.sensorsFound.isNotEmpty()) {
                    InfoRow("Sensors", "${m.sensorsFound.size} readable")
                    Text(m.sensorsFound.joinToString(" · "),
                        Modifier.padding(start = 14.dp, top = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim.copy(alpha = 0.7f))
                }
            }
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
