package com.mytools.sleepsnore.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 智能音频采样：自动检测截取的最具代表性的打鼾音频片段
 */
@Entity(
    tableName = "audio_samples",
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
data class AudioSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestampMs: Long,
    val offsetSeconds: Long,
    val durationSeconds: Float,
    val peakDb: Float,
    val level: SnoreIntensity,
    val audioPath: String,
    val isFavorite: Boolean = false,
    val label: String = ""
)
