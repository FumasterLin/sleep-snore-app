package com.mytools.sleepsnore

import android.app.Application
import com.mytools.sleepsnore.data.db.SleepDatabase

class SleepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 预热初始化数据库
        SleepDatabase.getInstance(this)
    }
}
