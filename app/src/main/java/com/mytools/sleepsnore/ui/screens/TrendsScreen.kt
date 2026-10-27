package com.mytools.sleepsnore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.db.SleepDao
import com.mytools.sleepsnore.data.model.SleepInfluenceItem
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.ui.components.SmoothSplineChart
import com.mytools.sleepsnore.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrendsScreen(
    sleepDao: SleepDao,
    onNavigateToResult: (Long) -> Unit
) {
    val allSessions by sleepDao.getAllSessions().collectAsState(initial = emptyList())
    var selectedSubTab by remember { mutableStateOf(0) } // 0: 日历, 1: 走势折线, 2: 对策匹配器

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 顶栏
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "打鼾趋势与对比 (Trends)",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "追踪长期打鼾变化规律，对比止鼾用品的实际改善成效",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // 二级导航条 (日历 / 趋势 / 对策成效)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("📅 打鼾日历", "📈 评分走势", "⚖️ 止鼾成效对比").forEachIndexed { index, title ->
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryAccent else DarkCard)
                        .clickable { selectedSubTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedSubTab) {
            0 -> CalendarView(allSessions, onNavigateToResult)
            1 -> ChartsView(allSessions)
            2 -> RemedyMatcherView(allSessions)
        }
    }
}

/**
 * 1. 月历热力视图
 */
