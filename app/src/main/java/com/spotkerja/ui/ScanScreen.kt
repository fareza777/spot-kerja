package com.spotkerja.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.data.SpotResult
import com.spotkerja.sense.ScanProgress
import com.spotkerja.ui.components.LiveTile
import com.spotkerja.ui.components.ScoreRing
import com.spotkerja.ui.theme.scoreColor

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
    val currentLabel = spotLabels.getOrNull(currentSpotIndex)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            // Indikator progress antar-spot
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                spotLabels.forEachIndexed { i, label ->
                    val done = i < spotsDone.size
                    val active = i == currentSpotIndex
                    Surface(
                        color = when {
                            done -> MaterialTheme.colorScheme.primary
                            active -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (done) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (progress.running && currentLabel != null) {
            item {
                Text("Scanning Spot $currentLabel…", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    "Letakkan HP di posisi kerja, jangan digerakkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ScoreRing(
                    score = progress.elapsedSec * 100f / progress.totalSec,
                    label = "${progress.totalSec - progress.elapsedSec} dtk",
                    sizeDp = 150,
                )
            }
            item {
                val l = progress.live
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Wi-Fi", l.wifiRssiDbm?.let { "$it dBm" } ?: "—",
                            l.wifiBand ?: "", Modifier.weight(1f))
                        LiveTile("Ping router", l.pingMs?.let { "≈${it.toInt()} ms" } ?: "—",
                            "", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Cahaya", l.lux?.let { "≈${it.toInt()} lx" } ?: "—",
                            if (!hasLightSensor) "sensor n/a" else "", Modifier.weight(1f))
                        LiveTile("Noise", l.noiseDb?.let { "≈${it.toInt()} dB" } ?: "—",
                            "", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveTile("Hadap", l.azimuthDeg?.let { "${it.toInt()}°" } ?: "—",
                            "azimuth", Modifier.weight(1f))
                        LiveTile("Seluler", l.cellularDbm?.let { "$it dBm" } ?: "—",
                            "", Modifier.weight(1f))
                    }
                }
            }
            item {
                OutlinedButton(onClick = onStopEarly, Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Selesaikan lebih awal")
                }
                TextButton(onClick = onCancel) { Text("Batalkan scan") }
            }
        } else if (currentLabel != null) {
            // Jeda antar-spot: user memindahkan HP ke spot berikutnya
            item {
                spotsDone.lastOrNull()?.let { last ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = scoreColor(last.totalScore))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Spot ${last.label} selesai", fontWeight = FontWeight.SemiBold)
                                Text("Skor sementara: ${"%.0f".format(last.totalScore)}/100",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                Text("Pindah ke Spot $currentLabel", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    "Posisikan HP di titik kerja berikutnya, lalu mulai scan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(onClick = onScanNext, Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Spot $currentLabel")
                }
                Row {
                    TextButton(onClick = onRescan) { Text("Ulangi spot terakhir") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Batalkan") }
                }
            }
        }
    }
}
