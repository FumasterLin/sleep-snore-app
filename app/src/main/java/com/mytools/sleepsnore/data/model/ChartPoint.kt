package com.mytools.sleepsnore.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 对应临床标准与 SnoreLab 的 30 秒时间窗口（Epoch）统计点
 */
@Entity(
    tableName = "chart_points",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChartPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestampMs: Long,                    // 该点绝对时间戳 (毫秒)
    val offsetSeconds: Long,                  // 距离睡眠开始的偏移秒数
    val durationSeconds: Float = 30f,         // 统计窗口长度 (默认30秒)
    val snorePercentage: Float = 0f,          // 30秒内打鼾时长占比 (0.0~1.0)
    val volumeDb: Float = 0f,                 // 该窗口最高/代表性分贝
    val intensityLevel: SnoreIntensity = SnoreIntensity.QUIET,
    val isApneaSuspected: Boolean = false     // 是否存在疑似呼吸暂停/气流中断征兆
)
