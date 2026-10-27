package com.mytools.sleepsnore.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.mytools.sleepsnore.R
import com.mytools.sleepsnore.audio.AlarmConfig
import com.mytools.sleepsnore.audio.AlarmManagerHelper
import com.mytools.sleepsnore.audio.SoundscapeManager
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.data.model.SleepInfluenceItem
import com.mytools.sleepsnore.service.SleepTrackingService
import com.mytools.sleepsnore.ui.components.*
import com.mytools.sleepsnore.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun RecordScreen(
    soundscapeManager: SoundscapeManager,
    alarmHelper: AlarmManagerHelper,
    selectedFactors: List<String>,
    onFactorsChanged: (List<String>) -> Unit,
    selectedRemedies: List<String>,
    onRemediesChanged: (List<String>) -> Unit,
    onNavigateToResult: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { SleepDatabase.getInstance(context).sleepDao() }
    val liveState by SleepTrackingService.liveState.collectAsState()

    var showDelayDialog by remember { mutableStateOf(false) }
    var showAudioModeDialog by remember { mutableStateOf(false) }
    var showRemediesDialog by remember { mutableStateOf(false) }
    var showFactorsDialog by remember { mutableStateOf(false) }
    var showSoundscapeDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }

    var showRestRatingDialog by remember { mutableStateOf(false) }
    var stoppedSessionId by remember { mutableStateOf(0L) }

    var delayMinutes by remember { mutableStateOf(20) }
    var audioMode by remember { mutableStateOf("smart") } // smart, all, none
    var alarmConfig by remember { mutableStateOf(AlarmConfig(enabled = false)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerVibration(context)
            SleepTrackingService.startTracking(context, selectedFactors, selectedRemedies)
        }
    }

    // 智能浅睡轻柔闹钟唤醒循环监测
    LaunchedEffect(liveState.isTracking, alarmConfig) {
        while (liveState.isTracking && alarmConfig.enabled) {
            delay(10000)
            if (alarmHelper.checkShouldTriggerSmartWake(alarmConfig, liveState.currentDb, liveState.noiseFloorDb)) {
                alarmHelper.triggerAlarm(alarmConfig.soundId)
                break
            }
        }
    }

    if (liveState.isTracking) {
        // SnoreLab 官方 1:1 全屏夜视月亮星空监测界面 (Night View)
        NightViewScreen(
            liveState = liveState,
            delayMinutes = delayMinutes,
            onStop = {
                val sessionId = liveState.sessionId
                SleepTrackingService.stopTracking(context)
                if (sessionId > 0) {
                    stoppedSessionId = sessionId
                    showRestRatingDialog = true
                }
            }
        )
    } else {
        // SnoreLab 官方 1:1 主监测配置屏 (Record Screen)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StandardBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶栏：标题 + 辅助功能快捷入口 (声音景观、智能闹钟)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "监测",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    val todayStr = remember {
                        SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(Date())
                    }
                    Text(text = todayStr, color = TextSecondary, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showSoundscapeDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LighterBackground)
                    ) {
                        Icon(
                            Icons.Default.NaturePeople,
                            contentDescription = "助眠声景",
                            tint = BlueHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showAlarmDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LighterBackground)
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = "智能闹钟",
                            tint = if (alarmConfig.enabled) BlueHighlight else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 官方顶部状态药丸 (HomeScreenPill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(LighterBackground)
                    .border(0.5.dp, DialOuterCircle, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.StayCurrentPortrait,
                        contentDescription = null,
                        tint = BlueHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "就寝时请将手机正面朝下放在床边柜上",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 核心 2x2 宫格大功能卡片 (SnoreLab Four Large Cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 第一行：入睡时间 (Delay) + 录音模式 (Mode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 1 (左上)：延迟开始 (TimeToSleep)
                    LargeHomeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HourglassEmpty,
                        title = "延迟开始",
                        value = if (delayMinutes == 0) "即刻开始" else "$delayMinutes 分钟",
                        subtitle = "留出安稳入睡时间",
                        onClick = { showDelayDialog = true }
                    )

                    // 卡片 2 (右上)：录音选项 (Results / Audio Mode)
                    LargeHomeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GraphicEq,
                        title = "录音模式",
                        value = when (audioMode) {
                            "all" -> "整夜录音"
                            "none" -> "仅统计数据"
                            else -> "智能切片"
                        },
                        subtitle = "高质量音频切片采样",
                        onClick = { showAudioModeDialog = true }
                    )
                }

                // 第二行：止鼾对策 (Remedies) + 影响因素 (Factors)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 3 (左下)：止鼾对策 (Remedies)
                    val remedyText = if (selectedRemedies.isEmpty()) "未选择" else "已选 ${selectedRemedies.size} 项"
                    val remedySub = if (selectedRemedies.isNotEmpty()) {
                        val firstTitle = SleepInfluenceItem.ALL_REMEDIES.find { it.id == selectedRemedies.first() }?.title ?: ""
                        if (selectedRemedies.size > 1) "$firstTitle 等" else firstTitle
                    } else "测试对策改善效果"

                    LargeHomeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MedicalServices,
                        title = "止鼾对策",
                        value = remedyText,
                        subtitle = remedySub,
                        onClick = { showRemediesDialog = true }
                    )

                    // 卡片 4 (右下)：影响因素 (Factors)
                    val factorText = if (selectedFactors.isEmpty()) "未选择" else "已选 ${selectedFactors.size} 项"
                    val factorSub = if (selectedFactors.isNotEmpty()) {
                        val firstTitle = SleepInfluenceItem.ALL_FACTORS.find { it.id == selectedFactors.first() }?.title ?: ""
                        if (selectedFactors.size > 1) "$firstTitle 等" else firstTitle
                    } else "记录睡前生活习惯"

                    LargeHomeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalBar,
                        title = "影响因素",
                        value = factorText,
                        subtitle = factorSub,
                        onClick = { showFactorsDialog = true }
                    )
                }
            }

            // 比例优化：拉开配置卡片与触发按钮的纵深间距，让 START 按钮平稳下移至视觉黄金操作区
            Spacer(modifier = Modifier.height(48.dp))

            // 4. 底部中央大号醒目发光 START 按钮
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // 外层径向呼吸光晕
                Box(
                    modifier = Modifier
                        .size(146.dp * glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BlueHighlight.copy(alpha = 0.26f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 核心大圆按钮 (114dp 黄金比例与质感)
                Surface(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .clickable {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasMic) {
                                triggerVibration(context)
                                SleepTrackingService.startTracking(context, selectedFactors, selectedRemedies)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(BlueHighlight, BlueButton)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "开始",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "START",
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. 底部温馨提示
            Text(
                text = "请连接充电器以保证整夜电量，麦克风朝向床位",
                color = TextSecondary.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // 晨起精力打卡与备忘弹窗
    if (showRestRatingDialog && stoppedSessionId > 0) {
        RestRatingDialog(
            onConfirm = { rating, notes ->
                coroutineScope.launch {
                    dao.updateSessionRatingAndNotes(stoppedSessionId, rating, notes)
                    showRestRatingDialog = false
                    onNavigateToResult(stoppedSessionId)
                }
            },
            onDismiss = {
                showRestRatingDialog = false
                onNavigateToResult(stoppedSessionId)
            }
        )
    }

    // 各项配置弹窗
    if (showDelayDialog) {
        DelayTimerDialog(
            currentDelayMinutes = delayMinutes,
            onSelectDelay = { delayMinutes = it },
            onDismiss = { showDelayDialog = false }
        )
    }

    if (showAudioModeDialog) {
        AudioModeDialog(
            currentMode = audioMode,
            onSelectMode = { audioMode = it },
            onDismiss = { showAudioModeDialog = false }
        )
    }

    if (showRemediesDialog) {
        RemediesSelectionDialog(
            selectedRemedies = selectedRemedies,
            onSave = onRemediesChanged,
            onDismiss = { showRemediesDialog = false }
        )
    }

    if (showFactorsDialog) {
        FactorsSelectionDialog(
            selectedFactors = selectedFactors,
            onSave = onFactorsChanged,
            onDismiss = { showFactorsDialog = false }
        )
    }

    if (showSoundscapeDialog) {
        SoundscapeDialog(
            soundscapeManager = soundscapeManager,
            onDismiss = { showSoundscapeDialog = false }
        )
    }

    if (showAlarmDialog) {
        SmartAlarmDialog(
            alarmConfig = alarmConfig,
            alarmHelper = alarmHelper,
            onSaveConfig = { alarmConfig = it },
            onDismiss = { showAlarmDialog = false }
        )
    }

    // 智能闹钟响铃唤醒弹窗
    val isAlarmRinging by alarmHelper.isAlarmRinging.collectAsState()
    if (isAlarmRinging) {
        Dialog(onDismissRequest = { alarmHelper.dismissAlarm() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val ringTransition = rememberInfiniteTransition(label = "alarm_ring")
                    val ringScale by ringTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ringScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PrimaryAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = "闹钟",
                            tint = PrimaryAccent,
                            modifier = Modifier.size((36 * ringScale).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("⏰ 浅睡期轻柔唤醒中", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("系统检测到您已进入浅睡阶段，正以渐进低音量唤醒您", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { alarmHelper.snooze(alarmConfig.soundId, 5) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("小睡 5分钟", color = TextPrimary)
                        }

                        Button(
                            onClick = { alarmHelper.dismissAlarm() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("起床关闭", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * SnoreLab 官方主屏 2x2 大卡片 (黄金比例优化版：放大卡片面积、提升文字与图标质感)
 */
@Composable
private fun LargeHomeCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(152.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LighterBackground),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, DialOuterCircle.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkerBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = BlueHighlight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * SnoreLab 官方 1:1 全屏夜视月亮星空监测页面 (Night View)
 * 升级版：星空动态粒子系统 + 传感器正面朝下超低功耗自动息屏 + 唤醒检测
 */
@Composable
fun NightViewScreen(
    liveState: SleepTrackingService.LiveState,
    delayMinutes: Int,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    var isPaused by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isFaceDown by remember { mutableStateOf(false) }

    // 传感器硬件接入：监听距离传感器与加速度计 (检测手机正面朝下放在床头柜上)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var proximityNear = false
        var zAxisNegative = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val maxRange = event.sensor.maximumRange
                    proximityNear = event.values[0] < maxRange && event.values[0] <= 2.0f
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val z = event.values[2]
                    zAxisNegative = z < -6.5f // 屏幕正面朝下
                }
                val prefs = context.getSharedPreferences("snore_settings", Context.MODE_PRIVATE)
                val proximityEnabled = prefs.getBoolean("proximity_screen_off_enabled", true)
                isFaceDown = proximityEnabled && (proximityNear || zAxisNegative)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        proximitySensor?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelSensor?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // 切换手电筒
    fun toggleTorch() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
            }
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isTorchOn) {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                    if (cameraManager != null && cameraId != null) {
                        cameraManager.setTorchMode(cameraId, false)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // 动态时间走动
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeStr = sdf.format(Date())
            delay(1000)
        }
    }

    // 如果手机正面朝下平放在桌面：激活纯黑 AMOLED 息屏超低功耗模式
    if (isFaceDown) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "正面朝下监听中 · 屏幕已自动熄屏省电",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "拿起或翻转手机自动唤醒夜视月亮时钟",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 10.sp
                )
            }
        }
        return
    }

    // 官方夜视星空径向背景 (night_view.xml)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(NightCenter, NightCorner),
                    center = Offset(Float.POSITIVE_INFINITY / 2, 800f),
                    radius = 900f
                )
            )
    ) {
        // 动态微光星空微粒系统 (Stars Particle Background)
        StarsBackground(modifier = Modifier.fillMaxSize())

        // 顶部控制条：暂停监测 / 手电筒开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 暂停/恢复 监测按钮 (Button.Transparent.NightView)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { isPaused = !isPaused }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isPaused) "恢复监测 (RESUME)" else "暂停监测 (PAUSE)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 手电筒开关
            IconButton(
                onClick = { toggleTorch() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isTorchOn) BlueHighlight else Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    Icons.Default.FlashlightOn,
                    contentDescription = "手电筒",
                    tint = if (isTorchOn) Color.Black else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 中心月亮容器 (Moon Container, 对应官方 FrameLayout @id/moon_container, 300dp)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // 官方真实月球纹理图像 (drawable/moon.png, alpha=0.68)
                Image(
                    painter = painterResource(id = R.drawable.moon),
                    contentDescription = "Moon",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.68f),
                    contentScale = ContentScale.Fit
                )

                // 月亮内部文字与超大数字时间
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GOOD NIGHT",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif
                    )

                    val elapsedH = liveState.elapsedSeconds / 3600
                    val elapsedM = (liveState.elapsedSeconds % 3600) / 60
                    Text(
                        text = "已监测 %02d:%02d".format(elapsedH, elapsedM),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 月亮下方提示与声波指示
            val isDelaying = delayMinutes > 0 && liveState.elapsedSeconds < delayMinutes * 60L
            if (isDelaying) {
                val remainMins = delayMinutes - (liveState.elapsedSeconds / 60).toInt()
                Text(
                    text = "将在 $remainMins 分钟后开始记录声音",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "请将手机正面朝下",
                    color = BlueHighlight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 实时环境分贝指示
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (liveState.currentLevel) {
                                    com.mytools.sleepsnore.data.model.SnoreIntensity.QUIET -> SnoreQuiet
                                    com.mytools.sleepsnore.data.model.SnoreIntensity.MILD -> SnoreLight
                                    com.mytools.sleepsnore.data.model.SnoreIntensity.LOUD -> SnoreLoud
                                    com.mytools.sleepsnore.data.model.SnoreIntensity.EPIC -> SnoreEpic
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "实时环境: ${liveState.currentDb.toInt()} dB",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 底部夜间潮汐微光水波
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
        ) {
            FluidWaveVisualizer(
                modifier = Modifier.fillMaxSize(),
                amplitudeMultiplier = 0.8f,
                baseColor = Color(0xFF1565C0)
            )
        }

        // 底部防误触左右滑动停止栏 (Slide to Stop)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp)
        ) {
            SlideToStopBar(onStop = {
                triggerVibration(context)
                onStop()
            })
        }
    }
}

/**
 * 官方 1:1 防误触滑动停止条 (Slide to Stop)
 */
@Composable
private fun SlideToStopBar(onStop: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxDragDistance = 240.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxPx = with(density) { maxDragDistance.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(29.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        // 居中闪烁提示文案
        Text(
            text = "滑动以结束监测  ›››",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // 拖动滑块
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(4.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(BlueHighlight)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX >= maxPx * 0.75f) {
                                onStop()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxPx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "滑动停止",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun triggerVibration(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    } catch (_: Exception) {}
}
