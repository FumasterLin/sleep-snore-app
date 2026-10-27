package com.mytools.sleepsnore.audio

import android.content.Context
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.data.model.SnoreIntensity
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SmartAudioRecorder(
    private val context: Context,
    private val sessionId: Long
) {
    // 预触发环形缓冲区 (保留最近 3 秒 PCM)
    private val preTriggerSamplesCount = AudioConfig.SAMPLE_RATE * 3
    private val preTriggerRingBuffer = ShortArray(preTriggerSamplesCount)
    private var ringWriteIndex = 0
    private var ringCount = 0

    // 当前活跃采样录制状态
    private var isRecordingSample = false
    private var currentSampleFileOutputStream: FileOutputStream? = null
    private var currentSampleFile: File? = null
    private var currentSampleBytesWritten = 0
    private var currentSampleStartOffsetSeconds = 0L
    private var currentSamplePeakDb = 0f
    private var currentSampleLevel = SnoreIntensity.QUIET
    private val targetSampleBytes = AudioConfig.SAMPLE_RATE * AudioConfig.SAMPLE_RECORD_SECONDS * 2 // 16-bit = 2 bytes/sample

    private var recordedSamplesCount = 0
    private var lastRecordedTimeSeconds = -100L

    fun onFrame(
        buffer: ShortArray,
        analysis: AudioProcessor.FrameAnalysis,
        currentOffsetSeconds: Long
    ): AudioSample? {
        val sampleRate = AudioConfig.SAMPLE_RATE

        // 1. 始终写入预触发环形缓冲区
        for (sample in buffer) {
            preTriggerRingBuffer[ringWriteIndex] = sample
            ringWriteIndex = (ringWriteIndex + 1) % preTriggerSamplesCount
            if (ringCount < preTriggerSamplesCount) ringCount++
        }

        // 2. 检查是否正在录制打鼾样本片段
        if (isRecordingSample) {
            val fos = currentSampleFileOutputStream
            if (fos != null && currentSampleFile != null) {
                val byteBuffer = ByteBuffer.allocate(buffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in buffer) {
                    byteBuffer.putShort(s)
                }
                fos.write(byteBuffer.array())
                currentSampleBytesWritten += byteBuffer.array().size

                if (analysis.db > currentSamplePeakDb) {
                    currentSamplePeakDb = analysis.db
                }

                if (currentSampleBytesWritten >= targetSampleBytes) {
                    // 录制完成，封装 WAV 头
                    fos.flush()
                    fos.close()
                    writeWavHeader(currentSampleFile!!, currentSampleBytesWritten, sampleRate, 1, 16)

                    val audioSample = AudioSample(
                        sessionId = sessionId,
                        timestampMs = System.currentTimeMillis(),
                        offsetSeconds = currentSampleStartOffsetSeconds,
                        durationSeconds = AudioConfig.SAMPLE_RECORD_SECONDS.toFloat(),
                        peakDb = currentSamplePeakDb,
                        level = currentSampleLevel,
                        audioPath = currentSampleFile!!.absolutePath,
                        label = "${currentSampleLevel.title}鼾声 (${currentSamplePeakDb.toInt()}dB)"
                    )

                    isRecordingSample = false
                    currentSampleFileOutputStream = null
                    currentSampleFile = null
                    lastRecordedTimeSeconds = currentOffsetSeconds
                    recordedSamplesCount++

                    return audioSample
                }
            }
            return null
        }

        // 3. 检查是否触发新的打鼾录音
        // 涵盖轻声 (MILD)、大声 (LOUD) 与超大声 (EPIC) 各阶段
        val level = SnoreIntensity.fromDb(analysis.db)
        val minIntervalSeconds = if (level == SnoreIntensity.EPIC) 30 else if (level == SnoreIntensity.LOUD) 45 else 90
        val shouldTrigger = analysis.isSnoringCandidate &&
                level != SnoreIntensity.QUIET &&
                (currentOffsetSeconds - lastRecordedTimeSeconds >= minIntervalSeconds) &&
                (recordedSamplesCount < AudioConfig.MAX_AUDIO_SAMPLES_PER_SESSION)

        if (shouldTrigger) {
            startNewSample(currentOffsetSeconds, analysis.db, level)
        }

        return null
    }

    private fun startNewSample(offsetSeconds: Long, initialDb: Float, level: SnoreIntensity) {
        try {
            val dir = File(context.filesDir, "snore_audio/$sessionId")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "snore_${offsetSeconds}s_${System.currentTimeMillis()}.wav")
            val fos = FileOutputStream(file)

            // 先预留 44 字节 WAV 头
            fos.write(ByteArray(44))

            // 写入环形缓冲区内的前 3 秒历史音频
            val preTriggerBytes = ByteBuffer.allocate(ringCount * 2).order(ByteOrder.LITTLE_ENDIAN)
            val startIdx = if (ringCount < preTriggerSamplesCount) 0 else ringWriteIndex
            for (i in 0 until ringCount) {
                val idx = (startIdx + i) % preTriggerSamplesCount
                preTriggerBytes.putShort(preTriggerRingBuffer[idx])
            }
            fos.write(preTriggerBytes.array())

            currentSampleFile = file
            currentSampleFileOutputStream = fos
            currentSampleBytesWritten = preTriggerBytes.array().size
            currentSampleStartOffsetSeconds = offsetSeconds - 3
            currentSamplePeakDb = initialDb
            currentSampleLevel = level
            isRecordingSample = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecordingSample = false
        }
    }

    private fun writeWavHeader(file: File, pcmDataLength: Int, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(totalDataLen))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16)) // Subchunk1Size (16 for PCM)
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (1 for PCM)
        raf.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(sampleRate))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(pcmDataLength))
        raf.close()
    }

    fun release() {
        try {
            currentSampleFileOutputStream?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
