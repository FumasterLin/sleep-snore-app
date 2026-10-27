package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mytools.sleepsnore.audio.AlarmConfig
import com.mytools.sleepsnore.audio.AlarmManagerHelper
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary

@Composable
fun SmartAlarmDialog(
    alarmConfig: AlarmConfig,
    alarmHelper: AlarmManagerHelper,
    onSaveConfig: (AlarmConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(alarmConfig.enabled) }
    var hour by remember { mutableStateOf(alarmConfig.hour) }
    var minute by remember { mutableStateOf(alarmConfig.minute) }
    var windowMinutes by remember { mutableStateOf(alarmConfig.wakeWindowMinutes) }
    var selectedSoundId by remember { mutableStateOf(alarmConfig.soundId) }
    var isPreviewing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            alarmHelper.stopPreview()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "智能轻柔唤醒闹钟",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "在浅睡期以温和低音量逐渐唤醒，远离起床气",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 开启闹钟 Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用智能唤醒闹钟", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryAccent,
                            checkedTrackColor = PrimaryAccent.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 时间选择 (小时与分钟)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小时调节
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { hour = (hour + 1) % 24 },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = TextPrimary, fontSize = 16.sp) }

                        Text(
                            text = "%02d".format(hour),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Button(
                            onClick = { hour = (hour - 1 + 24) % 24 },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("-", color = TextPrimary, fontSize = 16.sp) }
                    }

                    Text(" : ", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))

                    // 分钟调节
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { minute = (minute + 5) % 60 },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = TextPrimary, fontSize = 16.sp) }

                        Text(
                            text = "%02d".format(minute),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Button(
                            onClick = { minute = (minute - 5 + 60) % 60 },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("-", color = TextPrimary, fontSize = 16.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 唤醒窗口选择
                Text("智能唤醒窗口:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "准时", 15 to "15分钟", 20 to "20分钟", 30 to "30分钟").forEach { (win, label) ->
                        val isSelected = windowMinutes == win
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryAccent else DarkCard)
                                .clickable { windowMinutes = win }
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

                // 铃声选择
                Text("唤醒铃声:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlarmManagerHelper.ALL_SOUNDS.forEach { sound ->
                        val isSelected = selectedSoundId == sound.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryAccent.copy(alpha = 0.15f) else DarkCard)
                                .clickable { selectedSoundId = sound.id }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sound.name,
                                    color = if (isSelected) PrimaryAccent else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(sound.description, color = TextSecondary, fontSize = 10.sp)
                            }

                            IconButton(
                                onClick = {
                                    if (isPreviewing) {
                                        alarmHelper.stopPreview()
                                        isPreviewing = false
                                    } else {
                                        alarmHelper.previewSound(sound.soundResId)
                                        isPreviewing = true
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewing && isSelected) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "试听",
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 保存确认
                Button(
                    onClick = {
                        alarmHelper.stopPreview()
                        onSaveConfig(
                            AlarmConfig(
                                enabled = enabled,
                                hour = hour,
                                minute = minute,
                                wakeWindowMinutes = windowMinutes,
                                soundId = selectedSoundId
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存闹钟配置", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
