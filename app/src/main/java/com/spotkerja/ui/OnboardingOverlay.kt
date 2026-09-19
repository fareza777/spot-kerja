package com.spotkerja.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotkerja.ui.theme.LocalPalette
import kotlinx.coroutines.launch

private data class Page(
    val icon: ImageVector,
    val kicker: String,
    val title: String,
    val body: String,
)

private val PAGES = listOf(
    Page(Icons.Default.Radar, "HOW IT WORKS", "Scan your spots",
        "Stand at each desk position and let SpotWise listen for 30–60 seconds. " +
            "It quietly samples Wi-Fi strength, ping, jitter, ambient light, noise, " +
            "and which way you're facing — all from sensors already in your phone."),
    Page(Icons.Default.Insights, "SCORING", "A score that fits your mode",
        "Every spot gets a 0–100 Work Spot Score. Work, Study, Gaming and Video Call " +
            "each weigh metrics differently — Gaming cares about latency, Video Call " +
            "cares about facing the light. The best spot gets crowned."),
    Page(Icons.Default.Tune, "CUSTOM SCANS", "Tune everything",
        "Pick which metrics to measure, set your own ping target, save named scan " +
            "presets and up to 8 named spots. Make SpotWise measure what matters to you."),
    Page(Icons.Default.Bolt, "FAST SCAN", "One tap, one spot",
        "In a rush? Fast Scan grades a single position in as little as 10 seconds — " +
            "from the button on Home, or the widget on your home screen."),
    Page(Icons.Default.Shield, "PRIVATE BY DESIGN", "Everything stays on-device",
        "No account, no cloud, no AI in the loop. Scores are practical estimates from " +
            "phone sensors — history and reports live only on this device."),
)

/** Onboarding — 5 slide animasi, hanya di launch pertama. */
@Composable
fun OnboardingOverlay(onDone: () -> Unit) {
    val p = LocalPalette.current
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.size - 1

    Box(Modifier.fillMaxSize().background(p.bg)) {
        // ambient glow
        Box(
            Modifier.fillMaxWidth().height(380.dp).align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(p.accent.copy(alpha = 0.10f), p.bg.copy(alpha = 0f))))
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${pager.currentPage + 1} / ${PAGES.size}",
                    style = MaterialTheme.typography.labelSmall, color = p.textDim)
                Spacer(Modifier.weight(1f))
                if (!last) {
                    TextButton(onClick = onDone) { Text("Skip", color = p.textDim) }
                }
            }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                OnboardingPage(PAGES[i], i == pager.currentPage)
            }

            // animated dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGES.size) { i ->
                    val w by animateFloatAsState(
                        if (pager.currentPage == i) 20f else 6f,
                        tween(300), label = "dot")
                    Box(
                        Modifier.size(w.dp, 6.dp).clip(CircleShape)
                            .background(
                                if (pager.currentPage == i) p.accent else p.border),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    if (last) onDone()
                    else scope.launch {
                        pager.animateScrollToPage(pager.currentPage + 1)
                    }
                },
                Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text(if (last) "Start scanning" else "Next",
                    style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            Text("Estimates from phone sensors — not lab measurements.",
                style = MaterialTheme.typography.labelSmall, color = p.textDim,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun OnboardingPage(pg: Page, active: Boolean) {
    val p = LocalPalette.current
    val pulse = rememberInfiniteTransition(label = "ob")
    val iconScale by pulse.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconPulse")
    val glowAlpha by pulse.animateFloat(
        0.25f, 0.5f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow")
    val contentAlpha by animateFloatAsState(
        if (active) 1f else 0f, tween(400), label = "contentAlpha")

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).scale(iconScale)
                    .clip(CircleShape)
                    .background(p.accent.copy(alpha = 0.10f * glowAlpha * 4))
                    .blur(30.dp))
            Box(
                Modifier.size(118.dp).scale(iconScale)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.linearGradient(listOf(
                            p.accent.copy(alpha = 0.18f), p.accent2.copy(alpha = 0.10f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(pg.icon, null, Modifier.size(54.dp), tint = p.accent)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(pg.kicker, style = MaterialTheme.typography.labelSmall,
            color = p.accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(pg.title, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha })
        Spacer(Modifier.height(14.dp))
        Text(pg.body, style = MaterialTheme.typography.bodyMedium,
            color = p.textDim, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
                .graphicsLayer { alpha = contentAlpha })
    }
}
