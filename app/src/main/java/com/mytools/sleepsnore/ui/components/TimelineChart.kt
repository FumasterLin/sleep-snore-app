package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * SnoreLab 官方 1:1 结果页时间轴直方图 (SnoreGraphView)
 * 升级版：支持双指缩放 (Pinch-to-Zoom)、平滑横向拖拽、呼吸暂停 (Apnea) 警示标与光标联动
 */
@Composable
fun TimelineChart(
    points: List<ChartPoint>,
    startTimeMs: Long,
    endTimeMs: Long,
    selectedPoint: ChartPoint?,
    onPointSelected: (ChartPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(DarkerBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无音频分析柱状图数据",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkerBackground, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        // 顶部控制行：状态提示 + 缩放倍率 / 复位按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "点击柱条播放音频",
                    color = BlueHighlight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (selectedPoint != null) {
                    val timeStr = timeFormatter.format(Date(selectedPoint.timestampMs))
                    val levelColor = when (selectedPoint.intensityLevel) {
                        SnoreIntensity.QUIET -> SnoreQuiet
                        SnoreIntensity.MILD -> SnoreLight
                        SnoreIntensity.LOUD -> SnoreLoud
                        SnoreIntensity.EPIC -> SnoreEpic
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· $timeStr (${selectedPoint.volumeDb.toInt()}dB)",
                        color = levelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedPoint.isApneaSuspected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⚠️疑似暂停",
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 缩放控制器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (zoomScale > 1.05f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LighterBackground)
                            .border(0.5.dp, BlueHighlight, RoundedCornerShape(6.dp))
                            .clickable {
                                zoomScale = 1.0f
                                panOffsetX = 0f
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(zoomScale)}x 复位",
                            color = BlueHighlight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "双指可放大",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 柱状图 Canvas（支持双指缩放 + 单指点击定位 + 拖拽滑动）
        val selectedIndex = if (selectedPoint != null) points.indexOf(selectedPoint) else -1
        val dialColor = DialOuterCircle
        val blueColor = BlueHighlight
        val quietColor = SnoreQuiet
        val lightColor = SnoreLight
        val loudColor = SnoreLoud
        val epicColor = SnoreEpic

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        // 1. 双指手势缩放与拖动平移
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1.0f, 4.5f)
                            val maxPan = (size.width * (newScale - 1f)).coerceAtLeast(0f)
                            val newPan = (panOffsetX + pan.x).coerceIn(-maxPan, 0f)
                            zoomScale = newScale
                            panOffsetX = newPan
                        }
                    }
                    .pointerInput(points, zoomScale, panOffsetX) {
                        // 2. 点击柱条定位
                        detectTapGestures { offset ->
                            val totalWidth = size.width * zoomScale
                            val effectiveX = offset.x - panOffsetX
                            val barWidth = totalWidth / points.size.toFloat()
                            val index = (effectiveX / barWidth).toInt().coerceIn(0, points.size - 1)
                            onPointSelected(points[index])
                        }
                    }
            ) {
                val count = points.size
                val totalCanvasWidth = size.width * zoomScale
                val barWidth = (totalCanvasWidth / count.toFloat()).coerceAtLeast(1.2f)
                val maxH = size.height - 18.dp.toPx()

                // 标尺底线
                drawLine(
                    color = dialColor,
                    start = Offset(0f, maxH),
                    end = Offset(size.width, maxH),
                    strokeWidth = 1.dp.toPx()
                )

                // 绘制柱条
                for (i in 0 until count) {
                    val p = points[i]
                    val left = panOffsetX + i * barWidth
                    // 屏幕外剔除优化
                    if (left + barWidth < 0 || left > size.width) continue

                    val normalizedDb = ((p.volumeDb - 30f) / 55f).coerceIn(0.08f, 1f)
                    val barH = maxH * normalizedDb
                    val top = maxH - barH

                    val barColor = when (p.intensityLevel) {
                        SnoreIntensity.QUIET -> quietColor
                        SnoreIntensity.MILD -> lightColor
                        SnoreIntensity.LOUD -> loudColor
                        SnoreIntensity.EPIC -> epicColor
                    }

                    drawRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size((barWidth - 0.6f).coerceAtLeast(0.8f), barH)
                    )

                    // 绘制呼吸暂停警示倒三角标志 (Apnea Marker)
                    if (p.isApneaSuspected) {
                        val markerPath = Path().apply {
                            val cx = left + barWidth / 2
                            moveTo(cx, top - 2.dp.toPx())
                            lineTo(cx - 3.5.dp.toPx(), top - 8.dp.toPx())
                            lineTo(cx + 3.5.dp.toPx(), top - 8.dp.toPx())
                            close()
                        }
                        drawPath(path = markerPath, color = Color(0xFFFF5252))
                    }
                }

                // 绘制高亮光标指示线 (Cursor Line)
                if (selectedIndex in 0 until count) {
                    val cursorX = panOffsetX + selectedIndex * barWidth + barWidth / 2
                    if (cursorX in 0f..size.width) {
                        drawLine(
                            color = Color.White,
                            start = Offset(cursorX, 0f),
                            end = Offset(cursorX, maxH),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = blueColor,
                            radius = 4.dp.toPx(),
                            center = Offset(cursorX, 4.dp.toPx())
                        )
                    }
                }
            }
        }

        // 底部时间横轴标尺
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val startStr = if (startTimeMs > 0) timeFormatter.format(Date(startTimeMs)) else "23:00"
            val endStr = if (endTimeMs > 0) timeFormatter.format(Date(endTimeMs)) else "07:00"
            val midMs = if (startTimeMs > 0 && endTimeMs > startTimeMs) (startTimeMs + endTimeMs) / 2 else startTimeMs + 14400000L
            val midStr = timeFormatter.format(Date(midMs))

            Text(text = startStr, color = TextSecondary, fontSize = 10.sp)
            Text(text = midStr, color = TextSecondary, fontSize = 10.sp)
            Text(text = endStr, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
