package com.spotkerja.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// Palet dark premium — deep green-charcoal + aksen teal→cyan & amber
val BgDeep = Color(0xFF070D0B)
val BgElevated = Color(0xFF0B1210)
val SurfaceCard = Color(0xFF101A16)
val SurfaceHigh = Color(0xFF182521)
val SurfaceBorder = Color(0xFF223330)
val AccentTeal = Color(0xFF4ED8C3)
val AccentCyan = Color(0xFF5CC8FF)
val AccentTealDim = Color(0xFF2FA08F)
val AccentAmber = Color(0xFFF2B84B)
val TextPrimary = Color(0xFFEFF7F3)
val TextSecondary = Color(0xFF93A8A0)
val ScoreGood = Color(0xFF4ED8C3)
val ScoreMid = Color(0xFFF2B84B)
val ScoreBad = Color(0xFFE5655F)

val ScoreGradient = Brush.sweepGradient(listOf(AccentTeal, AccentCyan, AccentTeal))
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF123B33), Color(0xFF0D2A33)))
val TrackColor = Color(0xFF1B2A25)

private val Scheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = Color(0xFF04211C),
    primaryContainer = Color(0xFF155047),
    onPrimaryContainer = Color(0xFFBCF4E9),
    secondary = AccentAmber,
    onSecondary = Color(0xFF2E1F00),
    secondaryContainer = Color(0xFF4D3A0F),
    onSecondaryContainer = Color(0xFFFCE7B5),
    tertiary = AccentCyan,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgDeep,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = SurfaceHigh,
    outline = SurfaceBorder,
    error = ScoreBad,
)

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
fun SpotkerjaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}

fun scoreColor(score: Float): Color = when {
    score >= 70f -> ScoreGood
    score >= 45f -> ScoreMid
    else -> ScoreBad
}
