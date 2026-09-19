package com.spotkerja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.SectionHeader
import com.spotkerja.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun modeIcon(mode: WorkMode): ImageVector = when (mode) {
    WorkMode.WORK -> Icons.Default.Work
    WorkMode.STUDY -> Icons.AutoMirrored.Filled.MenuBook
    WorkMode.GAMING -> Icons.Default.SportsEsports
    WorkMode.VIDEO_CALL -> Icons.Default.Videocam
}

@Composable
fun HomeScreen(
    mode: WorkMode,
    durationSec: Int,
    spotCount: Int,
    spotNames: List<String>,
    history: List<ScanSession>,
    onModeChange: (WorkMode) -> Unit,
    onDurationChange: (Int) -> Unit,
    onSpotCountChange: (Int) -> Unit,
    onSpotNameChange: (Int, String) -> Unit,
    onStartScan: () -> Unit,
    onFastScan: () -> Unit,
    fastDurationSec: Int,
    onOpenSession: (ScanSession) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val p = LocalPalette.current
    var editingSpot by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp),
    ) {
        item {
            // Hero card — balanced headline, not top-cramped
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Box(Modifier.background(heroBrush()).padding(22.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = p.accent.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, p.accent.copy(alpha = 0.45f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Radar, null, tint = p.accent,
                                        modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("SpotWise", style = MaterialTheme.typography.headlineSmall)
                                Text("find your perfect spot",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = p.textDim)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Find the best spot to work in any room.",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Measure Wi-Fi, ping, light, noise & facing direction at each spot — " +
                                "all processed on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textDim,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Evaluation mode")
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
                        Icon(Icons.Default.Timer, null, Modifier.size(16.dp), tint = p.accentDim)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan duration per spot", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text("${durationSec}s", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = p.accent)
                    }
                    Slider(
                        value = durationSec.toFloat(),
                        onValueChange = { onDurationChange(it.toInt()) },
                        valueRange = 15f..60f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = p.accent,
                            activeTrackColor = p.accent,
                            inactiveTrackColor = p.border,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("15s", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        Text("60s", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, Modifier.size(16.dp), tint = p.accentDim)
                        Spacer(Modifier.width(8.dp))
                        Text("Spots to compare", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        FilledTonalIconButton(
                            onClick = { onSpotCountChange(spotCount - 1) },
                            Modifier.size(30.dp), enabled = spotCount > 2,
                        ) { Icon(Icons.Default.Remove, "Less", Modifier.size(16.dp)) }
                        Text("$spotCount", Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = p.accent)
                        FilledTonalIconButton(
                            onClick = { onSpotCountChange(spotCount + 1) },
                            Modifier.size(30.dp), enabled = spotCount < 8,
                        ) { Icon(Icons.Default.Add, "More", Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Tap a spot to rename it (e.g. “By the window”)",
                        style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    Spacer(Modifier.height(10.dp))
                    // Wrapping spot chips
                    FlowRowCompat {
                        spotNames.forEachIndexed { i, name ->
                            Surface(
                                color = if (i % 2 == 0) p.high else p.card,
                                border = BorderStroke(1.dp, p.border),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp, bottom = 8.dp)
                                    .clickable { editingSpot = i },
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = p.text)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Edit, "Rename", Modifier.size(12.dp),
                                        tint = p.textDim)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFastScan()
                    },
                    Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, p.accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent),
                ) {
                    Icon(Icons.Default.Bolt, null, Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Fast Scan", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("${fastDurationSec}s · 1 spot",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textDim)
                    }
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartScan()
                    },
                    Modifier.weight(1.2f).height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.accent, contentColor = p.bg),
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Start Scan", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold)
                        Text("$spotCount spots · ${durationSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.bg.copy(alpha = 0.7f))
                    }
                }
            }
            Text(
                "Location & mic permissions are requested on start. Everything runs on-device.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textDim.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (history.isNotEmpty()) {
            item { SectionHeader("Recent scans") }
            items(history.take(3)) { s -> HistoryRow(s, onClick = { onOpenSession(s) }) }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Info, null, Modifier.size(12.dp),
                    tint = p.textDim.copy(alpha = 0.6f))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Practical estimates from phone sensors — not scientific measurements.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.textDim.copy(alpha = 0.6f),
                )
            }
        }
    }

    editingSpot?.let { idx ->
        RenameSpotDialog(
            current = spotNames.getOrNull(idx) ?: "",
            onDismiss = { editingSpot = null },
            onSave = { name -> onSpotNameChange(idx, name); editingSpot = null },
        )
    }
}

/** Wrapping row without experimental FlowLayout API. */
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var cur = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var curW = 0
        placeables.forEach { pl ->
            if (curW + pl.width > constraints.maxWidth && cur.isNotEmpty()) {
                rows += cur; cur = mutableListOf(); curW = 0
            }
            cur += pl; curW += pl.width
        }
        if (cur.isNotEmpty()) rows += cur
        val h = rows.maxOfOrNull { r -> r.maxOf { it.height } } ?: 0
        val totalH = rows.sumOf { r -> r.maxOf { it.height } }
        layout(constraints.maxWidth, totalH.coerceAtLeast(h)) {
            var y = 0
            rows.forEach { r ->
                var x = 0
                r.forEach { pl -> pl.place(x, y); x += pl.width }
                y += r.maxOf { it.height }
            }
        }
    }
}

@Composable
private fun RenameSpotDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this spot") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(24) },
                singleLine = true,
                placeholder = { Text("e.g. By the window") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ModeCard(m: WorkMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) p.accentDim.copy(alpha = 0.5f) else p.card),
        border = BorderStroke(1.dp,
            if (selected) p.accent.copy(alpha = 0.7f) else p.border),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(modeIcon(m), null, Modifier.size(22.dp),
                tint = if (selected) p.accent else p.textDim)
            Spacer(Modifier.height(10.dp))
            Text(m.label, style = MaterialTheme.typography.titleSmall,
                color = if (selected) p.text else MaterialTheme.colorScheme.onSurface)
            Text(modeTagline(m), style = MaterialTheme.typography.labelSmall,
                color = p.textDim, maxLines = 2)
        }
    }
}

private fun modeTagline(m: WorkMode) = when (m) {
    WorkMode.WORK -> "Balanced light + quiet"
    WorkMode.STUDY -> "Light & silence first"
    WorkMode.GAMING -> "Latency is king"
    WorkMode.VIDEO_CALL -> "Realtime + face light"
}

@Composable
fun HistoryRow(
    session: ScanSession,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    val sc = LocalScoreColor.current
    Card(
        onClick = onClick,
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = p.card),
        border = BorderStroke(1.dp, p.border),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(38.dp),
                shape = RoundedCornerShape(11.dp),
                color = p.accentDim.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(modeIcon(WorkMode.entries.firstOrNull { it.label == session.mode }
                        ?: WorkMode.WORK), null, Modifier.size(18.dp), tint = p.accent)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.mode, fontWeight = FontWeight.SemiBold)
                Text(
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(Date(session.createdAtEpochMs)) +
                        " • ${session.spots.size} spots" +
                        (session.bestSpotLabel?.let { " • best: $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
            }
            session.spots.maxByOrNull { it.totalScore }?.let {
                Text("%.0f".format(it.totalScore), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = sc(it.totalScore))
            }
            trailing?.invoke()
        }
    }
}
