package com.mytools.sleepsnore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onSelectSession: (Long) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { SleepDatabase.getInstance(context).sleepDao() }
    val sessions by dao.getAllSessions().collectAsState(initial = emptyList())

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd EEE", Locale.CHINESE) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "历史睡眠记录",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无历史记录，开始您的第一次睡眠监测吧", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            // 历史平均概览卡片
            val avgScore = sessions.map { it.snoreScore }.average().toFloat()
            val avgSnoreTime = sessions.map { it.snoringDurationSeconds }.average().toLong()

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("平均打鼾评分", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${avgScore.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("平均打鼾时长", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${avgSnoreTime / 60}分钟",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("记录天数", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sessions.size}晚",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { session ->
                    SessionHistoryItem(
                        session = session,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        onClick = { onSelectSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryItem(
    session: SleepSession,
    dateFormatter: SimpleDateFormat,
    timeFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val score = session.snoreScore.toInt()
    val levelColor = when {
        score < 15 -> SnoreQuiet
        score < 30 -> SnoreMild
        score < 70 -> SnoreLoud
        else -> SnoreEpic
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dateFormatter.format(Date(session.startTime)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                val durationHours = session.sessionDurationSeconds / 3600
                val durationMinutes = (session.sessionDurationSeconds % 3600) / 60
                val snoreMinutes = session.snoringDurationSeconds / 60
                Text(
                    text = "${timeFormatter.format(Date(session.startTime))} ~ ${timeFormatter.format(Date(session.endTime))} · 睡眠 ${durationHours}h ${durationMinutes}m · 鼾声 ${snoreMinutes}m",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // 右侧评分徽章
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(levelColor.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = "分",
                        fontSize = 10.sp,
                        color = levelColor
                    )
                }
            }
        }
    }
}
