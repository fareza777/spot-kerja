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
import androidx.compose.ui.unit.dp
import com.spotkerja.data.SpotResult
import com.spotkerja.sense.ScanProgress
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.LiveTile
import com.spotkerja.ui.components.RadarSweep
import com.spotkerja.ui.theme.*

@Composable
fun ScanScreen(
    spotLabels: List<String>,
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
    val currentLabel = spotLabels.getOrNull(currentSpotIndex)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SpotStepper(spotLabels, spotsDone.size, currentSpotIndex) }

        if (progress.running && currentLabel != null) {
            item {
                Text("Scanning Spot $currentLabel", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Letakkan HP di posisi kerja, jangan digerakkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            item {
                RadarSweep(progressFrac = progress.elapsedSec.toFloat() / progress.totalSec)
                Text(
                    "${progress.totalSec - progress.elapsedSec} detik tersisa",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
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
                        LiveTile("Cahaya", l.lux?.let { "≈${it.toInt()}" } ?: "—",
                            if (!hasLightSensor) "lux • sensor n/a" else "lux",
                            Icons.Default.LightMode, Modifier.weight(1f))
                        LiveTile("Noise", l.noiseDb?.let { "≈${it.toInt()}" } ?: "—", "dB est.",
                            Icons.Default.Mic, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Hadap", l.azimuthDeg?.let { "${it.toInt()}°" } ?: "—",
                            "azimuth", Icons.Default.Explore, Modifier.weight(1f))
                        LiveTile("Seluler", l.cellularDbm?.let { "$it" } ?: "—", "dBm",
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
                    border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Default.Stop, null, tint = AccentAmber)
                    Spacer(Modifier.width(8.dp))
                    Text("Selesaikan lebih awal", color = AccentAmber)
                }
                TextButton(onClick = onCancel) { Text("Batalkan scan", color = TextSecondary) }
            }
        } else if (currentLabel != null) {
            item {
                spotsDone.lastOrNull()?.let { last ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = scoreColor(last.totalScore).copy(alpha = 0.15f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = scoreColor(last.totalScore))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Spot ${last.label} selesai", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("Skor sementara ${"%.0f".format(last.totalScore)}/100",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary)
                            }
                            Text("%.0f".format(last.totalScore),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor(last.totalScore))
                        }
                    }
                }
            }
            item {
                Surface(
                    Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = AccentTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(currentLabel, style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold, color = AccentTeal)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Pindah ke Spot $currentLabel",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Posisikan HP di titik kerja berikutnya, lalu mulai scan.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
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
                        containerColor = AccentTeal, contentColor = BgDeep),
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Spot $currentLabel", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
                Row {
                    TextButton(onClick = onRescan) { Text("Ulangi spot terakhir", color = TextSecondary) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Batalkan", color = TextSecondary) }
                }
            }
        }
    }
}

@Composable
private fun SpotStepper(labels: List<String>, doneCount: Int, activeIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            val done = i < doneCount
            val active = i == activeIndex
            Surface(
                color = when {
                    done -> AccentTeal
                    active -> AccentTeal.copy(alpha = 0.15f)
                    else -> SurfaceCard
                },
                border = BorderStroke(1.dp, when {
                    done -> AccentTeal
                    active -> AccentTeal.copy(alpha = 0.6f)
                    else -> SurfaceBorder
                }),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    if (done) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                            tint = BgDeep)
                    } else {
                        Text(label, fontWeight = FontWeight.Bold,
                            color = if (active) AccentTeal else TextSecondary)
                    }
                }
            }
        }
    }
}
