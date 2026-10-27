package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytools.sleepsnore.data.model.SleepInfluenceItem
import com.mytools.sleepsnore.ui.theme.*

/**
 * 止鼾对策 (Remedies) 弹窗选择器
 */
@Composable
fun RemediesSelectionDialog(
    selectedRemedies: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedRemedies.toSet()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(20.dp),
            color = LighterBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "止鼾对策 (Remedies)",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "选择您今晚使用的防鼾用品以测试改善效果",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 对策网格 (2列)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SleepInfluenceItem.ALL_REMEDIES) { item ->
                        val isSelected = tempSelected.contains(item.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) BlueHighlight else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    tempSelected = if (isSelected) {
                                        tempSelected - item.id
                                    } else {
                                        tempSelected + item.id
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkerBackground)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = item.imageResId),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(BlueHighlight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = if (isSelected) BlueHighlight else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.description,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部确认按钮
                Button(
                    onClick = {
                        onSave(tempSelected.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "保存并应用 (已选 ${tempSelected.size} 项)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 影响因素 (Factors) 弹窗选择器
 */
@Composable
fun FactorsSelectionDialog(
    selectedFactors: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedFactors.toSet()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(20.dp),
            color = LighterBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "影响因素 (Factors)",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "标记今晚的生活习惯与身心状态以排查诱因",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 因素网格 (2列)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SleepInfluenceItem.ALL_FACTORS) { item ->
                        val isSelected = tempSelected.contains(item.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) BlueHighlight else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    tempSelected = if (isSelected) {
                                        tempSelected - item.id
                                    } else {
                                        tempSelected + item.id
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkerBackground)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = item.imageResId),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(BlueHighlight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = if (isSelected) BlueHighlight else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.description,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部确认按钮
                Button(
                    onClick = {
                        onSave(tempSelected.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "保存并应用 (已选 ${tempSelected.size} 项)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 录音模式选择弹窗
 */
@Composable
fun AudioModeDialog(
    currentMode: String, // "smart", "all", "none"
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = LighterBackground
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
                        Text("录音采样模式", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("控制整夜录音与打鼾声音片段采样方式", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val modes = listOf(
                    Triple("smart", "智能打鼾切片 (Smart)", "官方推荐模式。仅在检测到疑似鼾声、呼吸暂停或异常声响时录制高质量切片，节省电量与存储。"),
                    Triple("all", "整夜连续录音 (All)", "整夜不间断录音，记录所有声音，第二天可在时间轴上任意拉动波形收听。占用较多存储空间。"),
                    Triple("none", "仅统计数据 (None)", "只进行实时频域特征分析与分贝计算，不保存任何音频文件，严格保护绝对隐私。")
                )

                modes.forEach { (modeKey, title, desc) ->
                    val isSelected = currentMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BlueHighlight.copy(alpha = 0.15f) else DarkerBackground)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) BlueHighlight else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectMode(modeKey)
                                onDismiss()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isSelected) BlueHighlight else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = BlueHighlight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
