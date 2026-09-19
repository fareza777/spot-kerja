package com.spotkerja.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spotkerja.settings.ThemeMode

data class Palette(
    val bg: Color,
    val card: Color,
    val high: Color,
    val border: Color,
    val accent: Color,
    val accent2: Color,
    val accentDim: Color,
    val gold: Color,
    val text: Color,
    val textDim: Color,
    val good: Color,
    val mid: Color,
    val bad: Color,
)

enum class ThemeOption(val label: String, val swatch: List<Color>) {
    EMERALD("Emerald", listOf(Color(0xFF070D0B), Color(0xFF4ED8C3), Color(0xFF5CC8FF))),
    OCEAN("Ocean", listOf(Color(0xFF070B12), Color(0xFF5C9DFF), Color(0xFF4ED8C3))),
    SUNSET("Sunset", listOf(Color(0xFF140B09), Color(0xFFFF8A5C), Color(0xFFF2B84B))),
    VIOLET("Violet", listOf(Color(0xFF0C0A14), Color(0xFFB48CFF), Color(0xFF5CC8FF))),
    MONO("Mono", listOf(Color(0xFF0B0B0B), Color(0xFFE8E8E8), Color(0xFF9A9A9A))),
}

fun paletteFor(t: ThemeOption, dark: Boolean = true): Palette = if (dark) darkPalette(t) else lightPalette(t)

private fun darkPalette(t: ThemeOption): Palette = when (t) {
    ThemeOption.EMERALD -> Palette(
        bg = Color(0xFF070D0B), card = Color(0xFF101A16), high = Color(0xFF182521),
        border = Color(0xFF223330), accent = Color(0xFF4ED8C3), accent2 = Color(0xFF5CC8FF),
        accentDim = Color(0xFF2FA08F), gold = Color(0xFFF2B84B),
        text = Color(0xFFEFF7F3), textDim = Color(0xFF93A8A0),
        good = Color(0xFF4ED8C3), mid = Color(0xFFF2B84B), bad = Color(0xFFE5655F),
    )
    ThemeOption.OCEAN -> Palette(
        bg = Color(0xFF070B12), card = Color(0xFF0F1722), high = Color(0xFF182234),
        border = Color(0xFF24334A), accent = Color(0xFF5C9DFF), accent2 = Color(0xFF4ED8C3),
        accentDim = Color(0xFF3A6FBF), gold = Color(0xFFF2B84B),
        text = Color(0xFFEFF4FB), textDim = Color(0xFF93A4BC),
        good = Color(0xFF4ED8C3), mid = Color(0xFFF2B84B), bad = Color(0xFFE5655F),
    )
    ThemeOption.SUNSET -> Palette(
        bg = Color(0xFF140B09), card = Color(0xFF1E1310), high = Color(0xFF2A1C16),
        border = Color(0xFF3D2A20), accent = Color(0xFFFF8A5C), accent2 = Color(0xFFF2B84B),
        accentDim = Color(0xFFC06035), gold = Color(0xFFFFC86B),
        text = Color(0xFFFBF3EE), textDim = Color(0xFFB59E92),
        good = Color(0xFFFF8A5C), mid = Color(0xFFF2B84B), bad = Color(0xFFE5655F),
    )
    ThemeOption.VIOLET -> Palette(
        bg = Color(0xFF0C0A14), card = Color(0xFF161226), high = Color(0xFF201A36),
        border = Color(0xFF32294E), accent = Color(0xFFB48CFF), accent2 = Color(0xFF5CC8FF),
        accentDim = Color(0xFF7A5CBC), gold = Color(0xFFF2B84B),
        text = Color(0xFFF4F0FB), textDim = Color(0xFFA398BC),
        good = Color(0xFFB48CFF), mid = Color(0xFFF2B84B), bad = Color(0xFFE5655F),
    )
    ThemeOption.MONO -> Palette(
        bg = Color(0xFF0B0B0B), card = Color(0xFF141414), high = Color(0xFF1F1F1F),
        border = Color(0xFF2E2E2E), accent = Color(0xFFE8E8E8), accent2 = Color(0xFF9A9A9A),
        accentDim = Color(0xFF6E6E6E), gold = Color(0xFFD8D8D8),
        text = Color(0xFFF2F2F2), textDim = Color(0xFF9A9A9A),
        good = Color(0xFFE8E8E8), mid = Color(0xFF9A9A9A), bad = Color(0xFF565656),
    )
}

