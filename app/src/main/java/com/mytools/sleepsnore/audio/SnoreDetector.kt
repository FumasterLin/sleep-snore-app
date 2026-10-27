package com.mytools.sleepsnore.audio

import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SnoreIntensity
import kotlin.math.max
import kotlin.math.min

/**
 * 打鼾检测器与临床特征分析器
 * 负责自适应底噪跟踪、30秒 Epoch 聚合、打鼾评分 (Snore Score) 计算
 * 以及疑似睡眠呼吸暂停 (Apnea / 气流中断) 识别
 */
class SnoreDetector(
    private val sessionId: Long,
    private val startTimeMs: Long
) {
    // 自适应环境底噪跟踪 (初始默认 32dB)
    var noiseFloorDb: Float = 32.0f
        private set

    // 当前 30 秒 Epoch 累加器
    private var epochFrameCount = 0
    private var epochSnoreFrameCount = 0
    private var epochMaxDb = 0f
    private var epochSumDb = 0.0
    private var epochHasApnea = false

    // 整场监测累计统计
    var totalSessionFrames: Long = 0
        private set
    var totalSnoreFrames: Long = 0
        private set

    var totalMildSeconds: Float = 0f
        private set
    var totalLoudSeconds: Float = 0f
        private set
    var totalEpicSeconds: Float = 0f
        private set

    var totalApneaEvents: Int = 0
        private set

    // 鼾声解剖部位来源统计 (舌后坠 / 软腭 / 呛咳喘鸣)
    var tongueBaseSnoreFrames: Long = 0
        private set
    var palatalSnoreFrames: Long = 0
        private set
    var gaspSnoreFrames: Long = 0
        private set

    var minSessionDb: Float = 100f
        private set
    var maxSessionDb: Float = 0f
        private set
    private var sumSessionDb: Double = 0.0

    // 呼吸暂停状态机
    private var recentSnoreEnergyHistory = FloatArray(8) // 记录最近几个周期的打鼾能量
    private var historyIndex = 0
    private var continuousSilenceFrames = 0
    private var hadLoudSnoreBeforeSilence = false

    // 智能微震动干预连续大声打鼾计数
    var continuousEpicSeconds: Float = 0f
        private set

    /**
     * 传入单个帧分析结果，如果完成了 30 秒窗口则返回生成的 ChartPoint，否则返回 null
     */
    fun processFrame(analysis: AudioProcessor.FrameAnalysis): ChartPoint? {
        val frameDurationSeconds = AudioConfig.FRAME_SIZE.toFloat() / AudioConfig.SAMPLE_RATE

        epochFrameCount++
        epochSumDb += analysis.db
        if (analysis.db > epochMaxDb) {
            epochMaxDb = analysis.db
        }

        totalSessionFrames++
        sumSessionDb += analysis.db
        minSessionDb = min(minSessionDb, analysis.db)
        maxSessionDb = max(maxSessionDb, analysis.db)

        if (analysis.isSnoringCandidate) {
            epochSnoreFrameCount++
            totalSnoreFrames++

            // 解剖部位特征累加
            if (analysis.gaspRatio > 0.35f) {
                gaspSnoreFrames++
            } else if (analysis.tongueBaseRatio > analysis.palatalRatio) {
                tongueBaseSnoreFrames++
            } else {
                palatalSnoreFrames++
            }

            val level = SnoreIntensity.fromDb(analysis.db)
            when (level) {
                SnoreIntensity.MILD -> {
                    totalMildSeconds += frameDurationSeconds
                    continuousEpicSeconds = 0f
                }
                SnoreIntensity.LOUD -> {
                    totalLoudSeconds += frameDurationSeconds
                    continuousEpicSeconds += frameDurationSeconds
                    hadLoudSnoreBeforeSilence = true
                }
                SnoreIntensity.EPIC -> {
                    totalEpicSeconds += frameDurationSeconds
                    continuousEpicSeconds += frameDurationSeconds
                    hadLoudSnoreBeforeSilence = true
                }
                SnoreIntensity.QUIET -> {
                    continuousEpicSeconds = 0f
                }
            }

            // 呼吸暂停气流恢复特征判定：
            // 若之前经历了 10 秒 ~ 35 秒的异常静止沉默（约 150 ~ 550 帧无声），随后突发强力大声打鼾冲开气道
            val silenceDurationSec = continuousSilenceFrames * frameDurationSeconds
            if (hadLoudSnoreBeforeSilence && silenceDurationSec in 10.0f..35.0f && analysis.db >= 52.0f) {
                totalApneaEvents++
                epochHasApnea = true
                hadLoudSnoreBeforeSilence = false
            }

            continuousSilenceFrames = 0
        } else {
            // 在安静无打鼾时，统计异常静默时间
            continuousSilenceFrames++
            continuousEpicSeconds = 0f

            // 平滑更新环境底噪 (EMA 衰减系数 0.005)
            if (analysis.db < noiseFloorDb + 10f) {
                noiseFloorDb = noiseFloorDb * 0.995f + analysis.db * 0.005f
            }

            // 静默超过 45 秒则认为进入正常平稳安睡，重置暂停判定链
            if (continuousSilenceFrames * frameDurationSeconds > 45f) {
                hadLoudSnoreBeforeSilence = false
            }
        }

        // 检查是否达到 30 秒一个 Epoch
        if (epochFrameCount >= AudioConfig.FRAMES_PER_EPOCH) {
            val snoreRatioInEpoch = (epochSnoreFrameCount.toFloat() / epochFrameCount).coerceIn(0f, 1f)
            val level = if (snoreRatioInEpoch > 0.15f) {
                SnoreIntensity.fromDb(epochMaxDb)
            } else {
                SnoreIntensity.QUIET
            }

            val offsetSeconds = ((totalSessionFrames * AudioConfig.FRAME_SIZE) / AudioConfig.SAMPLE_RATE)
            val point = ChartPoint(
                sessionId = sessionId,
                timestampMs = startTimeMs + (offsetSeconds * 1000),
                offsetSeconds = offsetSeconds,
                durationSeconds = AudioConfig.EPOCH_DURATION_SECONDS.toFloat(),
                snorePercentage = snoreRatioInEpoch,
                volumeDb = epochMaxDb,
                intensityLevel = level,
                isApneaSuspected = epochHasApnea
            )

            // 重置当前 Epoch
            epochFrameCount = 0
            epochSnoreFrameCount = 0
            epochMaxDb = 0f
            epochSumDb = 0.0
            epochHasApnea = false

            return point
        }

        return null
    }

    /**
     * 计算预估 AHI (Apnea-Hypopnea Index)
     */
    fun calculateEstimatedAhi(sessionDurationSeconds: Long): Float {
        val hours = sessionDurationSeconds / 3600f
        return if (hours > 0.2f) {
            (totalApneaEvents / hours).coerceAtLeast(0f)
        } else 0f
    }

    /**
     * 计算 SnoreLab 经典打鼾评分 (Snore Score)
     */
    fun calculateSnoreScore(totalDurationSeconds: Long): Float {
        if (totalDurationSeconds <= 0) return 0f

        val mildPct = (totalMildSeconds / totalDurationSeconds).coerceIn(0f, 1f)
        val loudPct = (totalLoudSeconds / totalDurationSeconds).coerceIn(0f, 1f)
        val epicPct = (totalEpicSeconds / totalDurationSeconds).coerceIn(0f, 1f)

        // 官方权重配比：轻声 15，大声 60，超大声 160，频次非线性加权
        val score = (mildPct * 15f + loudPct * 65f + epicPct * 180f) * 1.5f
        return score.coerceIn(0f, 180f)
    }

    fun getAverageDb(): Float {
        return if (totalSessionFrames > 0) {
            (sumSessionDb / totalSessionFrames).toFloat()
        } else {
            35.0f
        }
    }

    fun getPalatalPercent(): Float {
        val total = totalSnoreFrames.toFloat()
        return if (total > 0) (palatalSnoreFrames / total).coerceIn(0f, 1f) else 0.5f
    }

    fun getTongueBasePercent(): Float {
        val total = totalSnoreFrames.toFloat()
        return if (total > 0) (tongueBaseSnoreFrames / total).coerceIn(0f, 1f) else 0.35f
    }

    fun getGaspPercent(): Float {
        val total = totalSnoreFrames.toFloat()
        return if (total > 0) (gaspSnoreFrames / total).coerceIn(0f, 1f) else 0.15f
    }
}
