package com.spotkerja.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotkerja.ui.theme.LocalPalette
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: String, val body: String)

private val PAGES = listOf(
    Page(Icons.Default.Radar, "Scan your spots",
        "Measure Wi-Fi, ping, light, noise and facing direction at each corner of your room."),
    Page(Icons.Default.Insights, "Compare & score",
        "Each spot gets a 0–100 score tuned to your mode — Work, Study, Gaming or Video Call."),
    Page(Icons.Default.Bolt, "Fast Scan",
        "In a rush? One tap measures a single spot in seconds — or use the home-screen widget."),
)

/** Onboarding 3 slide — hanya muncul di launch pertama. */
@Composable
fun OnboardingOverlay(onDone: () -> Unit) {
    val p = LocalPalette.current
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(p.bg.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDone) { Text("Skip", color = p.textDim) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                val pg = PAGES[i]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(120.dp).clip(RoundedCornerShape(36.dp))
                            .background(p.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(pg.icon, null, Modifier.size(56.dp), tint = p.accent)
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(pg.title, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(pg.body, style = MaterialTheme.typography.bodyMedium,
                        color = p.textDim, textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGES.size) { i ->
                    Box(
                        Modifier.size(if (pager.currentPage == i) 18.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pager.currentPage == i) p.accent
                                else p.border),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (pager.currentPage < PAGES.size - 1) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else onDone()
                },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    if (pager.currentPage < PAGES.size - 1) "Next" else "Get started",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Practical estimates from phone sensors — not lab measurements.",
                style = MaterialTheme.typography.labelSmall, color = p.textDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}
