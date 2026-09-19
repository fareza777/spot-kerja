package com.spotkerja.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotkerja.ui.theme.scoreColor

/** Ring skor besar dengan angka di tengah. */
@Composable
fun ScoreRing(score: Float, label: String, modifier: Modifier = Modifier, sizeDp: Int = 132) {
    Box(modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = sizeDp.dp.toPx() * 0.09f
            val inset = stroke / 2
            drawArc(
                color = androidx.compose.ui.graphics.Color(0xFF22312C),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = scoreColor(score),
                startAngle = -90f, sweepAngle = 360f * (score / 100f), useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.0f".format(score), fontSize = (sizeDp / 3.6f).sp, fontWeight = FontWeight.Bold,
                color = scoreColor(score))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bar horizontal satu metrik (dipakai di breakdown + compare). */
@Composable
fun MetricBar(name: String, score: Float?, valueText: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        if (score == null) {
            Box(Modifier.fillMaxWidth().height(6.dp), contentAlignment = Alignment.CenterStart) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(androidx.compose.ui.graphics.Color(0xFF22312C),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
                }
            }
            Text("tidak tersedia", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        } else {
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                drawRoundRect(androidx.compose.ui.graphics.Color(0xFF22312C),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
                drawRoundRect(scoreColor(score),
                    size = Size(size.width * (score / 100f), size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            }
        }
    }
}

/** Tile kecil untuk metrik live saat scanning. */
@Composable
fun LiveTile(title: String, value: String, sub: String = "", modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Chart batang perbandingan skor antar-spot. */
@Composable
fun CompareChart(spots: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        spots.forEach { (label, score) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
                Canvas(Modifier.weight(1f).height(22.dp)) {
                    drawRoundRect(androidx.compose.ui.graphics.Color(0xFF22312C),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
                    drawRoundRect(scoreColor(score),
                        size = Size(size.width * (score / 100f).coerceIn(0f, 1f), size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
                }
                Text("%.0f".format(score), Modifier.width(36.dp),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = scoreColor(score))
            }
        }
    }
}
