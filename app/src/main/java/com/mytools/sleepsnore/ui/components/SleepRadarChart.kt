package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 五维睡眠通气健康雷达图
 * 维度：打鼾控制、气流通畅、安睡静音、呼吸平稳、晨起活力
 */
@Composable
fun SleepRadarChart(
    session: SleepSession,
    modifier: Modifier = Modifier
) {
    // 5个维度的标准化数值 (0.0 ~ 1.0)
    val snoreControl = ((100f - session.snoreScore) / 100f).coerceIn(0.12f, 1.0f)
    val airwayFlow = ((100f - session.estimatedAhi * 5f) / 100f).coerceIn(0.12f, 1.0f)
    val silenceRatio = session.quietPercent.coerceIn(0.12f, 1.0f)
    val stability = ((100f - (session.loudPercent + session.epicPercent * 2f) * 100f) / 100f).coerceIn(0.12f, 1.0f)
    val morningEnergy = (if (session.restRating > 0) session.restRating * 0.2f else 0.8f).coerceIn(0.12f, 1.0f)

    val values = listOf(snoreControl, airwayFlow, silenceRatio, stability, morningEnergy)
    val labels = listOf("打鼾控制", "气流通畅", "安睡静音", "呼吸平稳", "晨起活力")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(22.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(centerX, centerY) * 0.78f
            val count = 5
            val angleStep = (2 * Math.PI / count).toFloat()
            val startAngle = (-Math.PI / 2).toFloat() // 顶点朝上

            // 1. 绘制 4 层同心五边形网格背景
            val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
            gridLevels.forEach { level ->
                val gridPath = Path()
                for (i in 0 until count) {
                    val angle = startAngle + i * angleStep
                    val x = centerX + radius * level * cos(angle)
                    val y = centerY + radius * level * sin(angle)
                    if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                drawPath(
                    path = gridPath,
                    color = Color.White.copy(alpha = 0.08f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. 绘制 5 根轴线
            for (i in 0 until count) {
                val angle = startAngle + i * angleStep
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(centerX, centerY),
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. 绘制用户数据多边形
            val dataPath = Path()
            val points = mutableListOf<Offset>()
            for (i in 0 until count) {
                val angle = startAngle + i * angleStep
                val r = radius * values[i]
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                val pt = Offset(x, y)
                points.add(pt)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // 填充半透明青蓝色
            drawPath(
                path = dataPath,
                color = Color(0x3800E5FF)
            )
            // 描边
            drawPath(
                path = dataPath,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 2.2.dp.toPx())
            )

            // 绘制各顶点高光圆点
            points.forEach { pt ->
                drawCircle(color = Color(0xFF00E5FF), radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = pt)
            }
        }

        // 标签布局 (顶、右上、右下、左下、左上)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部：打鼾控制
            Text(
                text = "打鼾控制 ${(values[0] * 100).toInt()}%",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "晨起活力 ${(values[4] * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "气流通畅 ${(values[1] * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "呼吸平稳 ${(values[3] * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "安睡静音 ${(values[2] * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
