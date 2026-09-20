package com.spotkerja.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.settings.ScanPreset
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.RiseIn
import com.spotkerja.ui.components.SectionHeader
import com.spotkerja.ui.components.ShimmerBand
import com.spotkerja.ui.components.bouncyPress
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
    presets: List<ScanPreset> = emptyList(),
    onApplyPreset: (ScanPreset) -> Unit = {},
    bannerAd: (@Composable () -> Unit)? = null,
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
            RiseIn(0) {
            // Hero card — layered gradient, decorative rings, breathing icon.
            val breath = rememberInfiniteTransition(label = "hero")
            val iconPulse by breath.animateFloat(
                1f, 1.07f,
                infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse),
                label = "iconPulse")
            val drift by breath.animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(22000, easing = LinearEasing)),
                label = "ringDrift")

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, p.accent.copy(alpha = 0.22f))) {
                Box {
                    // base gradient
                    Box(Modifier.fillMaxWidth().matchParentSize()
                        .background(heroBrush()))
                    // decorative rotating radar rings, top-right
                    Canvas(
                        Modifier.align(Alignment.TopEnd).size(170.dp)
                    ) {
                        val c = Offset(size.width * 0.82f, size.height * 0.10f)
                        rotate(drift, c) {
                            listOf(0.5f, 0.75f, 1f).forEach { f ->
                                drawArc(
                                    p.accent.copy(alpha = 0.10f * (1.3f - f)),
                                    -90f, 300f, false,
                                    topLeft = Offset(c.x - 130 * f, c.y - 130 * f),
                                    size = Size(260 * f, 260 * f),
                                    style = Stroke(1.2.dp.toPx()),
                                )
                            }
                        }
                    }
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.size(54.dp * iconPulse)
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(p.accent.copy(alpha = 0.14f)),
                                )
                                Surface(
                                    Modifier.size(46.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    color = p.accent.copy(alpha = 0.20f),
                                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.5f)),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Radar, null, tint = p.accent,
                                            modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("SpotWise",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("find your perfect spot",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = p.accent)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Find the best spot to work\nin any room.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Measure Wi-Fi, ping, light, noise & facing direction " +
                                "at each spot — all processed on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textDim,
                        )
                        if (history.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val best = history.flatMap { it.spots }
                                    .maxByOrNull { it.totalScore }
                                HeroStat("${history.size}", "scans")
                                best?.let { HeroStat("%.0f".format(it.totalScore), "best score") }
                                HeroStat("${history.flatMap { it.spots }.distinctBy { it.label }.size}",
                                    "spots")
                            }
                        }
                    }
                }
            }
            }
        }

        if (presets.isNotEmpty()) {
            item {
                RiseIn(1) {
                SectionHeader("Scan presets")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        Surface(
                            onClick = { onApplyPreset(preset) },
                            shape = RoundedCornerShape(14.dp),
                            color = p.high,
                            border = BorderStroke(1.dp, p.border),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                                Text(preset.name, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelLarge, color = p.text,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${preset.durationSec}s • ${preset.spotCount} spots",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.textDim)
                            }
                        }
                    }
                }
                }
            }
        }

        item {
            RiseIn(2) {
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
        }

        item {
            RiseIn(3) {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                                .background(p.accentDim.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Timer, null, Modifier.size(16.dp),
                                tint = p.accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Scan duration per spot",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = p.accent.copy(alpha = 0.14f),
                        ) {
                            Text("${durationSec}s",
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold, color = p.accent)
                        }
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
                        Text("15s quick", style = MaterialTheme.typography.labelSmall,
                            color = p.textDim)
                        Text("60s thorough", style = MaterialTheme.typography.labelSmall,
                            color = p.textDim)
                    }
                }
            }
            }
        }

        item {
            RiseIn(4) {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                                .background(p.accentDim.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Place, null, Modifier.size(16.dp),
                                tint = p.accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Spots to compare",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        // Pill stepper
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(p.bg.copy(alpha = 0.5f))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StepperBtn(Icons.Default.Remove, enabled = spotCount > 2) {
                                onSpotCountChange(spotCount - 1)
                            }
                            Text("$spotCount",
                                Modifier.padding(horizontal = 14.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold, color = p.accent)
                            StepperBtn(Icons.Default.Add, enabled = spotCount < 8) {
                                onSpotCountChange(spotCount + 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Tap a spot to rename it (e.g. “By the window”)",
                        style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    Spacer(Modifier.height(10.dp))
                    // Wrapping spot chips dengan huruf avatar
                    FlowRowCompat {
                        spotNames.forEachIndexed { i, name ->
                            Surface(
                                color = p.bg.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp,
                                    p.accent.copy(alpha = 0.18f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp, bottom = 8.dp)
                                    .clickable { editingSpot = i },
                            ) {
                                Row(
                                    Modifier.padding(
                                        start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(p.accentDim.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(name.first().uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = p.accent)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = p.text, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Edit, "Rename", Modifier.size(11.dp),
                                        tint = p.textDim)
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        item {
            RiseIn(5) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fast Scan — glassy dark dengan border accent
                val fastSrc = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFastScan()
                    },
                    interactionSource = fastSrc,
                    modifier = Modifier.weight(1f).height(62.dp).bouncyPress(fastSrc),
                    shape = RoundedCornerShape(20.dp),
                    color = p.card,
                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.6f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                                .background(p.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Bolt, null, Modifier.size(19.dp),
                                tint = p.accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Fast Scan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = p.text)
                            Text("${fastDurationSec}s · 1 spot",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textDim)
                        }
                    }
                }
                // Start Scan — CTA gradient utama dengan shimmer sweep
                val startSrc = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartScan()
                    },
                    interactionSource = startSrc,
                    modifier = Modifier.weight(1.2f).height(62.dp).bouncyPress(startSrc),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(p.accent, p.accent2))),
                    ) {
                    ShimmerBand(Modifier.matchParentSize())
                    Row(
                        Modifier.fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(24.dp),
                            tint = p.bg)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Start Scan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold, color = p.bg)
                            Text("$spotCount spots · ${durationSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.bg.copy(alpha = 0.75f))
                        }
                    }
                    }
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
            item { RiseIn(6) { SectionHeader("Recent scans") } }
            items(history.take(3)) { s ->
                Box(Modifier.animateItem()) {
                    HistoryRow(s, onClick = { onOpenSession(s) })
                }
            }
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

        bannerAd?.let { item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() } } }
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
    val haptics = LocalHapticFeedback.current
    val selAnim by animateFloatAsState(
        if (selected) 1f else 0f, tween(280), label = "sel")
    val src = remember { MutableInteractionSource() }
    Card(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = src,
        modifier = modifier.scale(1f + selAnim * 0.02f).bouncyPress(src),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            (1f + selAnim).dp,
            p.accent.copy(alpha = 0.25f + selAnim * 0.55f)),
    ) {
        Column(
            Modifier.background(
                Brush.verticalGradient(
                    if (selected)
                        listOf(p.accent.copy(alpha = 0.16f), p.card)
                    else
                        listOf(p.high.copy(alpha = 0.4f), p.card)))
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (selected) p.accent.copy(alpha = 0.22f)
                            else p.accentDim.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(modeIcon(m), null, Modifier.size(18.dp),
                        tint = if (selected) p.accent else p.textDim)
                }
                if (selected) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp),
                        tint = p.accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(m.label, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
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

/** Circular icon button inside the spot-count pill stepper. */
@Composable
private fun StepperBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, enabled = enabled,
        interactionSource = src,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (enabled) p.accent.copy(alpha = 0.16f) else Color.Transparent,
        modifier = Modifier.bouncyPress(src),
    ) {
        Icon(icon, null, Modifier.padding(7.dp).size(16.dp),
            tint = if (enabled) p.accent else p.textDim.copy(alpha = 0.4f))
    }
}

/** Small stat pill inside the hero card. */
@Composable
private fun HeroStat(value: String, label: String) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = p.bg.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, p.accent.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = p.textDim)
        }
    }
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
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
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
