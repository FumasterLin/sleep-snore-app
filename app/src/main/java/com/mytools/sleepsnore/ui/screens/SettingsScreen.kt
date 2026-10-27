package com.mytools.sleepsnore.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.backup.SleepDataBackupManager
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.ui.components.CalibrationDialog
import com.mytools.sleepsnore.ui.components.ProfileEditDialog
import com.mytools.sleepsnore.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { SleepDatabase.getInstance(context).sleepDao() }
    val prefs = remember { context.getSharedPreferences("snore_settings", Context.MODE_PRIVATE) }

    // 0. 主题模式
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "dark") ?: "dark") }

    // 1. 生理档案状态
    var gender by remember { mutableStateOf(prefs.getString("gender", "男") ?: "男") }
    var age by remember { mutableIntStateOf(prefs.getInt("age", 32)) }
    var heightCm by remember { mutableIntStateOf(prefs.getInt("height", 175)) }
    var weightKg by remember { mutableIntStateOf(prefs.getInt("weight", 72)) }
    var noseStatus by remember { mutableStateOf(prefs.getString("nose_status", "通畅") ?: "通畅") }
    var smokingStatus by remember { mutableStateOf(prefs.getString("smoking_status", "从不") ?: "从不") }
    var showProfileDialog by remember { mutableStateOf(false) }

    // 2. 算法与灵敏度
    var snoreSensitivity by remember { mutableStateOf(prefs.getString("snore_sensitivity", "normal") ?: "normal") }
    var snoreTriggerDb by remember { mutableFloatStateOf(prefs.getFloat("snore_trigger_db", 40.0f)) }
    var apneaSensitivity by remember { mutableStateOf(prefs.getString("apnea_sensitivity", "normal") ?: "normal") }
    var adaptiveNoiseFilter by remember { mutableStateOf(prefs.getBoolean("adaptive_noise_filter", true)) }
    var micGain by remember { mutableFloatStateOf(prefs.getFloat("mic_gain", 0.0f)) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    // 3. 录音与存储
    var audioMode by remember { mutableStateOf(prefs.getString("audio_mode", "smart") ?: "smart") }
    var clipMaxSec by remember { mutableIntStateOf(prefs.getInt("clip_max_duration", 15)) }
    var sampleRateKhz by remember { mutableIntStateOf(prefs.getInt("audio_sample_rate", 16000)) }
    var retentionDays by remember { mutableIntStateOf(prefs.getInt("retention_days", 30)) }
    var audioStorageMb by remember { mutableFloatStateOf(0.0f) }
    var audioSampleCount by remember { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 4. 防鼾微震动干预
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("anti_snore_vibration_enabled", true)) }
    var vibrationTriggerSec by remember { mutableIntStateOf(prefs.getInt("vibration_trigger_sec", 60)) }
    var vibrationIntensity by remember { mutableStateOf(prefs.getString("vibration_intensity", "medium") ?: "medium") }
    var vibrationGracePeriod by remember { mutableStateOf(prefs.getBoolean("vibration_grace_period_enabled", true)) }

    // 5. 夜视与屏幕
    var proximityScreenOff by remember { mutableStateOf(prefs.getBoolean("proximity_screen_off_enabled", true)) }
    var amoledUltraDark by remember { mutableStateOf(prefs.getBoolean("amoled_ultra_dark", true)) }
    var is24hClock by remember { mutableStateOf(prefs.getBoolean("clock_24h", true)) }
    var showLiveDbMeter by remember { mutableStateOf(prefs.getBoolean("show_live_db", true)) }

    // 6. 计划与目标
    var bedtimeReminderEnabled by remember { mutableStateOf(prefs.getBoolean("bedtime_reminder_enabled", false)) }
    var bedtimeReminderTime by remember { mutableStateOf(prefs.getString("bedtime_reminder_time", "23:00") ?: "23:00") }
    var targetSleepHours by remember { mutableFloatStateOf(prefs.getFloat("target_sleep_hours", 8.0f)) }
    var smartAlarmWindow by remember { mutableIntStateOf(prefs.getInt("smart_alarm_default_window", 20)) }

    // 计算音频文件占用
    fun refreshAudioStorage() {
        val audioDir = File(context.getExternalFilesDir(null), "snore_samples")
        if (audioDir.exists()) {
            val files = audioDir.walkTopDown().filter { it.isFile }.toList()
            val totalBytes = files.map { it.length() }.sum()
            audioStorageMb = totalBytes / (1024f * 1024f)
            audioSampleCount = files.size
        } else {
            audioStorageMb = 0f
            audioSampleCount = 0
        }
    }

    LaunchedEffect(Unit) {
        refreshAudioStorage()
    }

    // BMI 计算
    val bmi = remember(heightCm, weightKg) {
        val hM = heightCm / 100.0f
        if (hM > 0) weightKg / (hM * hM) else 22.0f
    }
    val bmiStatus = remember(bmi) {
        when {
            bmi < 18.5f -> Pair("偏瘦", Color(0xFF64B5F6))
            bmi < 24.0f -> Pair("正常", Color(0xFF4CAF50))
            bmi < 28.0f -> Pair("超重", Color(0xFFFF9800))
            else -> Pair("肥胖 (打鼾高危)", Color(0xFFF44336))
        }
    }

    // JSON 导入 Launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = SleepDataBackupManager.importJsonBackup(context, uri, dao)
                if (result.isSuccess) {
                    Toast.makeText(context, "成功恢复 ${result.getOrNull()} 条睡眠记录！", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "导入失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StandardBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = "设置与个人偏好 (Settings)",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "全量配置主题外观、检测算法、录音音质、微震动干预与睡眠目标",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // ==========================================
        // 0. 主题外观切换 (深色 / 亮色 / 跟随系统)
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎨 主题视觉外观风格", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("支持暗色与全新亮色模式无缝切换（夜视监测中自动保持防刺眼深空）", color = TextSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("dark", "🌙 深色模式", "经典夜间"),
                            Triple("light", "☀️ 亮色模式", "清爽明亮"),
                            Triple("system", "📱 跟随系统", "自适应")
                        ).forEach { (mode, title, sub) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        themeMode = mode
                                        prefs.edit().putString("theme_mode", mode).apply()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. 身体档案与 BMI 指数卡片
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("睡眠生理档案与体征", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("$gender · $age 岁 · $heightCm cm · $weightKg kg", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { showProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("编辑档案", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BMI 指数: %.1f".format(bmi), color = bmiStatus.second, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            val riskText = when {
                                bmi >= 28.0f -> "颈部气道受压明显，打鼾与呼吸暂停风险极高"
                                bmi >= 24.0f -> "体重略微超标，建议侧睡以减少舌后坠"
                                else -> "处于健康标准范围，注意保持鼻腔通畅"
                            }
                            Text(riskText, color = TextSecondary, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bmiStatus.second.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(bmiStatus.first, color = bmiStatus.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. 打鼾算法与检测灵敏度配置卡片
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎚️ 核心算法与检测灵敏度", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 灵敏度分段按钮
                    Text("打鼾检测灵敏度档位:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("high" to "高 (灵敏度优先)", "normal" to "标准 (推荐)", "low" to "低 (防杂音误报)").forEach { (key, label) ->
                            val isSelected = snoreSensitivity == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        snoreSensitivity = key
                                        prefs.edit().putString("snore_sensitivity", key).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 打鼾起始分贝阈值滑块
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("打鼾起始识别分贝: ${snoreTriggerDb.toInt()} dB", color = TextPrimary, fontSize = 12.sp)
                        Text(if (snoreTriggerDb <= 38f) "极其微弱鼾声" else if (snoreTriggerDb >= 48f) "仅识别大声打鼾" else "常规标准", color = TextSecondary, fontSize = 11.sp)
                    }
                    Slider(
                        value = snoreTriggerDb,
                        onValueChange = {
                            snoreTriggerDb = it
                            prefs.edit().putFloat("snore_trigger_db", it).apply()
                        },
                        valueRange = 35f..55f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryAccent, activeTrackColor = PrimaryAccent)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 呼吸暂停判定敏感度
                    Text("疑似呼吸暂停 (Apnea) 静默时长判定:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("strict" to "严格 (10秒)", "normal" to "标准 (15秒)", "loose" to "宽松 (25秒)").forEach { (key, label) ->
                            val isSelected = apneaSensitivity == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        apneaSensitivity = key
                                        prefs.edit().putString("apnea_sensitivity", key).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 自适应底噪消除开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自适应环境底噪过滤", color = TextPrimary, fontSize = 13.sp)
                            Text("自动追踪滤除空调与风扇低频恒定杂音", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = adaptiveNoiseFilter,
                            onCheckedChange = {
                                adaptiveNoiseFilter = it
                                prefs.edit().putBoolean("adaptive_noise_filter", it).apply()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 麦克风校准入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .clickable { showCalibrationDialog = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🎙️ 麦克风输入校准与增益", color = TextPrimary, fontSize = 13.sp)
                            Text("当前增益补偿: ${if (micGain >= 0) "+" else ""}${micGain.toInt()} dB", color = TextSecondary, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }

        // ==========================================
        // 3. 录音切片与音频存储偏好
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎙️ 录音与音频存储偏好", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 录音模式
                    Text("默认夜间录音模式:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("smart" to "智能打鼾切片", "all" to "整夜全录", "none" to "仅统计不录音").forEach { (key, label) ->
                            val isSelected = audioMode == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        audioMode = key
                                        prefs.edit().putString("audio_mode", key).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 单个片段最长录制秒数
                    Text("单个切片最大时长:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10 to "10秒", 15 to "15秒(推荐)", 20 to "20秒", 30 to "30秒").forEach { (sec, label) ->
                            val isSelected = clipMaxSec == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        clipMaxSec = sec
                                        prefs.edit().putInt("clip_max_duration", sec).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 录音过期自动清理
                    Text("历史音频自动保留时长:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "15天", 30 to "30天", 90 to "90天", 365 to "永久").forEach { (days, label) ->
                            val isSelected = retentionDays == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryAccent else DarkSurface)
                                    .clickable {
                                        retentionDays = days
                                        prefs.edit().putInt("retention_days", days).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 当前音频存储占用与一键清理
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("已录制音频: $audioSampleCount 段", color = TextPrimary, fontSize = 12.sp)
                            Text("空间占用: %.1f MB".format(audioStorageMb), color = TextSecondary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("清理历史音频", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. 智能微震动防鼾干预
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("📳 智能微震动防鼾干预", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("持续剧烈打鼾触发轻微震动提醒侧卧翻身", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                prefs.edit().putBoolean("anti_snore_vibration_enabled", it).apply()
                            }
                        )
                    }

                    if (vibrationEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("持续大声打鼾触发等待时长:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30 to "30秒 (敏感)", 60 to "60秒 (推荐)", 90 to "90秒 (重度)").forEach { (sec, label) ->
                                val isSelected = vibrationTriggerSec == sec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkSurface)
                                        .clickable {
                                            vibrationTriggerSec = sec
                                            prefs.edit().putInt("vibration_trigger_sec", sec).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("微震动干预强度:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("light" to "轻柔提示 (80ms)", "medium" to "标准提醒 (150ms)", "strong" to "强力翻身 (300ms)").forEach { (key, label) ->
                                val isSelected = vibrationIntensity == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkSurface)
                                        .clickable {
                                            vibrationIntensity = key
                                            prefs.edit().putString("vibration_intensity", key).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 入睡保护期
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("入睡免打扰保护期", color = TextPrimary, fontSize = 13.sp)
                                Text("启动监测后前 30 分钟内不触发微震动，确保平静入睡", color = TextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = vibrationGracePeriod,
                                onCheckedChange = {
                                    vibrationGracePeriod = it
                                    prefs.edit().putBoolean("vibration_grace_period_enabled", it).apply()
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. 夜视模式与屏幕功耗
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌙 夜视模式与功耗省电", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 正面朝下息屏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("手机正面朝下自动息屏", color = TextPrimary, fontSize = 13.sp)
                            Text("感应桌面平放切入 AMOLED 纯黑极致省电，翻转即亮", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = proximityScreenOff,
                            onCheckedChange = {
                                proximityScreenOff = it
                                prefs.edit().putBoolean("proximity_screen_off_enabled", it).apply()
                            }
                        )
                    }

                    Divider(color = DarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // AMOLED 极暗模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("夜视界面 AMOLED 防刺眼保护", color = TextPrimary, fontSize = 13.sp)
                            Text("深色径向极暗星空，降低夜间刺眼感", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = amoledUltraDark,
                            onCheckedChange = {
                                amoledUltraDark = it
                                prefs.edit().putBoolean("amoled_ultra_dark", it).apply()
                            }
                        )
                    }

                    Divider(color = DarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // 24小时制显示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("月亮夜钟 24 小时制", color = TextPrimary, fontSize = 13.sp)
                            Text("开启为 23:45，关闭为 11:45 PM", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = is24hClock,
                            onCheckedChange = {
                                is24hClock = it
                                prefs.edit().putBoolean("clock_24h", it).apply()
                            }
                        )
                    }

                    Divider(color = DarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // 实时环境分贝仪表
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("夜视界面显示实时分贝浮标", color = TextPrimary, fontSize = 13.sp)
                            Text("在月亮下方直观展示当前麦克风分贝值", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = showLiveDbMeter,
                            onCheckedChange = {
                                showLiveDbMeter = it
                                prefs.edit().putBoolean("show_live_db", it).apply()
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 6. 就寝提醒与睡眠目标
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⏰ 就寝计划与睡眠目标", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 就寝提醒
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("每日就寝提醒", color = TextPrimary, fontSize = 13.sp)
                            Text("提醒准备上床并开启打鼾监测", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = bedtimeReminderEnabled,
                            onCheckedChange = {
                                bedtimeReminderEnabled = it
                                prefs.edit().putBoolean("bedtime_reminder_enabled", it).apply()
                            }
                        )
                    }

                    if (bedtimeReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("22:30", "23:00", "23:30", "00:00").forEach { time ->
                                val isSelected = bedtimeReminderTime == time
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkSurface)
                                        .clickable {
                                            bedtimeReminderTime = time
                                            prefs.edit().putString("bedtime_reminder_time", time).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = time,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 每日目标睡眠时长
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("每日目标睡眠时长: %.1f 小时".format(targetSleepHours), color = TextPrimary, fontSize = 12.sp)
                        Text(if (targetSleepHours >= 7.5f) "达成年人健康标准" else "偏短", color = TextSecondary, fontSize = 11.sp)
                    }
                    Slider(
                        value = targetSleepHours,
                        onValueChange = {
                            targetSleepHours = it
                            prefs.edit().putFloat("target_sleep_hours", it).apply()
                        },
                        valueRange = 6.0f..9.5f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = PrimaryAccent, activeTrackColor = PrimaryAccent)
                    )
                }
            }
        }

        // ==========================================
        // 7. 数据全量备份与跨平台迁移
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💾 数据全量备份与跨平台迁移", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("支持将全部历史打鼾、呼吸暂停、评分与备忘导出为标准 JSON 或 CSV，方便换机迁移与科研统计分析。", color = TextSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = SleepDataBackupManager.exportJsonBackup(context, dao)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "导出全量 JSON 备份"))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 JSON 备份", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = SleepDataBackupManager.exportCsvSummary(context, dao)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "导出 CSV 睡眠报表"))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📊 CSV 报表", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { importLauncher.launch("application/json") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text("📥 还原备份", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. 系统保活与电池优化
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔋 系统保活与后台运行保障", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("电池优化无限制白名单", color = TextPrimary, fontSize = 13.sp)
                            Text("避免手机睡眠锁屏时被系统杀后台导致监测中断", color = TextSecondary, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("前往设置", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 9. 打鼾医学科普折叠列表 (FAQ)
        // ==========================================
        item {
            FaqSection()
        }
    }

    // 生理档案编辑弹窗
    if (showProfileDialog) {
        ProfileEditDialog(
            initialGender = gender,
            initialAge = age,
            initialHeight = heightCm,
            initialWeight = weightKg,
            initialNoseStatus = noseStatus,
            initialSmoking = smokingStatus,
            onSave = { g, a, h, w, n, s ->
                gender = g
                age = a
                heightCm = h
                weightKg = w
                noseStatus = n
                smokingStatus = s
                prefs.edit()
                    .putString("gender", g)
                    .putInt("age", a)
                    .putInt("height", h)
                    .putInt("weight", w)
                    .putString("nose_status", n)
                    .putString("smoking_status", s)
                    .apply()
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    // 麦克风增益校准弹窗
    if (showCalibrationDialog) {
        CalibrationDialog(
            currentGainDb = micGain,
            onSaveGain = {
                micGain = it
                prefs.edit().putFloat("mic_gain", it).apply()
            },
            onDismiss = { showCalibrationDialog = false }
        )
    }

    // 清理全部历史音频二次确认弹窗
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认清理历史音频？", color = TextPrimary) },
            text = { Text("此操作将永久删除全部本地已录制的打鼾切片 WAV 文件，释放存储空间。睡眠评分与统计直方图将完好保留。", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val audioDir = File(context.getExternalFilesDir(null), "snore_samples")
                        if (audioDir.exists()) {
                            audioDir.listFiles()?.forEach { it.delete() }
                        }
                        refreshAudioStorage()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "历史音频已全部清空，空间已释放！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("确认删除", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消", color = TextPrimary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun FaqSection() {
    val faqList = listOf(
        "为什么人会打鼾？" to "睡眠时咽喉部肌肉松弛，上呼吸道软组织（软腭、悬雍垂、扁桃体、舌根）下垂塌陷致气道狭窄。气流高速通过狭窄处引发软组织剧烈共振，发出打鼾声。",
        "打鼾与睡眠呼吸暂停 (OSA) 的区别？" to "单纯性打鼾通常气流持续存在。而阻塞性睡眠呼吸暂停（OSA）是打鼾过程中气道完全塌陷闭塞，气流中断超过 10 秒并伴随血氧下降，晨起头晕口干、日间嗜睡，属于需就医的严重病症。",
        "各种止鼾用具的医学原理？" to "• 侧卧枕：避免重力导致舌后坠；\n• 通气鼻贴：撑开前鼻孔减少吸气负压；\n• 止鼾牙套：微前推下颌扩大咽腔横截面；\n• CPAP呼吸机：气流正压支撑气道，防止塌陷。",
        "Snore Score 评分是如何计算的？" to "采用 SnoreLab 经典二次加权算法：结合整夜 30 秒窗口的峰值与平均分贝，对安静(<40dB)、轻度(40-50dB)、中度(50-65dB)、重度(>65dB)分别施加二次非线性权重积分输出。"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📚 打鼾医学指南与常见问题 (FAQ)", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            faqList.forEach { (q, a) ->
                var expanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Q: $q", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = a,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
