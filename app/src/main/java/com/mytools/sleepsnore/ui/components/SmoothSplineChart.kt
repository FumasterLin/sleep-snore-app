package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

/**
 * 临床级三阶贝塞尔平滑趋势曲线图
 * 带有渐变垂直流光投影与关键峰谷节点标注
 */
@Composable
fun SmoothSplineChart(
    sessions: List<SleepSession>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val reversed = sessions.take(14).reversed()
    val maxScore = (reversed.maxOfOrNull { it.snoreScore } ?: 50f).coerceAtLeast(60f)
    val dateFormatter = SimpleDateFormat("M/d", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val n = reversed.size

            if (n == 1) {
                // 单点居中
                val cx = width / 2f
                val cy = height * (1f - (reversed[0].snoreScore / maxScore).coerceIn(0.1f, 0.9f))
                drawCircle(color = Color(0xFF29B6F6), radius = 6.dp.toPx(), center = Offset(cx, cy))
                return@Canvas
            }

            // 1. 绘制参考线 (Score = 30 轻度, Score = 60 大声)
            val y30 = height * (1f - (30f / maxScore).coerceIn(0f, 1f))
            val y60 = height * (1f - (60f / maxScore).coerceIn(0f, 1f))
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y30),
                end = Offset(width, y30),
                pathEffect = dashEffect,
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFFF44336).copy(alpha = 0.15f),
                start = Offset(0f, y60),
                end = Offset(width, y60),
                pathEffect = dashEffect,
                strokeWidth = 1.dp.toPx()
            )

            // 计算所有数据点坐标
            val stepX = width / (n - 1)
            val points = reversed.mapIndexed { idx, s ->
                val x = idx * stepX
                val ratio = (s.snoreScore / maxScore).coerceIn(0.08f, 0.92f)
                val y = height * (1f - ratio)
                Offset(x, y)
            }

            // 2. 构造三阶贝塞尔平滑曲线 Path
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx1 = p0.x + (p1.x - p0.x) / 2f
                    val cy1 = p0.y
                    val cx2 = p0.x + (p1.x - p0.x) / 2f
                    val cy2 = p1.y
                    cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }
            }

            // 3. 构造渐变填充闭合 Path
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF29B6F6).copy(alpha = 0.35f),
                        Color(0xFF00E5FF).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // 4. 绘制平滑曲线主体
            drawPath(
                path = strokePath,
                color = Color(0xFF29B6F6),
                style = Stroke(width = 3.dp.toPx())
            )

            // 5. 绘制数据点与节点高光
            points.forEachIndexed { idx, pt ->
                val s = reversed[idx]
                val dotColor = when (SnoreIntensity.fromScore(s.snoreScore)) {
                    SnoreIntensity.QUIET -> Color(0xFF4CAF50)
                    SnoreIntensity.MILD -> Color(0xFFFFEB3B)
                    SnoreIntensity.LOUD -> Color(0xFFFF9800)
                    SnoreIntensity.EPIC -> Color(0xFFF44336)
                }

                // 光晕
                drawCircle(color = dotColor.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pt)
                // 实体点
                drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = pt)
            }
        }

        // 底部日期轴
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayList = if (reversed.size > 7) {
                listOf(reversed.first(), reversed[reversed.size / 2], reversed.last())
            } else reversed

            displayList.forEach { s ->
                Text(
                    text = dateFormatter.format(Date(s.startTime)),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
