package com.spotkerja.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SpotResult
import com.spotkerja.data.WorkMode
import com.spotkerja.ui.components.CompareChart
import com.spotkerja.ui.components.MetricBar
import com.spotkerja.ui.components.ScoreRing
import com.spotkerja.ui.theme.scoreColor

@Composable
fun ResultsScreen(
    spots: List<SpotResult>,
    mode: WorkMode,
    onShare: () -> Unit,
    onNewScan: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val sorted = spots.sortedByDescending { it.totalScore }
    val best = sorted.firstOrNull()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
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
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null,
                                    tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Best Spot", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text("Spot ${b.label}", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold)
                            if (ScoreEngine.isCloseCall(spots)) {
                                Text("Hampir imbang dengan spot lain",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        ScoreRing(b.totalScore, "dari 100", sizeDp = 96)
                    }
                }
            }
        }

        item {
            Text("Perbandingan", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            CompareChart(sorted.map { "Spot ${it.label}" to it.totalScore })
        }

        item {
            Text("Detail per spot", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        }
        items(sorted) { spot -> SpotDetailCard(spot) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onShare, Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Share / Export")
                }
                Button(onClick = onNewScan, Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Scan Baru")
                }
            }
            Text(
                "Work Spot Score = gabungan berbobot Wi-Fi, ping/jitter/loss, cahaya, noise, " +
                    "orientasi, dan sinyal seluler — bobot menyesuaikan mode ${mode.label}. " +
                    "Estimasi praktis, bukan pengukuran ilmiah.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SpotDetailCard(spot: SpotResult) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Spot ${spot.label}", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("${spot.durationSec} dtk", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("%.0f".format(spot.totalScore), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = scoreColor(spot.totalScore))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Detail",
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
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
                    Spacer(Modifier.height(12.dp))
                    spot.notes.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
