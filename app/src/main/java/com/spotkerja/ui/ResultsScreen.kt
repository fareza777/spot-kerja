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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotResult
import com.spotkerja.data.WorkMode
import com.spotkerja.ui.components.*
import com.spotkerja.ui.theme.*

@Composable
fun ResultsScreen(
    spots: List<SpotResult>,
    mode: WorkMode,
    onShare: () -> Unit,
    onNewScan: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
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
                    Spacer(Modifier.width(4.dp)); Text("Kembali")
                }
            }
        }

        best?.let { b ->
            item {
                AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 4 }) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Box(Modifier.background(HeroGradient).padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentAmber.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp,
                                                AccentAmber.copy(alpha = 0.5f)),
                                        ) {
                                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.EmojiEvents, null,
                                                    Modifier.size(13.dp), tint = AccentAmber)
                                                Spacer(Modifier.width(4.dp))
                                                Text("BEST SPOT",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = AccentAmber)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Spot ${b.label}",
                                        style = MaterialTheme.typography.headlineMedium)
                                    if (ScoreEngine.isCloseCall(spots)) {
                                        Text("Hampir imbang dengan spot lain",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary)
                                    }
                                }
                                ScoreRing(b.totalScore, "dari 100", sizeDp = 100)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Perbandingan skor")
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    CompareChart(sorted.map { "Spot ${it.label}" to it.totalScore })
                }
            }
        }

        item { SectionHeader("Detail per spot") }
        itemsIndexed(sorted) { rank, spot -> SpotDetailCard(spot, rank) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceBorder),
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Share")
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNewScan()
                    },
                    Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal, contentColor = BgDeep),
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Baru", fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Work Spot Score = gabungan berbobot Wi-Fi, ping/jitter/loss, cahaya, noise, " +
                    "orientasi & seluler — bobot menyesuaikan mode ${mode.label}. " +
                    "Estimasi praktis, bukan pengukuran ilmiah.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SpotDetailCard(spot: SpotResult, rank: Int) {
    var expanded by remember { mutableStateOf(rank == 0) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Spot ${spot.label}", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("${spot.durationSec} dtk • ${spot.metrics.wifiSamples +
                        spot.metrics.pingSamples + spot.metrics.luxSamples +
                        spot.metrics.noiseSamples} sampel",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary)
                }
                Text("%.0f".format(spot.totalScore),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = scoreColor(spot.totalScore))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        "Detail", tint = TextSecondary,
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
                        MetricBar("Cahaya", s.light, m.luxAvg?.let { "≈${it.toInt()} lux" } ?: "—")
                        MetricBar("Noise", s.noise, m.noiseDbAvg?.let { "≈${it.toInt()} dB" } ?: "—")
                        MetricBar("Orientasi", s.orientation,
                            when {
                                m.glareRisk == true -> "risiko silau"
                                m.sunAzimuthDeg != null -> "ok"
                                else -> "—"
                            })
                        MetricBar("Seluler", s.cellular, m.cellularDbm?.let { "$it dBm" } ?: "—")
                    }
                    if (spot.notes.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = SurfaceBorder)
                        Spacer(Modifier.height(10.dp))
                        spot.notes.forEach {
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp),
                                    tint = AccentTealDim)
                                Spacer(Modifier.width(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
