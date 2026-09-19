package com.spotkerja.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.SectionHeader
import com.spotkerja.ui.theme.LocalPalette
import com.spotkerja.ui.theme.ThemeOption
import com.spotkerja.ui.theme.paletteFor

@Composable
fun SettingsScreen(
    theme: ThemeOption,
    adsEnabled: Boolean,
    onThemeChange: (ThemeOption) -> Unit,
    onAdsChange: (Boolean) -> Unit,
    appVersion: String = "1.0.0",
) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var showAbout by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        }

        item {
            SectionHeader("Theme")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption.entries.forEach { t ->
                    val pal = paletteFor(t)
                    val sel = t == theme
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onThemeChange(t) }
                            .border(
                                1.dp,
                                if (sel) p.accent else p.border,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape)
                                .background(Brush.sweepGradient(t.swatch)),
                        ) {
                            if (sel) Icon(Icons.Default.Check, null, Modifier.align(Alignment.Center),
                                tint = pal.text)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(t.label, style = MaterialTheme.typography.labelSmall,
                            color = if (sel) p.accent else p.textDim)
                    }
                }
            }
        }

        item {
            SectionHeader("General")
            Spacer(Modifier.height(10.dp))
            GlassCard {
                SettingsRow(
                    icon = Icons.Default.Campaign,
                    title = "Show ads",
                    sub = "Test AdMob banner & interstitial",
                ) {
                    Switch(checked = adsEnabled, onCheckedChange = onAdsChange)
                }
            }
        }

        item {
            SectionHeader("About")
            Spacer(Modifier.height(10.dp))
            GlassCard {
                SettingsRow(Icons.Default.Share, "Share app", "Tell a friend about Spotkerja") {
                    TextButton(onClick = {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "Spotkerja — find the best desk spot using phone sensors. " +
                                    "https://play.google.com/store/apps/details?id=${ctx.packageName}")
                        }
                        ctx.startActivity(Intent.createChooser(i, "Share"))
                    }) { Text("Share") }
                }
                HorizontalDivider(color = p.border, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Default.Star, "Rate on Play Store", "Support the app") {
                    TextButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${ctx.packageName}")))
                        }.onFailure {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://play.google.com/store/apps/details?id=${ctx.packageName}")))
                        }
                    }) { Text("Rate") }
                }
                HorizontalDivider(color = p.border, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Default.Info, "About Spotkerja", "v$appVersion") {
                    TextButton(onClick = { showAbout = true }) { Text("View") }
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Spotkerja") },
            text = {
                Text(
                    "Find the best spot to work, study, game or take calls using phone sensors: " +
                        "Wi-Fi signal, ping/jitter/loss, ambient light, noise, and facing direction.\n\n" +
                        "All processing happens on-device — no data leaves your phone. " +
                        "Scores are practical estimates, not scientific measurements.\n\n" +
                        "Version $appVersion"
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    trailing: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(36.dp), shape = RoundedCornerShape(10.dp),
            color = p.accentDim.copy(alpha = 0.15f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = p.accent)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = p.textDim)
        }
        trailing()
    }
}
