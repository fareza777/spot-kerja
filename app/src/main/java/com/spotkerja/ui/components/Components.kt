package com.spotkerja.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotkerja.ui.theme.LocalPalette
import com.spotkerja.ui.theme.LocalScoreColor

/** Bordered card — the base premium surface. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LocalPalette.current.card),
        border = BorderStroke(1.dp, LocalPalette.current.border),
        shape = RoundedCornerShape(20.dp),
        content = content,
    )
}

/** Score ring with gradient sweep, soft glow and fill animation. */
@Composable
fun ScoreRing(score: Float, label: String, modifier: Modifier = Modifier, sizeDp: Int = 132) {
    val sc = LocalScoreColor.current
    val p = LocalPalette.current
    val animated by animateFloatAsState(
        targetValue = score.coerceIn(0f, 100f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "score",
    )
    Box(modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(sizeDp.dp * 0.72f)
                .blur(24.dp)
                .graphicsLayer { alpha = 0.35f },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(sc(score), radius = size.minDimension / 2)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = sizeDp.dp.toPx() * 0.085f
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = p.border,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0f to sc(score),
                    0.85f to sc(score).copy(alpha = 0.55f),
                    1f to sc(score),
                ),
                startAngle = -90f, sweepAngle = 360f * (animated / 100f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "%.0f".format(animated),
                fontSize = (sizeDp / 3.4f).sp, fontWeight = FontWeight.ExtraBold,
                color = sc(score),
            )
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Single metric bar with fill animation. */
@Composable
fun MetricBar(name: String, score: Float?, valueText: String, modifier: Modifier = Modifier) {
    val sc = LocalScoreColor.current
    val p = LocalPalette.current
    val animated by animateFloatAsState(
        targetValue = (score ?: 0f).coerceIn(0f, 100f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar",
    )
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(5.dp))
        Canvas(Modifier.fillMaxWidth().height(7.dp)) {
            drawRoundRect(p.border, cornerRadius = CornerRadius(10f))
            if (score != null) {
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(sc(score).copy(alpha = 0.7f), sc(score))),
                    size = Size(size.width * (animated / 100f), size.height),
                    cornerRadius = CornerRadius(10f),
                )
            }
        }
        if (score == null) {
            Text("not available", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        }
    }
}

/** Live metric tile with icon. */
@Composable
fun LiveTile(title: String, value: String, sub: String = "", icon: ImageVector? = null,
             modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = p.card),
        border = BorderStroke(1.dp, p.border),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, null, Modifier.size(13.dp), tint = p.accentDim)
                    Spacer(Modifier.width(5.dp))
                }
                Text(title, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Animated cross-spot comparison chart with rank badges. */
@Composable
fun CompareChart(spots: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    val sc = LocalScoreColor.current
    val p = LocalPalette.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(spots) { visible = true }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        spots.forEachIndexed { rank, (label, score) ->
            val width by animateFloatAsState(
                targetValue = if (visible) score else 0f,
                animationSpec = tween(700, delayMillis = rank * 120, easing = FastOutSlowInEasing),
                label = "cmp",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank, Modifier.width(30.dp))
                Text(
                    label, Modifier.width(74.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Canvas(Modifier.weight(1f).height(24.dp)) {
                    drawRoundRect(p.border, cornerRadius = CornerRadius(12f))
                    drawRoundRect(
                        Brush.horizontalGradient(
                            listOf(sc(score).copy(alpha = 0.65f), sc(score))),
                        size = Size(size.width * (width / 100f).coerceIn(0f, 1f), size.height),
                        cornerRadius = CornerRadius(12f),
                    )
                }
                Text("%.0f".format(score), Modifier.width(38.dp).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = sc(score))
            }
        }
    }
}

/** Rank medal for 1st/2nd/3rd. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val color = when (rank) {
        0 -> p.gold
        1 -> Color(0xFF9FB3BC)
        else -> Color(0xFF8A6A45)
    }
    Surface(
        modifier.size(24.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("${rank + 1}", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

/** Rotating radar sweep — the scanning signature animation. */
@Composable
fun RadarSweep(progressFrac: Float, modifier: Modifier = Modifier, sizeDp: Int = 210) {
    val p = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2
            listOf(1f, 0.72f, 0.44f).forEach { f ->
                drawCircle(p.border, radius = r * f, style = Stroke(1.5.dp.toPx()))
            }
            drawLine(p.border, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
            drawLine(p.border, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
            rotate(angle, c) {
                drawArc(
                    Brush.sweepGradient(
                        0f to p.accent.copy(alpha = 0.5f),
                        0.22f to p.accent.copy(alpha = 0.0f),
                        1f to Color.Transparent,
                    ),
                    startAngle = 0f, sweepAngle = 90f, useCenter = true,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                )
                drawCircle(p.accent.copy(alpha = 0.9f), radius = 3.dp.toPx(),
                    center = Offset(c.x + r, c.y))
            }
            drawArc(
                p.border, -90f, 360f, false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                Brush.sweepGradient(listOf(p.accent, p.accent2, p.accent)),
                -90f, 360f * progressFrac.coerceIn(0f, 1f), false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(p.accent, radius = 6.dp.toPx() * pulse)
            drawCircle(p.accent.copy(alpha = 0.3f), radius = 14.dp.toPx() * pulse)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(progressFrac * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
            )
            Text("scanning", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Consistent section header. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}
