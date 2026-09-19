package com.spotkerja.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    mode: WorkMode,
    durationSec: Int,
    spotLabels: List<String>,
    history: List<ScanSession>,
    onModeChange: (WorkMode) -> Unit,
    onDurationChange: (Int) -> Unit,
    onAddSpot: () -> Unit,
    onRemoveSpot: (Int) -> Unit,
    onStartScan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSession: (ScanSession) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text("Spotkerja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Temukan posisi meja kerja terbaik berdasarkan sensor HP — Wi-Fi, ping, cahaya, kebisingan, dan orientasi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text("Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkMode.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            val selected = m == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onModeChange(m) },
                                label = { Text(m.label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text(mode.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Text("Durasi per spot: ${durationSec} dtk", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Slider(
                value = durationSec.toFloat(),
                onValueChange = { onDurationChange(it.toInt()) },
                valueRange = 30f..60f,
                steps = 5,
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spot yang di-scan", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (spotLabels.size < 6) {
                    IconButton(onClick = onAddSpot) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah spot")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                spotLabels.forEachIndexed { i, label ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Spot $label", Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                                fontWeight = FontWeight.Medium)
                            if (spotLabels.size > 2) {
                                IconButton(onClick = { onRemoveSpot(i) }, Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Hapus", Modifier.size(16.dp))
                                }
                            } else Spacer(Modifier.width(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStartScan,
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mulai Scan", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Beri izin lokasi & mikrofon saat diminta agar semua metrik terukur. " +
                    "Semua data diproses lokal di HP — tanpa backend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (history.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Riwayat terakhir", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Semua")
                    }
                }
            }
            items(history.take(3)) { s -> HistoryRow(s, onClick = { onOpenSession(s) }) }
        }

        item {
            Text(
                "Estimasi praktis berbasis sensor HP — bukan pengukuran ilmiah.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun HistoryRow(
    session: ScanSession,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.mode, fontWeight = FontWeight.SemiBold)
                Text(
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(Date(session.createdAtEpochMs)) +
                        " • ${session.spots.size} spot" +
                        (session.bestSpotLabel?.let { " • best: $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            session.spots.maxByOrNull { it.totalScore }?.let {
                Text("%.0f".format(it.totalScore), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            trailing?.invoke()
        }
    }
}
