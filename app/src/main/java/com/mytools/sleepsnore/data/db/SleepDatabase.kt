package com.mytools.sleepsnore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SleepSession

@Database(
    entities = [SleepSession::class, ChartPoint::class, AudioSample::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null

        fun getInstance(context: Context): SleepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_snore.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
