package com.spotkerja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.SectionHeader
import com.spotkerja.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun modeIcon(mode: WorkMode): ImageVector = when (mode) {
    WorkMode.WORK -> Icons.Default.Work
    WorkMode.STUDY -> Icons.AutoMirrored.Filled.MenuBook
    WorkMode.GAMING -> Icons.Default.SportsEsports
    WorkMode.VIDEO_CALL -> Icons.Default.Videocam
}

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
    val haptics = LocalHapticFeedback.current
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        item {
            // Hero card gradient
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(Modifier.background(HeroGradient).padding(22.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = AccentTeal.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.45f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Radar, null, tint = AccentTeal,
                                        modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Spotkerja", style = MaterialTheme.typography.headlineSmall)
                                Text("sensor-based spot finder",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Temukan posisi meja kerja terbaik.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Ukur Wi-Fi, ping, cahaya, noise & orientasi di tiap sudut ruangan — " +
                                "semua diproses lokal di HP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Mode penilaian")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkMode.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { m -> ModeCard(m, m == mode, Modifier.weight(1f)) { onModeChange(m) } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, Modifier.size(16.dp), tint = AccentTealDim)
                        Spacer(Modifier.width(8.dp))
                        Text("Durasi per spot", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text("${durationSec} detik", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = AccentTeal)
                    }
                    Slider(
                        value = durationSec.toFloat(),
                        onValueChange = { onDurationChange(it.toInt()) },
                        valueRange = 30f..60f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal,
                            inactiveTrackColor = TrackColor,
                        ),
                    )
                }
            }
        }

        item {
            SectionHeader(
                "Spot yang di-scan",
                trailing = {
                    if (spotLabels.size < 6) {
                        FilledTonalIconButton(onClick = onAddSpot, Modifier.size(30.dp)) {
                            Icon(Icons.Default.Add, "Tambah spot", Modifier.size(16.dp))
                        }
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                spotLabels.forEachIndexed { i, label ->
                    Surface(
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                Modifier.padding(start = 14.dp, top = 9.dp, bottom = 9.dp,
                                    end = if (spotLabels.size > 2) 2.dp else 14.dp),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentTeal,
                            )
                            if (spotLabels.size > 2) {
                                IconButton(onClick = { onRemoveSpot(i) }, Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, "Hapus", Modifier.size(14.dp),
                                        tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartScan()
                },
                Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal,
                    contentColor = BgDeep),
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mulai Scan", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Izin lokasi & mikrofon diminta saat mulai. Data diproses 100% lokal.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (history.isNotEmpty()) {
            item {
                SectionHeader(
                    "Riwayat terakhir",
                    trailing = {
                        TextButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, null, Modifier.size(15.dp),
                                tint = AccentTeal)
                            Spacer(Modifier.width(4.dp))
                            Text("Semua", color = AccentTeal)
                        }
                    },
                )
            }
            items(history.take(3)) { s -> HistoryRow(s, onClick = { onOpenSession(s) }) }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Info, null, Modifier.size(12.dp),
                    tint = TextSecondary.copy(alpha = 0.6f))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Estimasi praktis berbasis sensor HP — bukan pengukuran ilmiah.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ModeCard(m: WorkMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF16463E) else SurfaceCard),
        border = BorderStroke(
            1.dp,
            if (selected) AccentTeal.copy(alpha = 0.7f) else SurfaceBorder),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(modeIcon(m), null, Modifier.size(22.dp),
                tint = if (selected) AccentTeal else TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(m.label, style = MaterialTheme.typography.titleSmall,
                color = if (selected) TextPrimary else MaterialTheme.colorScheme.onSurface)
            Text(
                modeTagline(m),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2,
            )
        }
    }
}

private fun modeTagline(m: WorkMode) = when (m) {
    WorkMode.WORK -> "Seimbang: cahaya + noise"
    WorkMode.STUDY -> "Fokus: cahaya & hening"
    WorkMode.GAMING -> "Latency paling penting"
    WorkMode.VIDEO_CALL -> "Realtime + pencahayaan"
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
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(38.dp),
                shape = RoundedCornerShape(11.dp),
                color = AccentTealDim.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(modeIcon(WorkMode.entries.firstOrNull { it.label == session.mode }
                        ?: WorkMode.WORK), null, Modifier.size(18.dp), tint = AccentTeal)
                }
            }
            Spacer(Modifier.width(12.dp))
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
                    fontWeight = FontWeight.Bold, color = scoreColor(it.totalScore))
            }
            trailing?.invoke()
        }
    }
}
