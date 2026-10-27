package com.mytools.sleepsnore.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * 轻量级高保真 WAV 音频真实波形振幅包络提取器
 */
object WaveformExtractor {

    private val cache = mutableMapOf<String, FloatArray>()

    suspend fun extractWaveform(audioPath: String, barCount: Int = 42): FloatArray = withContext(Dispatchers.IO) {
        cache[audioPath]?.let { return@withContext it }

        val file = File(audioPath)
        if (!file.exists() || file.length() <= 44) {
            return@withContext defaultMockBars(barCount)
        }

        try {
            val fileBytes = file.readBytes()
            val pcmDataOffset = 44 // 标准 WAV PCM 数据起点
            val pcmLength = fileBytes.size - pcmDataOffset
            val sampleCount = pcmLength / 2 // 16-bit PCM (2 字节 / sample)

            if (sampleCount < barCount) {
                return@withContext defaultMockBars(barCount)
            }

            val samplesPerBar = sampleCount / barCount
            val buffer = ByteBuffer.wrap(fileBytes, pcmDataOffset, pcmLength).order(ByteOrder.LITTLE_ENDIAN)

            val rmsValues = FloatArray(barCount)
            var maxRms = 0.0001f

            for (i in 0 until barCount) {
                var sumSquare = 0.0
                for (j in 0 until samplesPerBar) {
                    if (buffer.hasRemaining()) {
                        val s = buffer.short.toDouble()
                        sumSquare += s * s
                    }
                }
                val rms = sqrt(sumSquare / samplesPerBar).toFloat()
                rmsValues[i] = rms
                if (rms > maxRms) maxRms = rms
            }

            // 归一化至 0.15f ~ 1.0f
            val normalized = FloatArray(barCount) { idx ->
                val ratio = (rmsValues[idx] / maxRms).coerceIn(0f, 1f)
                0.15f + ratio * 0.85f
            }

            cache[audioPath] = normalized
            return@withContext normalized
        } catch (_: Exception) {
            return@withContext defaultMockBars(barCount)
        }
    }

    private fun defaultMockBars(count: Int): FloatArray {
        return FloatArray(count) { idx ->
            0.2f + 0.7f * kotlin.math.abs(kotlin.math.sin(idx * 0.35f)).toFloat()
        }
    }
}
