package com.mytools.sleepsnore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class StarParticle(
    val xFraction: Float,
    val yFraction: Float,
    val baseRadius: Float,
    val pulsePeriodMs: Int,
    val phaseOffset: Float,
    val baseAlpha: Float
)

/**
 * SnoreLab 官方 1:1 动态微光星空粒子系统 (Stars Particle Background)
 * 对应官方 fragment_night_view.xml 中的 @id/stars_view
 */
@Composable
fun StarsBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 65
) {
    // 固定种子生成星宿布局
    val stars = remember {
        val rand = Random(42)
        List(starCount) {
            StarParticle(
                xFraction = rand.nextFloat(),
                yFraction = rand.nextFloat(),
                baseRadius = 0.8f + rand.nextFloat() * 1.6f,
                pulsePeriodMs = 2000 + rand.nextInt(3000),
                phaseOffset = rand.nextFloat() * 6.28f,
                baseAlpha = 0.2f + rand.nextFloat() * 0.45f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starsAnimation")
    val timeProgression by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val moonCenterX = size.width / 2f
        val moonCenterY = size.height * 0.41f // 对应夜视月亮中心水平位置
        val moonRadiusExclusion = 135.dp.toPx()

        for (star in stars) {
            val px = star.xFraction * size.width
            val py = star.yFraction * size.height

            // 避开月亮中心核心区域，保持月面时钟清晰
            val distToMoon = hypot(px - moonCenterX, py - moonCenterY)
            if (distToMoon < moonRadiusExclusion) continue

            // 动态呼吸正弦变化
            val alphaFactor = (sin(timeProgression * (6000f / star.pulsePeriodMs) + star.phaseOffset) + 1f) / 2f
            val alpha = (star.baseAlpha + alphaFactor * 0.45f).coerceIn(0.1f, 0.95f)

            // 银蓝微光与核心白点
            val radiusPx = star.baseRadius.dp.toPx()
            drawCircle(
                color = Color(0xFF90CAF9).copy(alpha = alpha * 0.4f),
                radius = radiusPx * 1.8f,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radiusPx,
                center = Offset(px, py)
            )
        }
    }
}
