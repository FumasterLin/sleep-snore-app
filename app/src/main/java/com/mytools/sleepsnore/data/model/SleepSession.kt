package com.mytools.sleepsnore.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,                      // 启动时间戳 (毫秒)
    val endTime: Long = 0,                    // 结束时间戳 (毫秒)
    val sessionDurationSeconds: Long = 0,     // 监测总时长 (秒)
    val snoringDurationSeconds: Long = 0,     // 打鼾总时长 (秒)
    val snoreScore: Float = 0f,               // 核心打鼾评分 (0 ~ 150+)
    val quietPercent: Float = 1.0f,           // 安静时长比例 (0.0 ~ 1.0)
    val mildPercent: Float = 0f,              // 轻度打鼾时长比例
    val loudPercent: Float = 0f,              // 中度打鼾时长比例
    val epicPercent: Float = 0f,              // 重度打鼾时长比例
    val minVolumeDb: Float = 0f,              // 最小环境分贝
    val maxVolumeDb: Float = 0f,              // 峰值打鼾分贝
    val avgVolumeDb: Float = 0f,              // 平均分贝
    val factorIds: List<String> = emptyList(),// 睡前诱因（饮酒、晚饭过饱等）
    val remedyIds: List<String> = emptyList(),// 止鼾对策（侧睡、鼻贴等）
    val restRating: Int = 0,                  // 晨起精力评分 (1~5星)
    val notes: String = "",                   // 睡后备忘与感受
    val apneaCount: Int = 0,                  // 疑似睡眠呼吸暂停/气流中断事件次数
    val estimatedAhi: Float = 0f,             // 预估 AHI (呼吸暂停低通气指数 = apneaCount / 小时数)
    val palatalPercent: Float = 0f,           // 软腭/悬雍垂震颤占比 (350~850Hz)
    val tongueBasePercent: Float = 0f,        // 舌后坠深喉打鼾占比 (100~320Hz)
    val gaspPercent: Float = 0f               // 呛咳/突发气流喘鸣占比 (>950Hz)
)
