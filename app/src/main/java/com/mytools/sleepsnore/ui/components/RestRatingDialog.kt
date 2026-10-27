package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytools.sleepsnore.ui.theme.*

/**
 * SnoreLab 官方 1:1 晨起精力与睡眠感受自评打卡弹窗 (Morning Rest Rating Dialog)
 */
@Composable
fun RestRatingDialog(
    initialRating: Int = 3,
    initialNotes: String = "",
    onConfirm: (rating: Int, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(if (initialRating in 1..5) initialRating else 3) }
    var notesText by remember { mutableStateOf(initialNotes) }

    val ratingLabels = listOf(
        1 to "😴 极度疲倦",
        2 to "🥱 略显困倦",
        3 to "😐 状态一般",
        4 to "😊 恢复良好",
        5 to "⚡ 精力充沛"
    )

    val quickTags = listOf(
        "半夜醒来", "口干舌燥", "早起头痛", "神清气爽", "枕头偏矮", "室温偏热", "呼吸顺畅"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(22.dp),
            color = LighterBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "早安！昨晚睡得怎么样？",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "记录早晨起床后的第一精力感受，与鼾声评分对比关联",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1~5 颗星打分
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isSelected = i <= rating
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i 星",
                                tint = if (isSelected) SnoreLoud else DialOuterCircle,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ratingLabels.find { it.first == rating }?.second ?: "",
                    color = SnoreLoud,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 快捷标签
                Text(
                    text = "常用睡后速记标签:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTags) { tag ->
                        val isContained = notesText.contains(tag)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isContained) BlueHighlight.copy(alpha = 0.2f) else DarkerBackground)
                                .border(0.5.dp, if (isContained) BlueHighlight else DialOuterCircle, RoundedCornerShape(8.dp))
                                .clickable {
                                    notesText = if (isContained) {
                                        notesText.replace(tag, "").replace("  ", " ").trim()
                                    } else {
                                        if (notesText.isBlank()) tag else "$notesText $tag"
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (isContained) BlueHighlight else TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 备注输入框
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("添加昨夜其他睡眠感受备注...", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueHighlight,
                        unfocusedBorderColor = DialOuterCircle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkerBackground,
                        unfocusedContainerColor = DarkerBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 底部保存按钮
                Button(
                    onClick = {
                        onConfirm(rating, notesText.trim())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存并查看睡眠分析报告", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
