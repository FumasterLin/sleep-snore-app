package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.DialogProperties
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary

/**
 * 睡眠生理档案与体征评估编辑弹窗
 */
@Composable
fun ProfileEditDialog(
    initialGender: String,
    initialAge: Int,
    initialHeight: Int,
    initialWeight: Int,
    initialNoseStatus: String,
    initialSmoking: String,
    onSave: (gender: String, age: Int, height: Int, weight: Int, nose: String, smoking: String) -> Unit,
    onDismiss: () -> Unit
) {
    var gender by remember { mutableStateOf(initialGender) }
    var age by remember { mutableIntStateOf(initialAge) }
    var height by remember { mutableIntStateOf(initialHeight) }
    var weight by remember { mutableIntStateOf(initialWeight) }
    var noseStatus by remember { mutableStateOf(initialNoseStatus) }
    var smoking by remember { mutableStateOf(initialSmoking) }

    // 实时计算 BMI
    val bmi = remember(height, weight) {
        val hM = height / 100.0f
        if (hM > 0) weight / (hM * hM) else 22.0f
    }

    val bmiCategory = when {
        bmi < 18.5f -> Pair("偏瘦", Color(0xFF64B5F6))
        bmi < 24.0f -> Pair("标准正常", Color(0xFF4CAF50))
        bmi < 28.0f -> Pair("超重 (咽腔压迫)", Color(0xFFFF9800))
        else -> Pair("肥胖 (打鼾极高危)", Color(0xFFF44336))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("👤 睡眠生理健康档案", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("精确的生理档案能显著提升打鼾风险与 BMI 预测精度", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BMI 实时看板
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("身体质量指数 BMI", color = TextSecondary, fontSize = 11.sp)
                                Text("%.1f".format(bmi), color = bmiCategory.second, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bmiCategory.second.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(bmiCategory.first, color = bmiCategory.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 1. 性别选择
                    Column {
                        Text("生理性别:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("男", "女").forEach { g ->
                                val isSelected = gender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkCard)
                                        .clickable { gender = g }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (g == "男") "👨 男士 (咽腔阻力偏高)" else "👩 女士",
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // 2. 年龄选择
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("年龄: $age 岁", color = TextPrimary, fontSize = 13.sp)
                            Text(if (age >= 50) "软腭肌肉弹性下降" else "肌肉张力正常", color = TextSecondary, fontSize = 11.sp)
                        }
                        Slider(
                            value = age.toFloat(),
                            onValueChange = { age = it.toInt() },
                            valueRange = 18f..85f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryAccent, activeTrackColor = PrimaryAccent)
                        )
                    }

                    // 3. 身高选择
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("身高: $height cm", color = TextPrimary, fontSize = 13.sp)
                        }
                        Slider(
                            value = height.toFloat(),
                            onValueChange = { height = it.toInt() },
                            valueRange = 140f..215f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryAccent, activeTrackColor = PrimaryAccent)
                        )
                    }

                    // 4. 体重选择
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("体重: $weight kg", color = TextPrimary, fontSize = 13.sp)
                            Text(if (weight >= 85) "颈部脂肪压迫气道风险偏高" else "", color = Color(0xFFFF9800), fontSize = 11.sp)
                        }
                        Slider(
                            value = weight.toFloat(),
                            onValueChange = { weight = it.toInt() },
                            valueRange = 40f..150f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryAccent, activeTrackColor = PrimaryAccent)
                        )
                    }

                    // 5. 鼻腔通气情况
                    Column {
                        Text("鼻腔通气自评:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("通畅良好" to "通畅", "季节性鼻塞" to "轻度鼻塞", "鼻中隔偏曲/肥厚" to "严重鼻塞").forEach { (title, key) ->
                                val isSelected = noseStatus == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkCard)
                                        .clickable { noseStatus = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // 6. 吸烟习惯
                    Column {
                        Text("吸烟习惯:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("从不吸烟" to "从不", "偶尔吸烟" to "偶尔", "经常吸烟" to "经常").forEach { (title, key) ->
                                val isSelected = smoking == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkCard)
                                        .clickable { smoking = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部保存按钮
                Button(
                    onClick = {
                        onSave(gender, age, height, weight, noseStatus, smoking)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("保存健康生理档案", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