@Composable
private fun CalendarView(
    sessions: List<SleepSession>,
    onNavigateToResult: (Long) -> Unit
) {
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val monthTitle = remember(calendarMonth) {
        SimpleDateFormat("yyyy年 M月", Locale.CHINESE).format(calendarMonth.time)
    }

    val daysInMonth = remember(calendarMonth) {
        val cal = calendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1: 周日, 2: 周一
        Triple(maxDays, firstDayOfWeek, cal)
    }

    // 将 sessions 映射到按天格式化字符串
    val sessionMap = remember(sessions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sessions.associateBy { sdf.format(Date(it.startTime)) }
    }

    var selectedSessionForPopup by remember { mutableStateOf<SleepSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 月份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val cal = calendarMonth.clone() as Calendar
                cal.add(Calendar.MONTH, -1)
                calendarMonth = cal
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = TextPrimary)
            }

            Text(text = monthTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = {
                val cal = calendarMonth.clone() as Calendar
                cal.add(Calendar.MONTH, 1)
                calendarMonth = cal
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = d, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日历网格
        val (maxDays, firstDayOfWeek, _) = daysInMonth
        val offsetDays = firstDayOfWeek - 1

        val totalCells = ((offsetDays + maxDays + 6) / 7) * 7

        Column(modifier = Modifier.fillMaxWidth()) {
            for (week in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    for (dayOfWeek in 0..6) {
                        val cellIndex = week * 7 + dayOfWeek
                        val dayNum = cellIndex - offsetDays + 1

                        if (dayNum in 1..maxDays) {
                            val curCal = calendarMonth.clone() as Calendar
                            curCal.set(Calendar.DAY_OF_MONTH, dayNum)
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(curCal.time)
                            val session = sessionMap[dateKey]

                            val scoreColor = session?.let {
                                when (SnoreIntensity.fromScore(it.snoreScore)) {
                                    SnoreIntensity.QUIET -> Color(0xFF4CAF50)
                                    SnoreIntensity.MILD -> Color(0xFFFFEB3B)
                                    SnoreIntensity.LOUD -> Color(0xFFFF9800)
                                    SnoreIntensity.EPIC -> Color(0xFFF44336)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (session != null) DarkCard else Color.Transparent)
                                    .clickable(enabled = session != null) {
                                        selectedSessionForPopup = session
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (session != null) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        fontWeight = if (session != null) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (session != null && scoreColor != null) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(scoreColor.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${session.snoreScore.toInt()}",
                                                color = scoreColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 选中日期的弹窗卡片
        selectedSessionForPopup?.let { session ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToResult(session.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val dateStr = SimpleDateFormat("M月d日 睡眠报告", Locale.CHINESE).format(Date(session.startTime))
                        Text(text = dateStr, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val apneaInfo = if (session.apneaCount > 0) " · 暂停 ${session.apneaCount}次" else ""
                        val ratingInfo = if (session.restRating > 0) " · 精力 ${session.restRating}★" else ""
                        Text(
                            text = "打鼾: ${session.snoringDurationSeconds / 60}分钟 · 峰值: ${session.maxVolumeDb.toInt()}dB$apneaInfo$ratingInfo",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryAccent)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("得分: ${session.snoreScore.toInt()} ➔ 查看", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 2. 走势折线图与平均指标
 */
@Composable
private fun ChartsView(sessions: List<SleepSession>) {
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无足够的历史打鼾数据，请先进行整夜监测", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    val reversed = remember(sessions) { sessions.reversed() }
    val avgScore = remember(sessions) { sessions.map { it.snoreScore }.average().toFloat() }
    val maxScore = remember(sessions) { sessions.maxOfOrNull { it.snoreScore } ?: 0f }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                            Text("📈 三阶贝塞尔打鼾评分平滑走势", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("带渐变流光与 30/60 临床警戒线，越低越轻微", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 三阶贝塞尔平滑流光曲线图
                    SmoothSplineChart(sessions = sessions)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("近14天平均评分", color = TextSecondary, fontSize = 11.sp)
                            Text("${avgScore.toInt()}", color = PrimaryAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("最高峰值评分", color = TextSecondary, fontSize = 11.sp)
                            Text("${maxScore.toInt()}", color = Color(0xFFF44336), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. 止鼾对策实际改善成效对比器 (Remedy Matcher)
 */
@Composable
private fun RemedyMatcherView(sessions: List<SleepSession>) {
    // 计算每个 Remedy 在使用 vs 未使用时的平均打鼾评分
    val remedyStats = remember(sessions) {
        SleepInfluenceItem.ALL_REMEDIES.mapNotNull { item ->
            val withRemedy = sessions.filter { it.remedyIds.contains(item.id) }
            val withoutRemedy = sessions.filter { !it.remedyIds.contains(item.id) }

            if (withRemedy.isNotEmpty() && withoutRemedy.isNotEmpty()) {
                val avgWith = withRemedy.map { it.snoreScore }.average().toFloat()
                val avgWithout = withoutRemedy.map { it.snoreScore }.average().toFloat()
                val diffPercent = ((avgWith - avgWithout) / avgWithout * 100f).toInt()
                Triple(item, Pair(avgWith, avgWithout), diffPercent)
            } else null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 止鼾用具真实效果对比分析", color = PrimaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "基于您的多夜真实数据，对比使用特定对策时的打鼾评分改善幅度，帮您找到最适合自己的止鼾方案。",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (remedyStats.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无对比数据", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "提示：在睡前勾选不同的止鼾对策（如侧卧、鼻贴、牙套）并监测若干夜晚后，系统将自动计算出各对策的打鼾改善百分比！",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(remedyStats) { (item, scores, diff) ->
                val (avgWith, avgWithout) = scores
                val isImproved = diff < 0 // 分数降低 = 打鼾改善

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val ratingText = when {
                                diff <= -40 -> "显著改善 ★★★★★"
                                diff <= -20 -> "明显改善 ★★★★☆"
                                diff < 0 -> "轻微改善 ★★★☆☆"
                                else -> "效果不明显"
                            }
                            val badgeColor = if (isImproved) Color(0xFF4CAF50) else Color(0xFFF44336)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(ratingText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 对比柱条
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("使用该对策: ${avgWith.toInt()}分", color = TextSecondary, fontSize = 11.sp)
                            Text("未用对照: ${avgWithout.toInt()}分", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val maxVal = maxOf(avgWith, avgWithout, 1f)
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                Box(modifier = Modifier.fillMaxWidth((avgWith / maxVal).coerceIn(0.05f, 1f)).fillMaxHeight().background(if (isImproved) Color(0xFF4CAF50) else Color(0xFFF44336)))
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                Box(modifier = Modifier.fillMaxWidth((avgWithout / maxVal).coerceIn(0.05f, 1f)).fillMaxHeight().background(Color(0xFF90A4AE)))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "打鼾评分改善: ${if (diff > 0) "+" else ""}$diff%",
                                color = if (isImproved) Color(0xFF4CAF50) else Color(0xFFF44336),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
