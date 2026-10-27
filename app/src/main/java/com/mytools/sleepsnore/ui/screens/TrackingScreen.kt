package com.mytools.sleepsnore.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
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
import androidx.core.content.ContextCompat
import com.mytools.sleepsnore.service.SleepTrackingService
import com.mytools.sleepsnore.ui.theme.*

@Composable
fun TrackingScreen(
    selectedFactors: List<String>,
    selectedRemedies: List<String>,
    onOpenInfluences: () -> Unit,
    onNavigateToResult: (Long) -> Unit
) {
    val context = LocalContext.current
    val liveState by SleepTrackingService.liveState.collectAsState()

    // 录音权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SleepTrackingService.startTracking(context, selectedFactors, selectedRemedies)
        } else {
            Toast.makeText(context, "需要麦克风权限才能进行睡眠打鼾监测", Toast.LENGTH_LONG).show()
        }
    }

    // 监听停止监测后跳转结果页
    LaunchedEffect(liveState.isTracking, liveState.sessionId) {
        if (!liveState.isTracking && liveState.sessionId > 0) {
            onNavigateToResult(liveState.sessionId)
        }
    }

    // 水波纹脉动动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部标题与状态
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (liveState.isTracking) "睡眠监测中" else "准备入睡",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (liveState.isTracking) "请将手机放置于床头约 1 米内，屏幕将保持低功耗深色显示" else "记录整夜鼾声、呼吸节奏并生成专属打鼾评分",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // 中间动态声波与分贝仪表
        Box(
            modifier = Modifier
                .size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            if (liveState.isTracking) {
                // 动态声波光晕
                val levelColor = Color(liveState.currentLevel.colorHex)
                val dynamicRadiusFactor = (liveState.currentDb / 80f).coerceIn(0.8f, 1.4f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = levelColor.copy(alpha = 0.15f),
                        radius = (size.minDimension / 2) * pulseScale * dynamicRadiusFactor
                    )
                    drawCircle(
                        color = levelColor.copy(alpha = 0.3f),
                        radius = (size.minDimension / 2.5f) * dynamicRadiusFactor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${liveState.currentDb.toInt()}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "dB (分贝)",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 当前状态胶囊
                    Box(
                        modifier = Modifier
                            .background(levelColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, levelColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = liveState.currentLevel.title,
                            color = levelColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // 未启动时静态图标
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // 底部控制区域
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (liveState.isTracking) {
                // 实时计时器与当前预估评分
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("已监测时长", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sec = liveState.elapsedSeconds
                        val timeStr = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
                        Text(timeStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("当前预估评分", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${liveState.snoreScoreEstimate.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 停止按钮
                Button(
                    onClick = { SleepTrackingService.stopTracking(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = SnoreEpic),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("醒来，停止监测并生成报告", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // 睡前对策与诱因入口
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .clickable { onOpenInfluences() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("睡前影响因素设置", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "已选对策: ${selectedRemedies.size} 项 | 诱因: ${selectedFactors.size} 项",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text("设置 >", color = AccentPrimary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 开始监测按钮
                Button(
                    onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMic) {
                            SleepTrackingService.startTracking(context, selectedFactors, selectedRemedies)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("开始睡眠监测", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
