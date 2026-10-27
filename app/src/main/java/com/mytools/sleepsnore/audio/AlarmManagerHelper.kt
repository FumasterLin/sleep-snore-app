package com.mytools.sleepsnore.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.mytools.sleepsnore.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class AlarmSound(
    val id: String,
    val name: String,
    val description: String,
    @RawRes val soundResId: Int
)

data class AlarmConfig(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val wakeWindowMinutes: Int = 20, // 0: 准时, 15: 15分钟窗口, 20: 20分钟窗口, 30: 30分钟窗口
    val soundId: String = "time"
)

class AlarmManagerHelper(private val context: Context) {

    companion object {
        val ALL_SOUNDS = listOf(
            AlarmSound("time", "时光 (Morning Clock)", "渐进悠扬的清晨旋律", R.raw.alarm_time),
            AlarmSound("morning_breeze", "晨风 (Morning Breeze)", "如春风拂面的清爽和弦", R.raw.alarm_morning_breeze),
            AlarmSound("jazz", "柔和爵士 (Gentle Jazz)", "优雅舒缓的低音提琴与钢琴", R.raw.alarm_jazz),
            AlarmSound("snowflakes", "雪花轻灵 (Snowflakes)", "清澈空灵的水晶八音盒音色", R.raw.alarm_snowflakes),
            AlarmSound("bells", "晨钟风铃 (Soft Bells)", "微风吹拂的日式清脆风铃声", R.raw.alarm_bells)
        )
    }

    private var previewPlayer: MediaPlayer? = null
    private var alarmPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeInJob: Job? = null

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

    fun previewSound(@RawRes soundResId: Int) {
        stopPreview()
        try {
            previewPlayer = MediaPlayer.create(context, soundResId)?.apply {
                setVolume(0.8f, 0.8f)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        previewPlayer = null
    }

    fun triggerAlarm(soundId: String) {
        if (_isAlarmRinging.value) return
        stopPreview()

        val sound = ALL_SOUNDS.find { it.id == soundId } ?: ALL_SOUNDS[0]
        try {
            alarmPlayer = MediaPlayer.create(context, sound.soundResId)?.apply {
                isLooping = true
                setVolume(0.1f, 0.1f) // 从微弱音量开始
                start()
            }
            _isAlarmRinging.value = true

            // 30 秒内逐渐递增音量至 1.0 (温和唤醒)
            fadeInJob?.cancel()
            fadeInJob = scope.launch {
                for (i in 1..30) {
                    delay(1000)
                    val vol = (i / 30f).coerceIn(0.1f, 1.0f)
                    alarmPlayer?.setVolume(vol, vol)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissAlarm() {
        fadeInJob?.cancel()
        fadeInJob = null
        try {
            alarmPlayer?.stop()
            alarmPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        alarmPlayer = null
        _isAlarmRinging.value = false
    }

    fun snooze(soundId: String, minutes: Int = 5) {
        dismissAlarm()
        scope.launch {
            delay(minutes * 60 * 1000L)
            triggerAlarm(soundId)
        }
    }

    fun checkShouldTriggerSmartWake(config: AlarmConfig, currentDb: Float, noiseFloor: Float): Boolean {
        if (!config.enabled || _isAlarmRinging.value) return false

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.hour)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
        }

        val diffMs = target.timeInMillis - now.timeInMillis
        val diffMinutes = diffMs / (60 * 1000)

        // 1. 到达或超过目标设定时间，强制唤醒
        if (diffMinutes <= 0 && diffMinutes >= -15) {
            return true
        }

        // 2. 处于唤醒窗口期 (例如设定 7:30，窗口 20分钟，即 7:10 ~ 7:30)
        if (diffMinutes in 0..config.wakeWindowMinutes) {
            // 检测是否处于浅睡眠（有身体微动，分贝比底噪高出 5~15dB，但不是持续大声打鼾）
            val isLightSleepMovement = (currentDb in (noiseFloor + 4f)..(noiseFloor + 22f))
            if (isLightSleepMovement) {
                return true
            }
        }

        return false
    }

    fun release() {
        dismissAlarm()
        stopPreview()
        scope.cancel()
    }
}
