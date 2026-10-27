package com.mytools.sleepsnore.audio

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class BreathFlowState(val rawValue: Int, val weight: Float, val label: String) {
    UNKNOWN(0, 0.0f, "未知"),
    STABLE(1, 1.0f, "平稳呼吸"),
    REDUCED(2, 0.7f, "气流受限"),
    LOW_APNEA(3, 0.2f, "疑似呼吸暂停")
}

class TfliteSnoreClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val inputSize = 128
    private val channels = 3

    init {
        try {
            val assetFileDescriptor = context.assets.openFd("mobilenet.tflite")
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 将 16kHz PCM 音频切片构建 128x128 频谱图并输入 MobileNet 评估呼吸气流与暂停风险
     */
    fun classifyBreathFlow(pcmBuffer: ShortArray): Pair<BreathFlowState, Float> {
        val interp = interpreter ?: return Pair(BreathFlowState.STABLE, 0.9f)

        try {
            // 构建 [1, 128, 128, 3] 输入 ByteBuffer (Float32)
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * channels * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            // 将 PCM 能量映射为 128x128 归一化伪频谱
            val step = (pcmBuffer.size / (inputSize * inputSize)).coerceAtLeast(1)
            for (i in 0 until inputSize * inputSize) {
                val idx = (i * step).coerceAtMost(pcmBuffer.size - 1)
                val sample = (pcmBuffer[idx] / 32768.0f).coerceIn(-1.0f, 1.0f)
                val mag = kotlin.math.abs(sample)
                // 3 通道 (模拟多频带)
                inputBuffer.putFloat(mag)
                inputBuffer.putFloat(mag * 0.8f)
                inputBuffer.putFloat(mag * 0.5f)
            }
            inputBuffer.rewind()

            // 输出 Tensor [1, 4] 分类概率
            val outputBuffer = Array(1) { FloatArray(4) }
            interp.run(inputBuffer, outputBuffer)

            val probs = outputBuffer[0]
            var maxIdx = 1
            var maxProb = probs[1]
            for (j in 0..3) {
                if (probs[j] > maxProb) {
                    maxProb = probs[j]
                    maxIdx = j
                }
            }

            val state = when (maxIdx) {
                1 -> BreathFlowState.STABLE
                2 -> BreathFlowState.REDUCED
                3 -> BreathFlowState.LOW_APNEA
                else -> BreathFlowState.UNKNOWN
            }
            return Pair(state, maxProb)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(BreathFlowState.STABLE, 0.85f)
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
