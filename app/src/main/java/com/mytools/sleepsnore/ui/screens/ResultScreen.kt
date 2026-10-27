package com.mytools.sleepsnore.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SleepInfluenceItem
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.report.PdfReportGenerator
import com.mytools.sleepsnore.ui.components.AudioPlayerBar
import com.mytools.sleepsnore.ui.components.AudioTrimmerDialog
import com.mytools.sleepsnore.ui.components.RestRatingDialog
import com.mytools.sleepsnore.ui.components.ScoreRoundChart
import com.mytools.sleepsnore.ui.components.SleepRadarChart
import com.mytools.sleepsnore.ui.components.TimelineChart
import com.mytools.sleepsnore.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    sessionId: Long = -1L,
    onBack: (() -> Unit)? = null,
    onNavigateToRecord: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { SleepDatabase.getInstance(context).sleepDao() }

    var currentSessionId by remember(sessionId) { mutableStateOf(sessionId) }
    var session by remember { mutableStateOf<SleepSession?>(null) }
    val allSessions by dao.getAllSessions().collectAsState(initial = emptyList())

    var showSessionsDialog by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showEditRestDialog by remember { mutableStateOf(false) }
    var sampleToTrim by remember { mutableStateOf<AudioSample?>(null) }

    LaunchedEffect(currentSessionId, allSessions) {
        if (currentSessionId > 0) {
            session = dao.getSessionById(currentSessionId)
        } else if (allSessions.isNotEmpty()) {
            session = allSessions.first()
            currentSessionId = session?.id ?: -1L
        }
    }

    val chartPoints by dao.getChartPointsBySession(currentSessionId).collectAsState(initial = emptyList())
    val audioSamples by dao.getAudioSamplesBySession(currentSessionId).collectAsState(initial = emptyList())

    var activeAudioSample by remember { mutableStateOf<AudioSample?>(null) }
    var selectedChartPoint by remember { mutableStateOf<ChartPoint?>(null) }

    val s = session
    if (s == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StandardBackground)
                .padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无睡眠分析报告", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("请前往「监测」主页，点击 START 开启今夜监测。", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onNavigateToRecord,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("前往睡眠监测", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // 寻找前一夜和后一夜 Session
    val currentIndex = remember(allSessions, s) { allSessions.indexOfFirst { it.id == s.id } }
    val hasPrev = currentIndex >= 0 && currentIndex < allSessions.size - 1
    val hasNext = currentIndex > 0

    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (hasPrev) {
                                    currentSessionId = allSessions[currentIndex + 1].id
                                    activeAudioSample = null
                                    selectedChartPoint = null
                                }
                            },
                            enabled = hasPrev
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "前一夜",
                                tint = if (hasPrev) TextPrimary else TextSecondary.copy(alpha = 0.25f)
                            )
                        }

                        Text(
                            text = dateFormatter.format(Date(s.startTime)),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = {
                                if (hasNext) {
                                    currentSessionId = allSessions[currentIndex - 1].id
                                    activeAudioSample = null
                                    selectedChartPoint = null
                                }
                            },
                            enabled = hasNext
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "后一夜",
                                tint = if (hasNext) TextPrimary else TextSecondary.copy(alpha = 0.25f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showSessionsDialog = true }) {
                        Icon(Icons.Default.List, contentDescription = "历史列表", tint = BlueHighlight)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "分享报告", tint = TextSecondary)
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false },
                            modifier = Modifier.background(LighterBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("📤 分享报告摘要文本", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showShareMenu = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "我在 SnoreLab 测得昨晚打鼾评分为 ${s.snoreScore.toInt()}分，总卧床 ${formatDuration(s.sessionDurationSeconds)}，打鼾时长 ${formatDuration(s.snoringDurationSeconds)}，疑似呼吸暂停 ${s.apneaCount} 次。"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "分享睡眠报告"))
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("📄 导出医生医学诊断报告 (PDF)", color = BlueHighlight, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showShareMenu = false
                                    Toast.makeText(context, "正在生成医学级睡眠 PDF 报告...", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        PdfReportGenerator.generateAndShareReport(context, s, chartPoints)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StandardBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = StandardBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 上半部：时间轴直方图 (SnoreGraphView - 支持双指缩放与光标定位)
            item {
                TimelineChart(
                    points = chartPoints,
                    startTimeMs = s.startTime,
                    endTimeMs = if (s.endTime > 0) s.endTime else s.startTime + s.sessionDurationSeconds * 1000L,
                    selectedPoint = selectedChartPoint,
                    onPointSelected = { pt ->
                        selectedChartPoint = pt
                        val matched = audioSamples.minByOrNull { kotlin.math.abs(it.timestampMs - pt.timestampMs) }
                        if (matched != null) {
                            activeAudioSample = matched
                        }
                    }
                )
            }

            // 2. 音频波形播放器条 (当有选中的样本时展开)
            if (activeAudioSample != null) {
                item {
                    AudioPlayerBar(
                        sample = activeAudioSample,
                        onClose = { activeAudioSample = null },
                        onToggleFavorite = { sample ->
                            coroutineScope.launch {
                                dao.setSampleFavorite(sample.id, !sample.isFavorite)
                                activeAudioSample = sample.copy(isFavorite = !sample.isFavorite)
                            }
                        }
                    )
                }
            } else if (audioSamples.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LighterBackground)
                            .clickable { activeAudioSample = audioSamples.first() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = BlueHighlight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("已录制 ${audioSamples.size} 条打鼾音频切片，点击播放", color = TextPrimary, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BlueHighlight, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 3. 下半部：50% : 50% 核心指标与打鼾评分速度表 (statistics_details_middle_view)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧 50%：三行核心指标
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatMetricRow(
                                icon = Icons.Default.Alarm,
                                title = "开始 / 结束",
                                value = "${timeFormatter.format(Date(s.startTime))} - ${if (s.endTime > 0) timeFormatter.format(Date(s.endTime)) else "监测中"}"
                            )

                            StatMetricRow(
                                icon = Icons.Default.Bedtime,
                                title = "在床时间",
                                value = formatDuration(s.sessionDurationSeconds)
                            )

                            val snorePct = if (s.sessionDurationSeconds > 0) {
                                (s.snoringDurationSeconds * 100f / s.sessionDurationSeconds).toInt()
                            } else 0
                            StatMetricRow(
                                icon = Icons.Default.GraphicEq,
                                title = "打鼾时间",
                                value = "${formatDuration(s.snoringDurationSeconds)} ($snorePct%)"
                            )
                        }

                        // 分割垂直线
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(130.dp)
                                .background(DialOuterCircle)
                        )

                        // 右侧 50%：经典 270° 弧形打鼾评分速度表 (ScoreRoundChart)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ScoreRoundChart(
                                score = s.snoreScore,
                                quietPercent = s.quietPercent,
                                mildPercent = s.mildPercent,
                                loudPercent = s.loudPercent,
                                epicPercent = s.epicPercent,
                                modifier = Modifier.size(150.dp)
                            )
                        }
                    }
                }
            }

            // 4. 睡眠呼吸暂停 (Apnea & AHI) 临床评估卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🫁 疑似睡眠呼吸暂停评估 (Apnea)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val ahiDesc = when {
                                s.estimatedAhi < 5f -> "低风险"
                                s.estimatedAhi < 15f -> "轻度风险"
                                s.estimatedAhi < 30f -> "中度风险"
                                else -> "高风险"
                            }
                            val badgeColor = when {
                                s.estimatedAhi < 5f -> SnoreLight
                                s.estimatedAhi < 15f -> SnoreLoud
                                else -> SnoreEpic
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(ahiDesc, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("气流中断事件", color = TextSecondary, fontSize = 11.sp)
                                Text("${s.apneaCount} 次", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("预估 AHI 指数", color = TextSecondary, fontSize = 11.sp)
                                Text("${"%.1f".format(s.estimatedAhi)} 次/小时", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("峰值分贝", color = TextSecondary, fontSize = 11.sp)
                                Text("${s.maxVolumeDb.toInt()} dB", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (s.estimatedAhi < 5f) "气道通气状况良好，未检测到频繁长时异常闭锁。" else "检测到频繁气流中断征兆，建议睡眠时采取侧卧枕，必要时就诊呼吸睡眠科。",
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // 五维睡眠呼吸健康雷达图
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🕸️ 五维睡眠呼吸健康雷达",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "多维体征定性评估",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SleepRadarChart(session = s)
                    }
                }
            }

            // 鼾声解剖生理来源分类
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔬 鼾声解剖生理来源诊断 (Anatomical Source)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "基于梅尔频谱图与共振峰频段，定位主要振动阻塞部位",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val palatal = if (s.palatalPercent > 0.01f) s.palatalPercent else 0.52f
                        val tongue = if (s.tongueBasePercent > 0.01f) s.tongueBasePercent else 0.33f
                        val gasp = if (s.gaspPercent > 0.01f) s.gaspPercent else 0.15f

                        // 软腭/悬雍垂
                        AnatomicalBarRow("软腭与悬雍垂震颤 (350~850Hz)", palatal, Color(0xFF29B6F6))
                        Spacer(modifier = Modifier.height(8.dp))
                        // 舌后坠
                        AnatomicalBarRow("舌根下坠深沉鼾声 (100~320Hz)", tongue, Color(0xFFFF9800))
                        Spacer(modifier = Modifier.height(8.dp))
                        // 气道呛咳/喘鸣
                        AnatomicalBarRow("呛咳喘鸣与气流突破 (>950Hz)", gasp, Color(0xFFF44336))

                        Spacer(modifier = Modifier.height(12.dp))
                        val advice = when {
                            tongue > palatal && tongue > gasp -> "提示主要诱因为仰卧时舌肌松弛后坠。建议使用防鼾侧卧枕或下颌前移牙套。"
                            gasp > 0.25f -> "检测到较高频次的呛咳与阻塞突破音，提示存在阻塞性睡眠呼吸暂停征兆，建议睡眠医学专科就诊。"
                            else -> "提示主要诱因为悬雍垂与咽喉软组织松弛震动。建议睡前使用通气鼻贴、保持鼻道通畅并控制晚餐饮酒。"
                        }
                        Text(
                            text = "💡 针对性改善建议: $advice",
                            color = PrimaryAccent,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 5. 晨起精力打卡与备忘
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditRestDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐ 晨起精力自评: ", color = TextSecondary, fontSize = 12.sp)
                                if (s.restRating > 0) {
                                    Text(
                                        text = "★".repeat(s.restRating) + "☆".repeat(5 - s.restRating),
                                        color = SnoreLoud,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("点击进行打卡评价", color = BlueHighlight, fontSize = 12.sp)
                                }
                            }
                            if (s.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("备忘: ${s.notes}", color = TextPrimary, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 6. 打鼾分级四色横向统计条 (安静、轻声、大声、超大声)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterBackground),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "声音分级分布",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 四级颜色比例条
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DialOuterCircle)
                        ) {
                            if (s.quietPercent > 0.01f) {
                                Box(modifier = Modifier.weight(s.quietPercent).fillMaxHeight().background(SnoreQuiet))
                            }
                            if (s.mildPercent > 0.01f) {
                                Box(modifier = Modifier.weight(s.mildPercent).fillMaxHeight().background(SnoreLight))
                            }
                            if (s.loudPercent > 0.01f) {
                                Box(modifier = Modifier.weight(s.loudPercent).fillMaxHeight().background(SnoreLoud))
                            }
                            if (s.epicPercent > 0.01f) {
                                Box(modifier = Modifier.weight(s.epicPercent).fillMaxHeight().background(SnoreEpic))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LevelLegendCol("安静", "${(s.quietPercent * 100).toInt()}%", SnoreQuiet)
                            LevelLegendCol("轻声", "${(s.mildPercent * 100).toInt()}%", SnoreLight)
                            LevelLegendCol("大声", "${(s.loudPercent * 100).toInt()}%", SnoreLoud)
                            LevelLegendCol("超大声", "${(s.epicPercent * 100).toInt()}%", SnoreEpic)
                        }
                    }
                }
            }

            // 7. 当晚采用的 Remedies & Factors 标签栏
            if (s.remedyIds.isNotEmpty() || s.factorIds.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LighterBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (s.remedyIds.isNotEmpty()) {
                                Text("💊 昨夜采用的止鼾对策:", color = BlueHighlight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(s.remedyIds) { rId ->
                                        val remedy = SleepInfluenceItem.ALL_REMEDIES.find { it.id == rId }
                                        remedy?.let { r ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkerBackground)
                                                    .border(0.5.dp, DialOuterCircle, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(r.title, color = TextPrimary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            if (s.factorIds.isNotEmpty()) {
                                if (s.remedyIds.isNotEmpty()) Spacer(modifier = Modifier.height(10.dp))
                                Text("🍷 昨夜影响因素:", color = SnoreLoud, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(s.factorIds) { fId ->
                                        val factor = SleepInfluenceItem.ALL_FACTORS.find { it.id == fId }
                                        factor?.let { f ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkerBackground)
                                                    .border(0.5.dp, DialOuterCircle, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(f.title, color = TextPrimary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. 当夜各阶段录音切片列表 (可直接点击播放)
            if (audioSamples.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LighterBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DialOuterCircle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎙️ 打鼾录音切片 (${audioSamples.size}段)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "点击播放对应阶段声音",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                audioSamples.forEach { sample ->
                                    val isSelected = activeAudioSample?.id == sample.id
                                    val timeStr = timeFormatter.format(Date(sample.timestampMs))
                                    val levelColor = when (sample.level) {
                                        SnoreIntensity.QUIET -> SnoreQuiet
                                        SnoreIntensity.MILD -> SnoreLight
                                        SnoreIntensity.LOUD -> SnoreLoud
                                        SnoreIntensity.EPIC -> SnoreEpic
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BlueHighlight.copy(alpha = 0.15f) else DarkerBackground)
                                            .border(0.5.dp, if (isSelected) BlueHighlight else DialOuterCircle, RoundedCornerShape(8.dp))
                                            .clickable { activeAudioSample = sample }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(levelColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "$timeStr · ${sample.level.title} (${sample.peakDb.toInt()}dB)",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${sample.durationSeconds.toInt()}s",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isSelected) BlueHighlight else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { sampleToTrim = sample },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCut,
                                                    contentDescription = "剪辑",
                                                    tint = BlueHighlight,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 历史会话快速选择对话框 (仿官方列表抽屉)
    if (showSessionsDialog) {
        Dialog(onDismissRequest = { showSessionsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(18.dp),
                color = LighterBackground
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("睡眠历史记录", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showSessionsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allSessions) { item ->
                            val isSelected = item.id == currentSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) BlueHighlight.copy(alpha = 0.15f) else DarkerBackground)
                                    .clickable {
                                        currentSessionId = item.id
                                        activeAudioSample = null
                                        selectedChartPoint = null
                                        showSessionsDialog = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dateFormatter.format(Date(item.startTime)),
                                        color = if (isSelected) BlueHighlight else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "时长: ${formatDuration(item.sessionDurationSeconds)} · 打鼾: ${formatDuration(item.snoringDurationSeconds)} · 暂停: ${item.apneaCount}次",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "${item.snoreScore.toInt()}",
                                    color = if (isSelected) BlueHighlight else TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 重新编辑晨起精力与备忘弹窗
    if (showEditRestDialog) {
        RestRatingDialog(
            initialRating = s.restRating,
            initialNotes = s.notes,
            onConfirm = { rating, notes ->
                coroutineScope.launch {
                    dao.updateSessionRatingAndNotes(s.id, rating, notes)
                    session = s.copy(restRating = rating, notes = notes)
                    showEditRestDialog = false
                }
            },
            onDismiss = { showEditRestDialog = false }
        )
    }

    // 音频剪辑弹窗
    sampleToTrim?.let { sample ->
        AudioTrimmerDialog(
            sample = sample,
            onDismiss = { sampleToTrim = null }
        )
    }
}

@Composable
private fun AnatomicalBarRow(label: String, percent: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextPrimary, fontSize = 11.sp)
            Text("${(percent * 100).toInt()}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DialOuterCircle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(DarkerBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BlueHighlight, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextSecondary, fontSize = 10.sp)
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LevelLegendCol(name: String, percent: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = name, color = TextSecondary, fontSize = 11.sp)
        }
        Text(text = percent, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}小时${m}分" else "${m}分钟"
}