private fun lightPalette(t: ThemeOption): Palette = when (t) {
    ThemeOption.EMERALD -> Palette(
        bg = Color(0xFFF2F8F6), card = Color(0xFFFFFFFF), high = Color(0xFFE2EFEA),
        border = Color(0xFFD2E3DC), accent = Color(0xFF0D9484), accent2 = Color(0xFF1D7FB8),
        accentDim = Color(0xFF7FC5B8), gold = Color(0xFFBE8A17),
        text = Color(0xFF10221D), textDim = Color(0xFF5C7269),
        good = Color(0xFF0D9484), mid = Color(0xFFBE8A17), bad = Color(0xFFD24540),
    )
    ThemeOption.OCEAN -> Palette(
        bg = Color(0xFFF2F6FC), card = Color(0xFFFFFFFF), high = Color(0xFFE2EBF7),
        border = Color(0xFFD2DDED), accent = Color(0xFF2E6FD8), accent2 = Color(0xFF0D9484),
        accentDim = Color(0xFF8FB1E8), gold = Color(0xFFBE8A17),
        text = Color(0xFF12233C), textDim = Color(0xFF5D6E85),
        good = Color(0xFF0D9484), mid = Color(0xFFBE8A17), bad = Color(0xFFD24540),
    )
    ThemeOption.SUNSET -> Palette(
        bg = Color(0xFFFCF4F0), card = Color(0xFFFFFFFF), high = Color(0xFFF6E4DB),
        border = Color(0xFFEED5C8), accent = Color(0xFFE0612F), accent2 = Color(0xFFBE8A17),
        accentDim = Color(0xFFEEA47E), gold = Color(0xFFBE8A17),
        text = Color(0xFF2E1710), textDim = Color(0xFF857064),
        good = Color(0xFF0D9484), mid = Color(0xFFBE8A17), bad = Color(0xFFD24540),
    )
    ThemeOption.VIOLET -> Palette(
        bg = Color(0xFFF7F5FC), card = Color(0xFFFFFFFF), high = Color(0xFFEBE5F6),
        border = Color(0xFFDDD2EE), accent = Color(0xFF7B54D8), accent2 = Color(0xFF1D7FB8),
        accentDim = Color(0xFFB39BE4), gold = Color(0xFFBE8A17),
        text = Color(0xFF1E1533), textDim = Color(0xFF6E6385),
        good = Color(0xFF0D9484), mid = Color(0xFFBE8A17), bad = Color(0xFFD24540),
    )
    ThemeOption.MONO -> Palette(
        bg = Color(0xFFF6F6F6), card = Color(0xFFFFFFFF), high = Color(0xFFECECEC),
        border = Color(0xFFDEDEDE), accent = Color(0xFF2B2B2B), accent2 = Color(0xFF6E6E6E),
        accentDim = Color(0xFFBDBDBD), gold = Color(0xFF555555),
        text = Color(0xFF171717), textDim = Color(0xFF6E6E6E),
        good = Color(0xFF2B2B2B), mid = Color(0xFF8A8A8A), bad = Color(0xFFCFCFCF),
    )
}

val LocalPalette = compositionLocalOf { paletteFor(ThemeOption.EMERALD) }

val LocalScoreColor = compositionLocalOf<(Float) -> Color> {
    { paletteFor(ThemeOption.EMERALD).accent }
}

private val Type = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp),
)

@Composable
fun SpotkerjaTheme(
    option: ThemeOption = ThemeOption.EMERALD,
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val p = paletteFor(option, dark)
    val scheme = if (dark) darkColorScheme(
        primary = p.accent,
        onPrimary = p.bg,
        primaryContainer = p.accentDim.copy(alpha = 0.4f),
        onPrimaryContainer = p.text,
        secondary = p.gold,
        onSecondary = p.bg,
        secondaryContainer = p.gold.copy(alpha = 0.2f),
        onSecondaryContainer = p.text,
        tertiary = p.accent2,
        background = p.bg,
        onBackground = p.text,
        surface = p.bg,
        onSurface = p.text,
        surfaceVariant = p.card,
        onSurfaceVariant = p.textDim,
        outline = p.border,
        error = p.bad,
    ) else lightColorScheme(
        primary = p.accent,
        onPrimary = p.card,
        primaryContainer = p.accentDim.copy(alpha = 0.35f),
        onPrimaryContainer = p.text,
        secondary = p.gold,
        onSecondary = p.card,
        secondaryContainer = p.gold.copy(alpha = 0.15f),
        onSecondaryContainer = p.text,
        tertiary = p.accent2,
        background = p.bg,
        onBackground = p.text,
        surface = p.bg,
        onSurface = p.text,
        surfaceVariant = p.card,
        onSurfaceVariant = p.textDim,
        outline = p.border,
        error = p.bad,
    )
    CompositionLocalProvider(
        LocalPalette provides p,
        LocalScoreColor provides { s: Float -> scoreColor(s, p) },
    ) {
        MaterialTheme(colorScheme = scheme, typography = Type, content = content)
    }
}

fun scoreColor(score: Float, p: Palette): Color = when {
    score >= 70f -> p.good
    score >= 45f -> p.mid
    else -> p.bad
}

@Composable
fun heroBrush(): Brush = with(LocalPalette.current) {
    Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), accent2.copy(alpha = 0.18f), card))
}

@Composable
fun accentBrush(): Brush = with(LocalPalette.current) {
    Brush.sweepGradient(listOf(accent, accent2, accent))
}
