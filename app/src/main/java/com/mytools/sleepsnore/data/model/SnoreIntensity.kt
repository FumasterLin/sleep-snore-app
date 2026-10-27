package com.mytools.sleepsnore.data.model

/**
 * SnoreLab 官方打鼾四分级标准与原版配色
 * 对应官方 values/colors.xml (quiet, mild, loud, epic)
 * 对应官方 values-zh-rCN/strings.xml (安静, 轻声, 大声, 超大声)
 */
enum class SnoreIntensity(
    val level: Int,
    val title: String,
    val minDb: Float,
    val colorHex: Long
) {
    QUIET(0, "安静", 0f, 0xFF344E59),
    MILD(1, "轻声", 40f, 0xFF488E83),
    LOUD(2, "大声", 50f, 0xFFC9A36E),
    EPIC(3, "超大声", 65f, 0xFFD1693C);

    val label: String get() = title
    val desc: String get() = when (this) {
        QUIET -> "<40dB 安静"
        MILD -> "40-50dB 轻声"
        LOUD -> "50-65dB 大声"
        EPIC -> ">65dB 超大声"
    }

    companion object {
        fun fromLevel(level: Int): SnoreIntensity {
            return entries.find { it.level == level } ?: QUIET
        }

        fun fromDb(db: Float): SnoreIntensity {
            return when {
                db >= EPIC.minDb -> EPIC
                db >= LOUD.minDb -> LOUD
                db >= MILD.minDb -> MILD
                else -> QUIET
            }
        }

        fun fromScore(score: Float): SnoreIntensity {
            return when {
                score >= 50f -> EPIC
                score >= 25f -> LOUD
                score >= 10f -> MILD
                else -> QUIET
            }
        }
    }
}
