package com.spotkerja.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
) {
    val haptics = LocalHapticFeedback.current
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    val sorted = spots.sortedByDescending { it.totalScore }
    val best = sorted.firstOrNull()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        if (onBack != null) {
            item {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Back")
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
                                    Text(b.label, style = MaterialTheme.typography.headlineMedium,
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
                    CompareChart(sorted.map { it.label to it.totalScore })
                }
            }
        }

        // Radar overlay metrik per-spot (maks 3 teratas). Sumbu yang tidak
        // punya data sama sekali (semua spot null) tidak ditampilkan.
        val allAxes = listOf("Wi-Fi", "Ping", "Jitter", "Loss", "Light",
            "Noise", "Facing", "Cell")
        val allSeries = sorted.take(3).map { s ->
            s.label to listOf(s.scores.wifi, s.scores.ping, s.scores.jitter,
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
        itemsIndexed(sorted) { rank, spot -> SpotDetailCard(spot, rank) }

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
                Spacer(Modifier.weight(1f))
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

@Composable
private fun SpotDetailCard(spot: SpotResult, rank: Int) {
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    var expanded by remember { mutableStateOf(rank == 0) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(spot.label, fontWeight = FontWeight.Bold,
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
