package com.mytools.sleepsnore.audio

import android.media.AudioFormat
import android.media.MediaRecorder

object AudioConfig {
    const val SAMPLE_RATE = 16000 // 16kHz 为语音与鼾声分析黄金采样率
    const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    const val FRAME_SIZE = 512 // 每帧 512 个样本点 (~32ms)
    const val FFT_SIZE = 512

    const val EPOCH_DURATION_SECONDS = 30 // 30秒临床统计窗口
    const val FRAMES_PER_EPOCH = (SAMPLE_RATE * EPOCH_DURATION_SECONDS) / FRAME_SIZE // ~937 帧

    // 打鼾频谱主要特征集中在 100Hz ~ 1000Hz
    const val SNORE_FREQ_LOW = 100.0
    const val SNORE_FREQ_HIGH = 1000.0

    // 分贝级别阈值
    const val DB_MILD_THRESHOLD = 40.0f
    const val DB_LOUD_THRESHOLD = 50.0f
    const val DB_EPIC_THRESHOLD = 65.0f

    // 智能采样音频最大片段数（防存储膨胀）
    const val MAX_AUDIO_SAMPLES_PER_SESSION = 20
    const val SAMPLE_RECORD_SECONDS = 12 // 每段录音 12 秒
}
