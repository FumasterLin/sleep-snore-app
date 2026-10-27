package com.mytools.sleepsnore.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.audio.WaveformExtractor
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * SnoreLab 官方 1:1 音频波形播放器条 (Audio Player Bar)
 * 对应官方 fragment_statistics_player.xml
 */
@Composable
fun AudioPlayerBar(
    sample: AudioSample?,
    onClose: () -> Unit,
    onToggleFavorite: (AudioSample) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sample == null) return

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(1) }
    var volumeLevel by remember { mutableStateOf(0.85f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var realWaveform by remember(sample.id) { mutableStateOf<FloatArray?>(null) }

    // 加载真实 PCM 波形包络
    LaunchedEffect(sample.id, sample.audioPath) {
        realWaveform = WaveformExtractor.extractWaveform(sample.audioPath, 42)
    }

    // 初始化并播放
    LaunchedEffect(sample.id, sample.audioPath) {
        mediaPlayer?.release()
        isPlaying = false
        currentPositionMs = 0

        val file = File(sample.audioPath)
        if (file.exists()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(sample.audioPath)
                    prepare()
                    setVolume(volumeLevel, volumeLevel)
                    totalDurationMs = duration.coerceAtLeast(1)
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = 0
                    }
                }
                mediaPlayer = mp
                mp.start()
                isPlaying = true
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // 播放进度刷新
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                currentPositionMs = mp.currentPosition
            }
            kotlinx.coroutines.delay(80)
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val recordTimeStr = timeFormatter.format(Date(sample.timestampMs))

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkerBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, DialOuterCircle)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 上半部：左侧垂直音量滑条 + 中间波形与时间标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧垂直音量调节
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "音量",
                            tint = BlueHighlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Slider(
                            value = volumeLevel,
                            onValueChange = {
                                volumeLevel = it
                                mediaPlayer?.setVolume(it, it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .width(36.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = BlueHighlight,
                                activeTrackColor = BlueHighlight,
                                inactiveTrackColor = DialOuterCircle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(DialOuterCircle))
                    Spacer(modifier = Modifier.width(12.dp))

                    // 中间波形与标签
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(sample.level.colorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$recordTimeStr · ${sample.level.title} (${sample.peakDb.toInt()}dB)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 收藏图钉
                            IconButton(
                                onClick = { onToggleFavorite(sample) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (sample.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "收藏",
                                    tint = if (sample.isFavorite) SnoreLoud else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 绘制真实音频波形包络
                        val progressFraction = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        val playedColor = BlueHighlight
                        val unplayedColor = DialOuterCircle
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            val barCount = 42
                            val barWidth = size.width / (barCount * 1.6f)
                            val spacing = barWidth * 0.6f
                            val bars = realWaveform ?: FloatArray(barCount) { 0.35f }

                            for (i in 0 until barCount) {
                                val x = i * (barWidth + spacing)
                                val heightFraction = bars.getOrElse(i) { 0.35f }
                                val barH = size.height * heightFraction
                                val topY = (size.height - barH) / 2

                                val isPlayed = (x / size.width) <= progressFraction
                                val barColor = if (isPlayed) playedColor else unplayedColor

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, topY),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }

                            // 垂直高亮光标
                            val cursorX = size.width * progressFraction
                            drawLine(
                                color = Color.White,
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // 进度与总时长
                        val curSec = currentPositionMs / 1000
                        val totalSec = totalDurationMs / 1000
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "%02d:%02d".format(curSec / 60, curSec % 60),
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "%02d:%02d".format(totalSec / 60, totalSec % 60),
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // 分割线
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DialOuterCircle))

                // 下半部：5 经典控制键 (图钉、快退5s、播放/暂停、快进5s、关闭)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(LighterBackground),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 图钉/星标
                    IconButton(onClick = { onToggleFavorite(sample) }) {
                        Icon(
                            imageVector = if (sample.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "收藏",
                            tint = if (sample.isFavorite) SnoreLoud else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. 快退 5 秒
                    IconButton(onClick = {
                        val mp = mediaPlayer ?: return@IconButton
                        val target = (mp.currentPosition - 5000).coerceAtLeast(0)
                        mp.seekTo(target)
                        currentPositionMs = target
                    }) {
                        Icon(
                            Icons.Default.Replay5,
                            contentDescription = "后退5秒",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3. 核心 播放/暂停
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BlueHighlight)
                            .clickable {
                                val mp = mediaPlayer ?: return@clickable
                                if (isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 4. 快进 5 秒
                    IconButton(onClick = {
                        val mp = mediaPlayer ?: return@IconButton
                        val target = (mp.currentPosition + 5000).coerceAtMost(totalDurationMs)
                        mp.seekTo(target)
                        currentPositionMs = target
                    }) {
                        Icon(
                            Icons.Default.Forward5,
                            contentDescription = "快进5秒",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 5. 关闭播放条
                    IconButton(onClick = {
                        mediaPlayer?.pause()
                        isPlaying = false
                        onClose()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
