package com.spotkerja.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/**
 * Focus timer — Pomodoro-style: pilih durasi fokus, countdown, menit yang
 * berjalan dicatat ke spot ini ("Proven spot": skor sensor × deep work nyata).
 */
@Composable
fun FocusScreen(
    spotLabel: String,
    onLogMinutes: (String, Int) -> Unit,
    onDone: () -> Unit,
) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    var durationMin by remember { mutableStateOf(25) }
    var remainingSec by remember { mutableStateOf(durationMin * 60) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val elapsedMin = ((durationMin * 60 - remainingSec) / 60)

    LaunchedEffect(running) {
        while (running && remainingSec > 0) {
            delay(1000)
            remainingSec--
        }
        if (remainingSec <= 0) {
            running = false
            finished = true
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Back")
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Focus mode", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("at $spotLabel — log real deep-work minutes here",
            style = MaterialTheme.typography.bodySmall, color = p.textDim,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(36.dp))

        // Countdown ring
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2
                val frac = if (durationMin > 0)
                    remainingSec.toFloat() / (durationMin * 60f) else 0f
                drawArc(p.border, -90f, 360f, false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(p.accent, -90f, 360f * frac, false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(remainingSec / 60, remainingSec % 60),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (running) p.accent else p.text,
                )
                Text(if (finished) "done" else if (running) "focusing…" else "ready",
                    style = MaterialTheme.typography.labelMedium, color = p.textDim)
            }
        }

        Spacer(Modifier.height(32.dp))
        // Duration chips (only while idle)
        if (!running && !finished) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(15, 25, 45).forEach { m ->
                    FilterChip(
                        selected = durationMin == m,
                        onClick = { durationMin = m; remainingSec = m * 60 },
                        label = { Text("${m}m") },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!finished) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            running = !running
                        },
                        Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = p.accent, contentColor = p.bg),
                    ) {
                        Icon(
                            if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (running) "Pause" else if (elapsedMin > 0) "Resume" else "Start",
                            fontWeight = FontWeight.Bold)
                    }
                }
                // Log & exit — selalu tersedia begitu ada menit berjalan / selesai
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val mins = if (finished) durationMin else elapsedMin
                        if (mins > 0) onLogMinutes(spotLabel, mins)
                        onDone()
                    },
                    Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, p.accent.copy(alpha = 0.6f)),
                ) {
                    Text(
                        if (finished) "Log ${durationMin}m & done"
                        else "Log ${elapsedMin}m & done",
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Minutes logged count toward each spot's proven record in History.",
            style = MaterialTheme.typography.labelSmall,
            color = p.textDim.copy(alpha = 0.7f),
        )
    }
}
