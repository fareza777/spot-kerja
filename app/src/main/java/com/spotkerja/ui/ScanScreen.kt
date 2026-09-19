package com.spotkerja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotkerja.data.SpotResult
import com.spotkerja.sense.ScanProgress
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.LiveTile
import com.spotkerja.ui.components.RadarSweep
import com.spotkerja.ui.theme.*

@Composable
fun ScanScreen(
    spotNames: List<String>,
    currentSpotIndex: Int,
    spotsDone: List<SpotResult>,
    progress: ScanProgress,
    hasLightSensor: Boolean,
    onStopEarly: () -> Unit,
    onScanNext: () -> Unit,
    onRescan: () -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    val currentLabel = spotNames.getOrNull(currentSpotIndex)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SpotStepper(spotNames.size, spotsDone.size, currentSpotIndex) }

        if (progress.running && currentLabel != null) {
            item {
                Text("Scanning $currentLabel", style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Place the phone at the spot and keep it still.",
                    style = MaterialTheme.typography.bodySmall, color = p.textDim,
                )
            }
            item {
                RadarSweep(progressFrac = progress.elapsedSec.toFloat() / progress.totalSec)
                Text(
                    "${progress.totalSec - progress.elapsedSec}s remaining",
                    style = MaterialTheme.typography.labelMedium, color = p.textDim,
                )
            }
            item {
                val l = progress.live
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Wi-Fi", l.wifiRssiDbm?.let { "$it" } ?: "—",
                            l.wifiBand?.let { "dBm • $it" } ?: "dBm", Icons.Default.Wifi,
                            Modifier.weight(1f))
                        LiveTile("Ping", l.pingMs?.let { "≈${it.toInt()}" } ?: "—", "ms • router",
                            Icons.Default.Speed, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Light", l.lux?.let { "≈${it.toInt()}" } ?: "—",
                            if (!hasLightSensor) "lux • sensor n/a" else "lux",
                            Icons.Default.LightMode, Modifier.weight(1f))
                        LiveTile("Noise", l.noiseDb?.let { "≈${it.toInt()}" } ?: "—", "dB est.",
                            Icons.Default.Mic, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Facing", l.azimuthDeg?.let { "${it.toInt()}°" } ?: "—",
                            "azimuth", Icons.Default.Explore, Modifier.weight(1f))
                        LiveTile("Cellular", l.cellularDbm?.let { "$it" } ?: "—", "dBm",
                            Icons.Default.SignalCellularAlt, Modifier.weight(1f))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStopEarly()
                    },
                    Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, p.gold.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Default.Stop, null, tint = p.gold)
                    Spacer(Modifier.width(8.dp))
                    Text("Finish early", color = p.gold)
                }
                TextButton(onClick = onCancel) { Text("Cancel scan", color = p.textDim) }
            }
        } else if (currentLabel != null) {
            item {
                spotsDone.lastOrNull()?.let { last ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = sc(last.totalScore).copy(alpha = 0.15f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = sc(last.totalScore))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${last.label} done", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Score ${"%.0f".format(last.totalScore)}/100",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.textDim)
                            }
                            Text("%.0f".format(last.totalScore),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = sc(last.totalScore))
                        }
                    }
                }
            }
            item {
                Surface(
                    Modifier.size(76.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = p.accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.4f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${currentSpotIndex + 1}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold, color = p.accent)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Move to $currentLabel",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Place the phone at the next spot, then start scanning.",
                    style = MaterialTheme.typography.bodySmall, color = p.textDim,
                )
            }
            item {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScanNext()
                    },
                    Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.accent, contentColor = p.bg),
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan $currentLabel", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    TextButton(onClick = onRescan) { Text("Redo last spot", color = p.textDim) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel", color = p.textDim) }
                }
            }
        }
    }
}

@Composable
private fun SpotStepper(count: Int, doneCount: Int, activeIndex: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val done = i < doneCount
            val active = i == activeIndex
            Surface(
                color = when {
                    done -> p.accent
                    active -> p.accent.copy(alpha = 0.15f)
                    else -> p.card
                },
                border = BorderStroke(1.dp, when {
                    done -> p.accent
                    active -> p.accent.copy(alpha = 0.6f)
                    else -> p.border
                }),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    if (done) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = p.bg)
                    } else {
                        Text("${i + 1}", fontWeight = FontWeight.Bold,
                            color = if (active) p.accent else p.textDim)
                    }
                }
            }
        }
    }
}
