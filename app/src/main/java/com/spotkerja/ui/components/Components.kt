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
import com.spotkerja.ui.theme.*

/** Kartu dengan border halus — dasar tampilan premium. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(20.dp),
        content = content,
    )
}

/** Ring skor dengan gradient sweep, glow, dan animasi isi. */
@Composable
fun ScoreRing(score: Float, label: String, modifier: Modifier = Modifier, sizeDp: Int = 132) {
    val animated by animateFloatAsState(
        targetValue = score.coerceIn(0f, 100f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "score",
    )
    Box(modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        // glow lembut di belakang ring
        Box(
            Modifier.size(sizeDp.dp * 0.72f)
                .blur(24.dp)
                .graphicsLayer { alpha = 0.35f },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(scoreColor(score), radius = size.minDimension / 2)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = sizeDp.dp.toPx() * 0.085f
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = TrackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0f to scoreColor(score),
                    0.85f to scoreColor(score).copy(alpha = 0.55f),
                    1f to scoreColor(score),
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
                color = scoreColor(score),
            )
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bar horizontal satu metrik dengan animasi isi. */
@Composable
fun MetricBar(name: String, score: Float?, valueText: String, modifier: Modifier = Modifier) {
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
            drawRoundRect(TrackColor, cornerRadius = CornerRadius(10f))
            if (score != null) {
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(scoreColor(score).copy(alpha = 0.7f), scoreColor(score))),
                    size = Size(size.width * (animated / 100f), size.height),
                    cornerRadius = CornerRadius(10f),
                )
            }
        }
        if (score == null) {
            Text("tidak tersedia", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        }
    }
}

/** Tile metrik live dengan icon. */
@Composable
fun LiveTile(title: String, value: String, sub: String = "", icon: ImageVector? = null,
             modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, null, Modifier.size(13.dp), tint = AccentTealDim)
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

/** Chart batang perbandingan antar-spot, animasi saat muncul. */
@Composable
fun CompareChart(spots: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
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
                Text(label, Modifier.width(52.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
                Canvas(Modifier.weight(1f).height(24.dp)) {
                    drawRoundRect(TrackColor, cornerRadius = CornerRadius(12f))
                    drawRoundRect(
                        Brush.horizontalGradient(
                            listOf(scoreColor(score).copy(alpha = 0.65f), scoreColor(score))),
                        size = Size(size.width * (width / 100f).coerceIn(0f, 1f), size.height),
                        cornerRadius = CornerRadius(12f),
                    )
                }
                Text("%.0f".format(score), Modifier.width(38.dp).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = scoreColor(score))
            }
        }
    }
}

/** Medali peringkat 1/2/3. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val color = when (rank) {
        0 -> AccentAmber
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

/** Animasi radar sweep — identitas visual saat scanning. */
@Composable
fun RadarSweep(progressFrac: Float, modifier: Modifier = Modifier, sizeDp: Int = 210) {
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
            // lingkaran konsentris
            listOf(1f, 0.72f, 0.44f).forEach { f ->
                drawCircle(TrackColor, radius = r * f, style = Stroke(1.5.dp.toPx()))
            }
            // crosshair tipis
            drawLine(TrackColor, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
            drawLine(TrackColor, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
            // sweep gradient — cone mengikuti sudut rotasi
            rotate(angle, c) {
                drawArc(
                    Brush.sweepGradient(
                        0f to AccentTeal.copy(alpha = 0.5f),
                        0.22f to AccentTeal.copy(alpha = 0.0f),
                        1f to Color.Transparent,
                    ),
                    startAngle = 0f, sweepAngle = 90f, useCenter = true,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                )
                drawCircle(AccentTeal.copy(alpha = 0.9f), radius = 3.dp.toPx(),
                    center = Offset(c.x + r, c.y))
            }
            // ring progress luar
            drawArc(
                TrackColor, -90f, 360f, false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                Brush.sweepGradient(listOf(AccentTeal, AccentCyan, AccentTeal)),
                -90f, 360f * progressFrac.coerceIn(0f, 1f), false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round),
            )
            // titik pusat berdenyut
            drawCircle(AccentTeal, radius = 6.dp.toPx() * pulse)
            drawCircle(AccentTeal.copy(alpha = 0.3f), radius = 14.dp.toPx() * pulse)
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

/** Header section konsisten. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}
