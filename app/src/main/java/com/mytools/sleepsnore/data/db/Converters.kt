package com.mytools.sleepsnore.data.db

import androidx.room.TypeConverter
import com.mytools.sleepsnore.data.model.SnoreIntensity

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun fromSnoreIntensity(value: SnoreIntensity): Int {
        return value.level
    }

    @TypeConverter
    fun toSnoreIntensity(value: Int): SnoreIntensity {
        return SnoreIntensity.fromLevel(value)
    }
}
