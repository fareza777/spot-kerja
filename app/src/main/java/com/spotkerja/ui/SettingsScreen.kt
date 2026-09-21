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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.spotkerja.settings.ScanOptions
import com.spotkerja.settings.ScanPreset
import com.spotkerja.settings.ThemeMode
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.SectionHeader
import com.spotkerja.ui.theme.LocalPalette
import com.spotkerja.ui.theme.ThemeOption
import com.spotkerja.ui.theme.paletteFor

@Composable
fun SettingsScreen(
    theme: ThemeOption,
    themeMode: ThemeMode,
    adsEnabled: Boolean,
    scanOptions: ScanOptions,
    fastDurationSec: Int,
    onThemeChange: (ThemeOption) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAdsChange: (Boolean) -> Unit,
    onScanOptionsChange: (ScanOptions) -> Unit,
    onFastDurationChange: (Int) -> Unit,
    appVersion: String,
    presets: List<ScanPreset> = emptyList(),
    durationSec: Int = 30,
    spotCount: Int = 3,
    onSavePreset: (String) -> Unit = {},
    onApplyPreset: (ScanPreset) -> Unit = {},
    onDeletePreset: (String) -> Unit = {},
) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var showAbout by remember { mutableStateOf(false) }
    var showSavePreset by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var pingHost by remember(scanOptions.pingHost) { mutableStateOf(scanOptions.pingHost) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(vertical = 22.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        }

        // ---------- Appearance ----------
        item {
            SectionHeader("Appearance")
            Spacer(Modifier.height(10.dp))
            // Light / Dark / System segmented
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    ThemeMode.LIGHT to Icons.Default.LightMode,
                    ThemeMode.DARK to Icons.Default.DarkMode,
                    ThemeMode.SYSTEM to Icons.Default.SettingsBrightness,
                ).forEach { (m, icon) ->
                    val sel = m == themeMode
                    Row(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) p.accent else p.card)
                            .clickable { onThemeModeChange(m) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, Modifier.size(15.dp),
                            tint = if (sel) p.bg else p.textDim)
                        Spacer(Modifier.width(6.dp))
                        Text(m.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) p.bg else p.textDim)
                    }
                }
            }
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

        // ---------- Scan customization ----------
        item {
            SectionHeader("Scan customization")
            Spacer(Modifier.height(10.dp))
            GlassCard {
                MetricToggle(Icons.Default.Wifi, "Wi-Fi signal (RSSI)",
                    "Signal strength & link speed", scanOptions.wifi) {
                    onScanOptionsChange(scanOptions.copy(wifi = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.NetworkPing, "Ping & latency",
                    "Ping, jitter & packet loss", scanOptions.ping) {
                    onScanOptionsChange(scanOptions.copy(ping = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.WbSunny, "Ambient light",
                    "Lux level & stability", scanOptions.light) {
                    onScanOptionsChange(scanOptions.copy(light = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.Mic, "Noise level",
                    "Ambient loudness via mic", scanOptions.noise) {
                    onScanOptionsChange(scanOptions.copy(noise = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.Explore, "Facing direction",
                    "Orientation vs sun position", scanOptions.orientation) {
                    onScanOptionsChange(scanOptions.copy(orientation = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.CellTower, "Cellular signal",
                    "Backup signal estimate", scanOptions.cellular) {
                    onScanOptionsChange(scanOptions.copy(cellular = it))
                }
                OptDivider()
                MetricToggle(Icons.Default.Hub, "Deep metrics",
                    "Mesh, route trace, env sensors & sound events",
                    scanOptions.extended) {
                    onScanOptionsChange(scanOptions.copy(extended = it))
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, Modifier.size(16.dp), tint = p.accentDim)
                        Spacer(Modifier.width(8.dp))
                        Text("Fast scan duration", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text("${fastDurationSec}s", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = p.accent)
                    }
                    Slider(
                        value = fastDurationSec.toFloat(),
                        onValueChange = { onFastDurationChange(it.toInt()) },
                        valueRange = 10f..30f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = p.accent,
                            activeTrackColor = p.accent,
                            inactiveTrackColor = p.border,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10s", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                        Text("30s", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pingHost,
                        onValueChange = { pingHost = it },
                        label = { Text("Ping target (optional)") },
                        placeholder = { Text("Wi-Fi gateway (default)") },
                        singleLine = true,
                        enabled = scanOptions.ping,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (pingHost != scanOptions.pingHost) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            onScanOptionsChange(scanOptions.copy(pingHost = pingHost))
                        }) { Text("Apply ping target") }
                    }
                }
            }
        }

        // ---------- Scan presets ----------
        item {
            SectionHeader("Scan presets")
            Spacer(Modifier.height(10.dp))
            GlassCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Save your current scan setup — metrics, duration and spot " +
                        "count — as a named preset you can re-apply in one tap.",
                        style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    Spacer(Modifier.height(12.dp))
                    if (presets.isEmpty()) {
                        Text("No presets yet.", style = MaterialTheme.typography.bodySmall,
                            color = p.textDim)
                    } else {
                        presets.forEach { preset ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(preset.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${preset.durationSec}s • ${preset.spotCount} spots • " +
                                            listOfNotNull(
                                                "wifi".takeIf { preset.options.wifi },
                                                "ping".takeIf { preset.options.ping },
                                                "light".takeIf { preset.options.light },
                                                "noise".takeIf { preset.options.noise },
                                                "facing".takeIf { preset.options.orientation },
                                                "cell".takeIf { preset.options.cellular },
                                            ).joinToString("/"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.textDim)
                                }
                                TextButton(onClick = { onApplyPreset(preset) }) {
                                    Text("Apply", color = p.accent)
                                }
                                IconButton(onClick = { onDeletePreset(preset.name) }) {
                                    Icon(Icons.Default.Delete, "Delete preset",
                                        Modifier.size(18.dp), tint = p.textDim)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { presetName = ""; showSavePreset = true },
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.accentDim),
                    ) {
                        Icon(Icons.Default.BookmarkAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save current as preset (${durationSec}s · $spotCount spots)")
                    }
                }
            }
        }

        // ---------- General ----------
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

        // ---------- About ----------
        item {
            SectionHeader("About")
            Spacer(Modifier.height(10.dp))
            GlassCard {
                SettingsRow(Icons.Default.Share, "Share app", "Tell a friend about SpotWise") {
                    TextButton(onClick = {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "SpotWise — find your perfect work spot using phone sensors. " +
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
                SettingsRow(Icons.Default.Info, "About SpotWise", "v$appVersion") {
                    TextButton(onClick = { showAbout = true }) { Text("View") }
                }
            }
        }
    }

    if (showSavePreset) {
        AlertDialog(
            onDismissRequest = { showSavePreset = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = presetName, onValueChange = { presetName = it },
                    label = { Text("Preset name") },
                    placeholder = { Text("e.g. Night gaming, Café work") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePreset(presetName.trim().ifEmpty { "Preset" })
                    showSavePreset = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePreset = false }) { Text("Cancel") }
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("SpotWise") },
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
private fun OptDivider() {
    HorizontalDivider(color = LocalPalette.current.border,
        modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun MetricToggle(
    icon: ImageVector,
    title: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
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
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
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
