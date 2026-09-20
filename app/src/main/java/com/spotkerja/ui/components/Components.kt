package com.spotkerja.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotkerja.ui.theme.LocalPalette
import com.spotkerja.ui.theme.LocalScoreColor

/** Bordered card — the base premium surface (subtle top-down gradient). */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, p.border.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            Modifier.background(
                Brush.verticalGradient(
                    listOf(p.high.copy(alpha = 0.55f), p.card))),
            content = content,
        )
    }
}

/** Score ring with gradient sweep, soft glow and fill animation.
 *  Counts up from 0 on first composition. */
@Composable
fun ScoreRing(score: Float, label: String, modifier: Modifier = Modifier, sizeDp: Int = 132) {
    val sc = LocalScoreColor.current
    val p = LocalPalette.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) {
        anim.animateTo(score.coerceIn(0f, 100f),
            tween(900, easing = FastOutSlowInEasing))
    }
    val animated = anim.value
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
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) {
        anim.animateTo((score ?: 0f).coerceIn(0f, 100f),
            tween(800, easing = FastOutSlowInEasing))
    }
    val animated = anim.value
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
                    Box(
                        Modifier.size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(p.accentDim.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(it, null, Modifier.size(13.dp), tint = p.accent)
                    }
                    Spacer(Modifier.width(6.dp))
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

/** Polygon/radar chart overlaying per-metric sub-scores for up to 3 spots.
 *  Axis labels are drawn around the polygon; null scores sit at the centre. */
@Composable
fun RadarChart(
    series: List<Pair<String, List<Float?>>>, // label → sub-scores (0-100, null = n/a)
    axisLabels: List<String>,
    modifier: Modifier = Modifier,
    sizeDp: Int = 250,
) {
    val p = LocalPalette.current
    val colors = listOf(p.accent, p.gold, p.accent2)
    val n = axisLabels.size
    if (n < 3 || series.isEmpty()) return
    val labelPadDp = 34.dp

    Box(modifier.size(sizeDp.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2 - labelPadDp.toPx()
            val pt = { i: Int, f: Float ->
                val a = -Math.PI / 2 + 2 * Math.PI * i / n
                Offset(c.x + (r * f * kotlin.math.cos(a)).toFloat(),
                    c.y + (r * f * kotlin.math.sin(a)).toFloat())
            }
            // rings + spokes
            listOf(1f, 0.66f, 0.33f).forEach { f ->
                val path = Path().apply {
                    repeat(n) { i -> if (i == 0) moveTo(pt(i, f).x, pt(i, f).y) else lineTo(pt(i, f).x, pt(i, f).y) }
                    close()
                }
                drawPath(path, p.border.copy(alpha = 0.8f), style = Stroke(1.dp.toPx()))
            }
            repeat(n) { i -> drawLine(p.border.copy(alpha = 0.7f), c, pt(i, 1f), 1.dp.toPx()) }
            // axis end dots
            repeat(n) { i -> drawCircle(p.border, 2.5.dp.toPx(), pt(i, 1f)) }
            // series polygons — nulls collapse to centre
            series.take(3).forEachIndexed { si, (_, vals) ->
                val col = colors[si % colors.size]
                val pts = vals.mapIndexed { i, v -> pt(i, (v ?: 0f) / 100f) }
                val path = Path().apply {
                    pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                    close()
                }
                drawPath(path, col.copy(alpha = 0.14f))
                drawPath(path, col.copy(alpha = 0.9f), style = Stroke(2.dp.toPx()))
                vals.forEachIndexed { i, v ->
                    if (v != null) drawCircle(col, 3.5.dp.toPx(), pts[i])
                }
            }
        }
        // Axis labels around the polygon
        axisLabels.forEachIndexed { i, lab ->
            val a = -Math.PI / 2 + 2 * Math.PI * i / n
            // Labels sit between polygon edge and box edge.
            val rDp = sizeDp / 2f - labelPadDp.value * 0.42f
            val xDp = sizeDp / 2f + (rDp * kotlin.math.cos(a)).toFloat()
            val yDp = sizeDp / 2f + (rDp * kotlin.math.sin(a)).toFloat()
            Box(
                Modifier.offset(x = (xDp - 26).dp, y = (yDp - 8).dp).width(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    lab, style = MaterialTheme.typography.labelSmall,
                    color = p.textDim, fontWeight = FontWeight.Medium,
                    maxLines = 1, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Rolling sparkline for live series (lux/noise) during a scan. */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color? = null) {
    val p = LocalPalette.current
    val col = color ?: p.accent
    if (values.size < 2) {
        Box(modifier.height(44.dp).fillMaxWidth())
        return
    }
    Canvas(modifier.height(44.dp).fillMaxWidth()) {
        val max = values.max(); val min = values.min()
        val range = (max - min).coerceAtLeast(1f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - ((v - min) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        // area fill
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(col.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(path, col, style = Stroke(2.dp.toPx()))
        drawCircle(col, 3.5f.dp.toPx(), Offset(
            (values.size - 1) * step,
            size.height - ((values.last() - min) / range) * size.height))
    }
}

/** Squashes the element while pressed, springing back on release. */
@Composable
fun Modifier.bouncyPress(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Content that fades in and rises once, staggered by [index] (~80 ms step). */
@Composable
fun RiseIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f,
            tween(460, delayMillis = index * 80, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 30.dp.toPx()
    }) { content() }
}

/** Soft diagonal light band sweeping left→right, for gradient CTAs. */
@Composable
fun ShimmerBand(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -0.45f, targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "x",
    )
    Canvas(modifier) {
        val w = size.width
        val cx = w * x
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.20f),
                1f to Color.Transparent,
                start = Offset(cx - w * 0.28f, 0f),
                end = Offset(cx + w * 0.28f, size.height),
            )
        )
    }
}

/** Consistent section header. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}
