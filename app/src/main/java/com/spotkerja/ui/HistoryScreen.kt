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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotkerja.data.ScanSession
import com.spotkerja.ui.components.GlassCard
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
) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    // Set of days that have scans (for dot markers)
    val daysWithScans = remember(sessions, month) {
        sessions.map {
            Instant.ofEpochMilli(it.createdAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    val daySessions = remember(sessions, selectedDay) {
        selectedDay?.let { d ->
            sessions.filter {
                Instant.ofEpochMilli(it.createdAtEpochMs)
                    .atZone(ZoneId.systemDefault()).toLocalDate() == d
            }
        } ?: sessions
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        item {
            Text("Scan history", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Tap a day to filter", style = MaterialTheme.typography.bodySmall,
                color = p.textDim)
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
                Box(Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedDay == null) "No scans yet — start your first one."
                        else "No scans this day.",
                        color = p.textDim,
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

