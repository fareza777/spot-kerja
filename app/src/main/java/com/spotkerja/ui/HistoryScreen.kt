package com.spotkerja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotkerja.data.Insights
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.Sparkline
import com.spotkerja.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    sessions: List<ScanSession>,
    onOpen: (ScanSession) -> Unit,
    onDelete: (String) -> Unit,
    bannerAd: (@Composable () -> Unit)? = null,
    focusMinutes: Map<String, Int> = emptyMap(),
) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var query by remember { mutableStateOf("") }
    var modeFilter by remember { mutableStateOf<WorkMode?>(null) }
    var roomFilter by remember { mutableStateOf<String?>(null) }

    val rooms = remember(sessions) { sessions.mapNotNull { it.room }.distinct() }

    // Set of days that have scans (for dot markers)
    val daysWithScans = remember(sessions, month) {
        sessions.map {
            Instant.ofEpochMilli(it.createdAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    val daySessions = remember(sessions, selectedDay, query, modeFilter, roomFilter) {
        var list = selectedDay?.let { d ->
            sessions.filter {
                Instant.ofEpochMilli(it.createdAtEpochMs)
                    .atZone(ZoneId.systemDefault()).toLocalDate() == d
            }
        } ?: sessions
        modeFilter?.let { mf -> list = list.filter { it.mode == mf.name } }
        roomFilter?.let { rf -> list = list.filter { it.room == rf } }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { s ->
                s.spots.any { it.label.lowercase().contains(q) }
            }
        }
        list
    }

    // Trend per nama spot: skor spot yang sama lintas sesi, urut waktu.
    val spotTrends = remember(sessions) {
        sessions.sortedBy { it.createdAtEpochMs }
            .flatMap { s -> s.spots.map { it.label to it.totalScore } }
            .groupBy({ it.first }, { it.second })
            .filter { it.value.size >= 2 }
            .toList().sortedByDescending { it.second.size }.take(4)
    }
    val deltas = remember(sessions) { Insights.deltas(sessions).associateBy { it.label } }
    val byHour = remember(sessions) { Insights.bestByHour(sessions) }
    val busy = remember(sessions) { Insights.busyHours(sessions) }
    val badgeList = remember(sessions, focusMinutes) { Insights.badges(sessions, focusMinutes) }
    val streak = remember(sessions) { Insights.liveStreak(sessions) }
    val feelMatch = remember(sessions) { Insights.feelMatch(sessions) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Scan history", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Tap a day to filter", style = MaterialTheme.typography.bodySmall,
                        color = p.textDim)
                }
                if (streak > 1) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = p.gold.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, p.gold.copy(alpha = 0.5f)),
                    ) {
                        Text("${streak}d streak",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = p.gold)
                    }
                }
            }
        }

        // Badges — pencapaian lokal, dihitung dari history saja.
        if (sessions.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Badges", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        badgeList.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { b ->
                                    Surface(
                                        Modifier.weight(1f).padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (b.unlocked) p.accent.copy(alpha = 0.10f)
                                            else p.high.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp,
                                            if (b.unlocked) p.accent.copy(alpha = 0.5f) else p.border),
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(b.name, style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (b.unlocked) p.accent else p.textDim,
                                                maxLines = 1)
                                            Text(b.desc, style = MaterialTheme.typography.labelSmall,
                                                color = p.textDim, maxLines = 2)
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        feelMatch?.let { (pct, n) ->
                            Spacer(Modifier.height(8.dp))
                            Text("Blind tests: your feel matched the data $pct% of $n runs",
                                style = MaterialTheme.typography.labelSmall, color = p.accent)
                        }
                    }
                }
            }
        }

        // Best spot by hour — heatmap spot × waktu dari seluruh history.
        if (byHour.size >= 2 && byHour.any { r -> r.second.count { it != null } >= 2 }) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Best spot by hour", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Average score per time of day", style = MaterialTheme.typography.labelSmall,
                            color = p.textDim)
                        Spacer(Modifier.height(10.dp))
                        val shorts = listOf("AM", "Noon", "PM", "Eve", "Night")
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.width(92.dp))
                            shorts.forEach {
                                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall, color = p.textDim)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        byHour.forEach { (label, vals) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.width(92.dp), maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.text,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                vals.forEach { v ->
                                    Box(
                                        Modifier.weight(1f).padding(horizontal = 2.dp)
                                            .height(26.dp).clip(RoundedCornerShape(7.dp))
                                            .background(
                                                if (v == null) p.high.copy(alpha = 0.35f)
                                                else LocalScoreColor.current(v)
                                                    .copy(alpha = 0.25f + 0.55f * v / 100f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (v != null) Text("${v.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = p.text)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Congestion forecast — jam-jam jaringan ramai (avg ping per waktu).
        if (busy.any { it.samples > 0 }) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Network busiest hours", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Average ping per time of day", style = MaterialTheme.typography.labelSmall,
                            color = p.textDim)
                        Spacer(Modifier.height(12.dp))
                        val maxPing = busy.mapNotNull { it.pingMs }.maxOrNull() ?: 1f
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            busy.forEach { b ->
                                Column(Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(b.pingMs?.let { "${it.toInt()}" } ?: "·",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim)
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                            .height(((b.pingMs ?: 0f) / maxPing * 70f).coerceAtLeast(4f).dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (b.samples == 0) p.high.copy(alpha = 0.4f)
                                                else p.accent.copy(alpha = 0.55f)),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(listOf("AM", "Noon", "PM", "Eve", "Night")[
                                        busy.indexOf(b)],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search spot names…", color = p.textDim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = p.textDim) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { query = "" }) { Text("Clear") }
                    }
                },
                singleLine = true, shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = modeFilter == null, onClick = { modeFilter = null },
                    label = { Text("All") },
                )
                WorkMode.entries.forEach { m ->
                    FilterChip(
                        selected = modeFilter == m, onClick = {
                            modeFilter = if (modeFilter == m) null else m },
                        label = { Text(m.label, maxLines = 1) },
                    )
                }
            }
            if (rooms.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, null, Modifier.size(14.dp),
                        tint = p.textDim)
                    rooms.forEach { r ->
                        FilterChip(
                            selected = roomFilter == r, onClick = {
                                roomFilter = if (roomFilter == r) null else r },
                            label = { Text(r, maxLines = 1) },
                        )
                    }
                }
            }
        }

        if (spotTrends.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Spot trends", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Same spot name, over time", color = p.textDim,
                            style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(12.dp))
                        spotTrends.forEach { (label, scores) ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(Modifier.width(110.dp)) {
                                    Text(label, maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${scores.size}× • best %.0f".format(scores.max()) +
                                            (deltas[label]?.let { " • ${it.changeText()}" } ?: "") +
                                            (focusMinutes[label]?.let { " • ${it}m focus" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                                Sparkline(scores, Modifier.weight(1f))
                                Text("%.0f".format(scores.last()),
                                    Modifier.width(34.dp), textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalScoreColor.current(scores.last()))
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    // Month nav
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev",
                                tint = p.text)
                        }
                        Text(
                            "${month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())} ${month.year}",
                            Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next",
                                tint = p.text)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Day-of-week header
                    Row(Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    CalendarGrid(month, daysWithScans, selectedDay) { selectedDay = it }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedDay?.let { "Scans on ${it.dayOfMonth} ${it.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}" }
                        ?: "All scans",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (selectedDay != null) {
                    TextButton(onClick = { selectedDay = null }) { Text("Show all") }
                }
            }
        }

        if (daySessions.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.History, null,
                        Modifier.size(44.dp), tint = p.textDim)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            selectedDay == null && sessions.isEmpty() ->
                                "No scans yet — run your first scan."
                            query.isNotBlank() || modeFilter != null ->
                                "Nothing matches this filter."
                            else -> "No scans this day."
                        },
                        color = p.textDim, textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(daySessions, key = { it.id }) { s ->
                HistoryRow(s, onClick = { onOpen(s) }, trailing = {
                    IconButton(onClick = { onDelete(s.id) }) {
                        Icon(Icons.Default.Delete, "Delete", tint = p.textDim)
                    }
                })
            }
        }

        bannerAd?.let { item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() } } }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    daysWithScans: Set<LocalDate>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - 1) // Monday-based
    val days = month.lengthOfMonth()
    val cells = (offset + days + 6) / 7 * 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until cells / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val day = idx - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val has = daysWithScans.contains(date)
                            val sel = date == selected
                            val isToday = date == today
                            Box(
                                Modifier.size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onSelect(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (sel) {
                                    Surface(Modifier.fillMaxSize(), shape = CircleShape,
                                        color = p.accent.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, p.accent)) {}
                                } else if (isToday) {
                                    Surface(Modifier.fillMaxSize(), shape = CircleShape,
                                        color = Color.Transparent,
                                        border = BorderStroke(1.dp, p.border)) {}
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$day",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (sel || isToday) FontWeight.Bold
                                            else FontWeight.Normal,
                                        color = if (sel) p.accent else p.text)
                                    if (has) {
                                        Box(Modifier.size(4.dp).clip(CircleShape)
                                            .background(p.accent2))
                                    } else {
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

