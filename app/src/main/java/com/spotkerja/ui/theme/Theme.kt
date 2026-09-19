package com.spotkerja.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet dark premium: charcoal-hijau gelap + aksen teal & amber
val BgDeep = Color(0xFF0B1210)
val SurfaceCard = Color(0xFF141D1A)
val SurfaceHigh = Color(0xFF1C2823)
val AccentTeal = Color(0xFF4ED8C3)
val AccentTealDim = Color(0xFF2FA08F)
val AccentAmber = Color(0xFFF2B84B)
val TextPrimary = Color(0xFFECF4F0)
val TextSecondary = Color(0xFF93A8A0)
val ScoreGood = Color(0xFF4ED8C3)
val ScoreMid = Color(0xFFF2B84B)
val ScoreBad = Color(0xFFE5655F)

private val Scheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = Color(0xFF06231E),
    primaryContainer = Color(0xFF134B43),
    onPrimaryContainer = Color(0xFFB8F2E7),
    secondary = AccentAmber,
    onSecondary = Color(0xFF2E1F00),
    secondaryContainer = Color(0xFF4A3810),
    onSecondaryContainer = Color(0xFFFBE3B0),
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgDeep,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = SurfaceHigh,
    outline = Color(0xFF2C3B36),
    error = ScoreBad,
)

@Composable
fun SpotkerjaTheme(content: @Composable () -> Unit) {
    // App ini memang dirancang dark-first; tetap hormati sistem via isSystemInDarkTheme
    // bila nanti ditambah skema terang.
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = Scheme, content = content)
}

fun scoreColor(score: Float): Color = when {
    score >= 70f -> ScoreGood
    score >= 45f -> ScoreMid
    else -> ScoreBad
}
