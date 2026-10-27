package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * SnoreLab 官方 1:1 弧形打鼾评分仪表盘 (Speedometer Arc Gauge)
 * 弧形总跨度 270°（从 135° 顺时针扫至 405°，开口朝下 90°）
 * 外圈四色分段（安静、轻声、大声、超大声），中心大号评分与“鼾声评分”标签
 */
@Composable
fun ScoreRoundChart(
    score: Float,
    quietPercent: Float,
    mildPercent: Float,
    loudPercent: Float,
    epicPercent: Float,
    modifier: Modifier = Modifier
) {
    val scoreInt = score.toInt()
    val (statusText, statusColor) = when {
        score < 15f -> "安静" to SnoreQuiet
        score < 30f -> "轻声" to SnoreLight
        score < 70f -> "大声" to SnoreLoud
        else -> "超大声" to SnoreEpic
    }

    // 仪表盘总弧度 270 度，起始角 135 度 (左下方)
    val startAngle = 135f
    val totalSweep = 270f

    val dialColor = DialOuterCircle
    val quietColor = SnoreQuiet
    val lightColor = SnoreLight
    val loudColor = SnoreLoud
    val epicColor = SnoreEpic

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width - strokeWidth) / 2

            // 1. 底槽：官方深灰圆弧底槽 (DialOuterCircle)
            drawArc(
                color = dialColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. 四色分段弧（若有占比数据按占比，否则按标准分段 0~15, 15~30, 30~70, 70~100 绘制刻度）
            val totalPercent = quietPercent + mildPercent + loudPercent + epicPercent
            if (totalPercent > 0.05f) {
                var currentAngle = startAngle
                val segments = listOf(
                    (quietPercent / totalPercent) to quietColor,
                    (mildPercent / totalPercent) to lightColor,
                    (loudPercent / totalPercent) to loudColor,
                    (epicPercent / totalPercent) to epicColor
                )

                for ((fraction, color) in segments) {
                    if (fraction > 0.01f) {
                        val sweep = fraction * totalSweep
                        drawArc(
                            color = color,
                            startAngle = currentAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        currentAngle += sweep
                    }
                }
            } else {
                // 默认参考刻度分段
                val defaultSegments = listOf(
                    0.20f to quietColor,
                    0.30f to lightColor,
                    0.30f to loudColor,
                    0.20f to epicColor
                )
                var currentAngle = startAngle
                for ((fraction, color) in defaultSegments) {
                    val sweep = fraction * totalSweep
                    drawArc(
                        color = color.copy(alpha = 0.5f),
                        startAngle = currentAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += sweep
                }
            }

            // 3. 游标发光圆点 (根据当前 score 映射角度)
            val normalizedScore = (score / 100f).coerceIn(0f, 1f)
            val indicatorAngle = Math.toRadians((startAngle + normalizedScore * totalSweep).toDouble())
            val indicatorX = center.x + radius * cos(indicatorAngle).toFloat()
            val indicatorY = center.y + radius * sin(indicatorAngle).toFloat()

            // 游标外发光
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = 10.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
            // 游标核心白点
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }

        // 中心数值展示：官方大号评分 + 鼾声评分 (SNORE SCORE) + 状态
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = "$scoreInt",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "鼾声评分",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
