package com.mytools.sleepsnore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * 沉浸式动态水波流体微动效
 * 用于白噪音助眠界面与夜视全屏，营造宁静舒缓的潮汐呼吸感
 */
@Composable
fun FluidWaveVisualizer(
    modifier: Modifier = Modifier,
    amplitudeMultiplier: Float = 1.0f,
    baseColor: Color = Color(0xFF00E5FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = (Math.PI).toFloat(),
        targetValue = (3 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val waveBaseY = height * 0.65f
        val amp1 = 28f * amplitudeMultiplier * breatheScale
        val amp2 = 20f * amplitudeMultiplier

        // Wave Layer 1 (底层深邃微光)
        val path1 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, waveBaseY)
            val step = 15f
            var x = 0f
            while (x <= width) {
                val waveY = waveBaseY + sin((x / width * 3.5f * Math.PI) + phase1).toFloat() * amp1
                lineTo(x, waveY)
                x += step
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = path1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.22f),
                    baseColor.copy(alpha = 0.05f)
                ),
                startY = waveBaseY - amp1,
                endY = height
            )
        )

        // Wave Layer 2 (表层高光潮汐)
        val path2 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, waveBaseY + 6f)
            val step = 15f
            var x = 0f
            while (x <= width) {
                val waveY = (waveBaseY + 6f) + sin((x / width * 4.2f * Math.PI) + phase2).toFloat() * amp2
                lineTo(x, waveY)
                x += step
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = path2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2196F3).copy(alpha = 0.32f),
                    Color(0xFF1565C0).copy(alpha = 0.08f)
                ),
                startY = waveBaseY - amp2,
                endY = height
            )
        )
    }
}
