package com.mytools.sleepsnore.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.mytools.sleepsnore.MainActivity
import com.mytools.sleepsnore.R
import com.mytools.sleepsnore.audio.*
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class SleepTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioRecord: AudioRecord? = null
    private var isTracking = false

    private lateinit var database: SleepDatabase
    private var currentSessionId: Long = 0
    private var sessionStartTimeMs: Long = 0

    private lateinit var audioProcessor: AudioProcessor
    private var snoreDetector: SnoreDetector? = null
    private var smartRecorder: SmartAudioRecorder? = null

    data class LiveState(
        val isTracking: Boolean = false,
        val elapsedSeconds: Long = 0,
        val currentDb: Float = 0f,
        val currentLevel: SnoreIntensity = SnoreIntensity.QUIET,
        val noiseFloorDb: Float = 32f,
        val snoreScoreEstimate: Float = 0f,
        val sessionId: Long = 0
    )

    companion object {
        const val ACTION_START = "com.mytools.sleepsnore.action.START"
        const val ACTION_STOP = "com.mytools.sleepsnore.action.STOP"
        const val EXTRA_FACTORS = "extra_factors"
        const val EXTRA_REMEDIES = "extra_remedies"

        private const val NOTIFICATION_CHANNEL_ID = "sleep_tracking_channel"
        private const val NOTIFICATION_ID = 1001

        private val _liveState = MutableStateFlow(LiveState())
        val liveState: StateFlow<LiveState> = _liveState.asStateFlow()

        fun startTracking(context: Context, factorIds: List<String>, remedyIds: List<String>) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_START
                putStringArrayListExtra(EXTRA_FACTORS, ArrayList(factorIds))
                putStringArrayListExtra(EXTRA_REMEDIES, ArrayList(remedyIds))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = SleepDatabase.getInstance(this)
        audioProcessor = AudioProcessor()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val factors = intent.getStringArrayListExtra(EXTRA_FACTORS) ?: arrayListOf()
                val remedies = intent.getStringArrayListExtra(EXTRA_REMEDIES) ?: arrayListOf()
                if (!isTracking) {
                    startForegroundTracking(factors, remedies)
                }
            }
            ACTION_STOP -> {
                if (isTracking) {
                    stopTrackingInternal()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundTracking(factors: List<String>, remedies: List<String>) {
        isTracking = true
        sessionStartTimeMs = System.currentTimeMillis()

        // 1. 启动前台通知
        val notification = createNotification("正在准备睡眠监测...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. 申请 Partial WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepSnoreDetector:WakeLock").apply {
            acquire(12 * 3600 * 1000L) // 最大持有 12 小时
        }

        // 3. 在数据库中创建 Session
        serviceScope.launch {
            val session = SleepSession(
                startTime = sessionStartTimeMs,
                factorIds = factors,
                remedyIds = remedies
            )
            currentSessionId = database.sleepDao().insertSession(session)

            snoreDetector = SnoreDetector(currentSessionId, sessionStartTimeMs)
            smartRecorder = SmartAudioRecorder(this@SleepTrackingService, currentSessionId)

            startAudioRecordingLoop()
        }
    }

    private fun startAudioRecordingLoop() {
        serviceScope.launch(Dispatchers.IO) {
            val minBufSize = AudioRecord.getMinBufferSize(
                AudioConfig.SAMPLE_RATE,
                AudioConfig.CHANNEL_CONFIG,
                AudioConfig.AUDIO_FORMAT
            )
            val bufferSize = max(minBufSize, AudioConfig.FRAME_SIZE * 4)

            try {
                audioRecord = AudioRecord(
                    AudioConfig.AUDIO_SOURCE,
                    AudioConfig.SAMPLE_RATE,
                    AudioConfig.CHANNEL_CONFIG,
                    AudioConfig.AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    stopTrackingInternal()
                    return@launch
                }

                audioRecord?.startRecording()
                val pcmBuffer = ShortArray(AudioConfig.FRAME_SIZE)

                var lastNotificationUpdate = 0L

                while (isTracking && isActive) {
                    val readSamples = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: -1
                    if (readSamples > 0) {
                        val detector = snoreDetector ?: continue
                        val recorder = smartRecorder ?: continue

                        // 分析当前帧
                        val analysis = audioProcessor.processFrame(pcmBuffer, detector.noiseFloorDb)
                        val elapsedSec = (System.currentTimeMillis() - sessionStartTimeMs) / 1000

                        // 30秒 Epoch 聚合
                        val epochPoint = detector.processFrame(analysis)
                        if (epochPoint != null) {
                            launch(Dispatchers.IO) {
                                database.sleepDao().insertChartPoint(epochPoint)
                            }
                        }

                        // 智能音频录制触发
                        val audioSample = recorder.onFrame(pcmBuffer, analysis, elapsedSec)
                        if (audioSample != null) {
                            launch(Dispatchers.IO) {
                                database.sleepDao().insertAudioSample(audioSample)
                            }
                        }

                        // 更新状态流
                        val currentLevel = if (analysis.isSnoringCandidate) {
                            SnoreIntensity.fromDb(analysis.db)
                        } else {
                            SnoreIntensity.QUIET
                        }

                        _liveState.value = LiveState(
                            isTracking = true,
                            elapsedSeconds = elapsedSec,
                            currentDb = analysis.db,
                            currentLevel = currentLevel,
                            noiseFloorDb = detector.noiseFloorDb,
                            snoreScoreEstimate = detector.calculateSnoreScore(elapsedSec),
                            sessionId = currentSessionId
                        )

                        // 智能防鼾微震动干预 (根据设置读取开关、阈值、强度及入睡保护)
                        val prefs = getSharedPreferences("snore_settings", Context.MODE_PRIVATE)
                        val vibrationEnabled = prefs.getBoolean("anti_snore_vibration_enabled", true)
                        val triggerSec = prefs.getInt("vibration_trigger_sec", 60)
                        val gracePeriod = prefs.getBoolean("vibration_grace_period_enabled", true)
                        val inGrace = gracePeriod && elapsedSec < 1800 // 前 30 分钟入睡保护

                        if (vibrationEnabled && !inGrace && detector.continuousEpicSeconds >= triggerSec.toFloat()) {
                            val intensity = prefs.getString("vibration_intensity", "medium") ?: "medium"
                            triggerSnoreInterventionVibration(intensity)
                        }

                        // 周期刷新通知 (每 5 秒一次)
                        if (System.currentTimeMillis() - lastNotificationUpdate > 5000) {
                            lastNotificationUpdate = System.currentTimeMillis()
                            val notifText = "分贝: ${analysis.db.toInt()}dB | 状态: ${currentLevel.title} | 时长: ${formatElapsed(elapsedSec)}"
                            updateNotification(notifText)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cleanUpAudio()
            }
        }
    }

    private fun stopTrackingInternal() {
        isTracking = false
        val endTimeMs = System.currentTimeMillis()
        val totalSec = max(1L, (endTimeMs - sessionStartTimeMs) / 1000)

        val detector = snoreDetector
        if (detector != null && currentSessionId > 0) {
            val snoreScore = detector.calculateSnoreScore(totalSec)
            val mildSec = detector.totalMildSeconds
            val loudSec = detector.totalLoudSeconds
            val epicSec = detector.totalEpicSeconds
            val snoreTotalSec = (mildSec + loudSec + epicSec).toLong()

            val mildPct = (mildSec / totalSec).coerceIn(0f, 1f)
            val loudPct = (loudSec / totalSec).coerceIn(0f, 1f)
            val epicPct = (epicSec / totalSec).coerceIn(0f, 1f)
            val quietPct = (1f - (mildPct + loudPct + epicPct)).coerceIn(0f, 1f)

            serviceScope.launch(Dispatchers.IO) {
                val existingSession = database.sleepDao().getSessionById(currentSessionId)
                if (existingSession != null) {
                    val updated = existingSession.copy(
                        endTime = endTimeMs,
                        sessionDurationSeconds = totalSec,
                        snoringDurationSeconds = snoreTotalSec,
                        snoreScore = snoreScore,
                        quietPercent = quietPct,
                        mildPercent = mildPct,
                        loudPercent = loudPct,
                        epicPercent = epicPct,
                        minVolumeDb = detector.minSessionDb,
                        maxVolumeDb = detector.maxSessionDb,
                        avgVolumeDb = detector.getAverageDb(),
                        apneaCount = detector.totalApneaEvents,
                        estimatedAhi = detector.calculateEstimatedAhi(totalSec),
                        palatalPercent = detector.getPalatalPercent(),
                        tongueBasePercent = detector.getTongueBasePercent(),
                        gaspPercent = detector.getGaspPercent()
                    )
                    database.sleepDao().updateSession(updated)
                    com.mytools.sleepsnore.widget.SleepAppWidgetProvider.updateAllWidgets(this@SleepTrackingService)
                }
            }
        }

        cleanUpAudio()
        _liveState.value = LiveState(isTracking = false, sessionId = currentSessionId)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanUpAudio() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // ignore
        }
        smartRecorder?.release()
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "睡眠监测服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "在睡眠期间后台持续进行音频与打鼾分析"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    private fun formatElapsed(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private var lastVibrateInterventionMs = 0L
    private fun triggerSnoreInterventionVibration(intensity: String = "medium") {
        val now = System.currentTimeMillis()
        if (now - lastVibrateInterventionMs < 120_000L) return // 最少间隔 2 分钟，避免频繁干扰
        lastVibrateInterventionMs = now
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            val durationMs = when (intensity) {
                "light" -> 80L
                "strong" -> 300L
                else -> 150L
            }
            val amplitude = when (intensity) {
                "light" -> 60
                "strong" -> 160
                else -> 100
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        isTracking = false
        cleanUpAudio()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
