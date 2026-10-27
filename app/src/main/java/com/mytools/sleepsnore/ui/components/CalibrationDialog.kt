package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary

@Composable
fun CalibrationDialog(
    currentGainDb: Float,
    onSaveGain: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var gain by remember { mutableStateOf(currentGainDb) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
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
                        Text("麦克风灵敏度校准", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("适配不同手机麦克风增益硬件差异", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 摆放指导说明
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📱 手机最佳摆放建议:", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("1. 放置在床头柜上，距离枕头约 0.5 ~ 1.0 米。", color = TextPrimary, fontSize = 11.sp)
                        Text("2. 麦克风朝向床位，避免被被褥或衣物遮挡。", color = TextPrimary, fontSize = 11.sp)
                        Text("3. 建议整夜连接充电器，防止电量耗尽。", color = TextPrimary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("灵敏度偏移调整: ${if (gain >= 0) "+" else ""}${String.format("%.1f", gain)} dB", color = TextPrimary, fontSize = 14.sp)
                Slider(
                    value = gain,
                    onValueChange = { gain = it },
                    valueRange = -6.0f..6.0f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-6 dB (过敏降敏)", color = TextSecondary, fontSize = 10.sp)
                    Text("0 dB (默认标定)", color = TextSecondary, fontSize = 10.sp)
                    Text("+6 dB (增强收音)", color = TextSecondary, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSaveGain(gain)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存校准", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
