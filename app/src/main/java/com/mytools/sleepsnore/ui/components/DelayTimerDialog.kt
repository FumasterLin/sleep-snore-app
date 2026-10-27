package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary

@Composable
fun DelayTimerDialog(
    currentDelayMinutes: Int,
    onSelectDelay: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("延迟开始监测 (Delay Timer)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("等您安稳入睡后再启动录音，过滤翻身杂音", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val options = listOf(
                    0 to "无延迟 (立即开始监测)",
                    10 to "10 分钟 (适合快速入睡人群)",
                    20 to "20 分钟 (推荐日常标准入睡时长)",
                    30 to "30 分钟 (适合轻度失眠/翻身较多者)",
                    45 to "45 分钟",
                    60 to "60 分钟 (1小时超长就寝准备)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (mins, desc) ->
                        val isSelected = currentDelayMinutes == mins
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryAccent.copy(alpha = 0.2f) else DarkCard)
                                .clickable {
                                    onSelectDelay(mins)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = desc,
                                color = if (isSelected) PrimaryAccent else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text("✓", color = PrimaryAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
