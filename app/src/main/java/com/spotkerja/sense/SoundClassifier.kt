package com.spotkerja.sense

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Klasifikasi jenis suara ambient via YAMNet (TFLite, 521 kelas AudioSet).
 * Model lokal di assets — sepenuhnya offline. Input: PCM 16 kHz mono float,
 * jendela 0.975 s (15600 sampel) per inference.
 */
class SoundClassifier private constructor(
    private val interp: Interpreter,
    private val labels: List<String>,
    private val inputIs2D: Boolean,
    private val numClasses: Int,
) {

    private val window = FloatArray(WINDOW)
    private var pos = 0

    /** Feed chunk PCM16; jalankan inference tiap window penuh. */
    @Synchronized
    fun onPcm(buf: ShortArray, n: Int, acc: ScanAccumulator) {
        var i = 0
        while (i < n) {
            val take = minOf(n - i, WINDOW - pos)
            for (k in 0 until take) window[pos + k] = buf[i + k] / 32768f
            pos += take; i += take
            if (pos == WINDOW) {
                pos = 0
                runInference(acc)
            }
        }
    }

    private fun runInference(acc: ScanAccumulator) {
        val scores = runCatching {
            val out = Array(1) { FloatArray(numClasses) }
            val input: Any = if (inputIs2D) arrayOf(window) else window
            interp.run(input, out)
            out[0]
        }.getOrNull() ?: return
        val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: return
        val label = labels.getOrNull(bestIdx) ?: return
        // "Silence" tidak informatif untuk soundscape — skip dari tally.
        if (label != "Silence" && scores[bestIdx] >= 0.25f) acc.addSound(label)
    }

    fun close() = runCatching { interp.close() }

    companion object {
        const val WINDOW = 15600 // 0.975 s @ 16 kHz — input YAMNet

        /** Load model + label dari assets; null bila gagal (metrik tetap jalan). */
        fun create(ctx: Context): SoundClassifier? = runCatching {
            val modelBytes = ctx.assets.open("yamnet.tflite").readBytes()
            val buf = ByteBuffer.allocateDirect(modelBytes.size)
                .order(ByteOrder.nativeOrder())
                .apply { put(modelBytes); rewind() }
            val interp = Interpreter(buf, Interpreter.Options().apply { numThreads = 2 })
            val inShape = interp.getInputTensor(0).shape()
            val outShape = interp.getOutputTensor(0).shape()
            val classes = outShape.lastOrNull() ?: return null
            val labels = ctx.assets.open("yamnet_labels.txt").bufferedReader()
                .readLines().filter { it.isNotBlank() }
            if (labels.size < classes) return null
            SoundClassifier(interp, labels, inShape.size == 2, classes)
        }.getOrNull()
    }
}
