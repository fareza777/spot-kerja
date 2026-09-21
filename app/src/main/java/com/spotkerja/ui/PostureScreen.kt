package com.spotkerja.ui

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.spotkerja.ui.components.GlassCard
import com.spotkerja.ui.components.ScoreRing
import com.spotkerja.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2

/** Hasil posture: sudut dalam derajat + skor 0–100. */
data class PostureResult(
    val neckAngle: Float,
    val shoulderTilt: Float,
    val torsoLean: Float,
    val frames: Int,
) {
    val score: Int
        get() = (100f
            - maxOf(0f, neckAngle - 12f) * 2.2f
            - maxOf(0f, shoulderTilt - 6f) * 3f
            - maxOf(0f, torsoLean - 12f) * 1.8f).coerceIn(0f, 100f).toInt()

    fun tips(): List<String> {
        val t = mutableListOf<String>()
        if (neckAngle > 12f) t += "Head leans forward ~${neckAngle.toInt()}° — raise the screen to eye level"
        if (shoulderTilt > 6f) t += "Shoulders uneven by ~${shoulderTilt.toInt()}° — check armrests/desk height"
        if (torsoLean > 12f) t += "Torso leans ~${torsoLean.toInt()}° — sit back into the chair"
        if (t.isEmpty()) t += "Posture looks balanced — keep it up"
        return t
    }
}

/**
 * Ergonomic posture check — kamera depan 6 detik, MediaPipe Pose Landmarker
 * on-device. Landmark: telinga(7,8) bahu(11,12) pinggul(23,24).
 */
@SuppressLint("MissingPermission") // izin CAMERA sudah diminta sebelum navigasi
@Composable
fun PostureScreen(onDone: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var secondsLeft by remember { mutableStateOf(6) }
    var measuring by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PostureResult?>(null) }
    var noPose by remember { mutableStateOf(false) }
    var modelFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }

    // Landmark terakumulasi — diisi analyzer thread, dibaca saat selesai.
    val necks = remember { mutableListOf<Float>() }
    val tilts = remember { mutableListOf<Float>() }
    val leans = remember { mutableListOf<Float>() }
    var landmarker by remember { mutableStateOf<PoseLandmarker?>(null) }

    DisposableEffect(Unit) {
        val lm = runCatching {
            PoseLandmarker.createFromOptions(ctx,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder()
                        .setModelAssetPath("pose_lite.task").build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { r, _ ->
                        val pose = r.landmarks().firstOrNull() ?: return@setResultListener
                        if (pose.size < 25) return@setResultListener
                        val earX = (pose[7].x() + pose[8].x()) / 2f
                        val earY = (pose[7].y() + pose[8].y()) / 2f
                        val shX = (pose[11].x() + pose[12].x()) / 2f
                        val shY = (pose[11].y() + pose[12].y()) / 2f
                        val hipX = (pose[23].x() + pose[24].x()) / 2f
                        val hipY = (pose[23].y() + pose[24].y()) / 2f
                        // Sudut vs vertikal — normalisasi oleh tinggi torso.
                        necks += Math.toDegrees(
                            atan2(abs(earX - shX).toDouble(),
                                abs(shY - earY).toDouble().coerceAtLeast(1e-6))).toFloat()
                        tilts += Math.toDegrees(atan2(
                            abs(pose[11].y() - pose[12].y()).toDouble(),
                            abs(pose[11].x() - pose[12].x()).toDouble()
                                .coerceAtLeast(1e-6))).toFloat()
                        leans += Math.toDegrees(atan2(
                            abs(shX - hipX).toDouble(),
                            abs(hipY - shY).toDouble().coerceAtLeast(1e-6))).toFloat()
                    }
                    .build())
        }.getOrNull()
        landmarker = lm
        modelFailed = (lm == null)
        onDispose { runCatching { lm?.close() } }
    }

    // CameraX: front camera — preview + analysis (frame → pose).
    LaunchedEffect(landmarker, attempt) {
        if (landmarker == null) return@LaunchedEffect
        measuring = true
        while (secondsLeft > 0) { delay(1000); secondsLeft-- }
        measuring = false
        if (necks.size >= 8) {
            fun median(l: List<Float>) = l.sorted()[l.size / 2]
            result = PostureResult(median(necks), median(tilts), median(leans), necks.size)
        } else noPose = true
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Back")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Posture check", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("Sit as you work — front camera watches neck, shoulders and torso.",
            style = MaterialTheme.typography.bodySmall, color = p.textDim)
        Spacer(Modifier.height(14.dp))

        if (result == null) {
            // Preview kamera + frame analyzer
            GlassCard(Modifier.fillMaxWidth().height(340.dp)) {
                Box {
                    AndroidView(
                        factory = { c ->
                            PreviewView(c).also { pv ->
                                val provider = runCatching {
                                    ProcessCameraProvider.getInstance(c).get()
                                }.getOrNull() ?: return@also
                                val preview = androidx.camera.core.Preview.Builder()
                                    .build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                var lastFeed = 0L
                                analysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(c)) { img ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastFeed > 120 && measuring) {
                                        lastFeed = now
                                        val bmp = runCatching { img.toBitmap() }.getOrNull()
                                        if (bmp != null) runCatching {
                                            landmarker?.detectAsync(
                                                BitmapImageBuilder(bmp).build(), now)
                                        }
                                    }
                                    img.close()
                                }
                                runCatching {
                                    provider.bindToLifecycle(lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (measuring) {
                        Surface(
                            Modifier.align(Alignment.TopCenter).padding(10.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = p.bg.copy(alpha = 0.75f),
                        ) {
                            Text("Measuring… ${secondsLeft}s",
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = p.accent)
                        }
                    }
                }
            }
            if (noPose) {
                Spacer(Modifier.height(12.dp))
                Text("No person detected — point the front camera at yourself while seated.",
                    style = MaterialTheme.typography.bodySmall, color = p.gold)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    noPose = false; secondsLeft = 6
                    necks.clear(); tilts.clear(); leans.clear()
                    attempt++
                }) { Text("Try again", color = p.accent) }
            }
            if (modelFailed) {
                Spacer(Modifier.height(12.dp))
                Text("Pose model unavailable on this device.",
                    style = MaterialTheme.typography.bodySmall, color = p.gold)
            }
        } else {
            val r = result!!
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccessibilityNew, null, Modifier.size(28.dp),
                        tint = p.accent)
                    Spacer(Modifier.height(10.dp))
                    ScoreRing(r.score.toFloat(), "posture", sizeDp = 110)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        PostureStat("Neck fwd", "${r.neckAngle.toInt()}°")
                        PostureStat("Shoulder tilt", "${r.shoulderTilt.toInt()}°")
                        PostureStat("Torso lean", "${r.torsoLean.toInt()}°")
                    }
                    Spacer(Modifier.height(14.dp))
                    r.tips().forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = p.textDim, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${r.frames} frames analyzed · on-device estimate",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = {
                result = null; noPose = false; secondsLeft = 6
                necks.clear(); tilts.clear(); leans.clear()
                attempt++
            }, Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.accent, contentColor = p.bg)) {
                Text("Measure again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PostureStat(label: String, value: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = p.accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.textDim)
    }
}
