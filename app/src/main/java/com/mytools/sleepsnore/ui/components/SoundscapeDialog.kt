package com.mytools.sleepsnore.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytools.sleepsnore.audio.SoundscapeItem
import com.mytools.sleepsnore.audio.SoundscapeManager
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary

@Composable
fun SoundscapeDialog(
    soundscapeManager: SoundscapeManager,
    onDismiss: () -> Unit
) {
    val activeTracks by soundscapeManager.activeTracks.collectAsState()
    val isPlaying by soundscapeManager.isPlaying.collectAsState()
    val volume by soundscapeManager.volume.collectAsState()
    val remainingSec by soundscapeManager.remainingSeconds.collectAsState()

    var selectedTimerMinutes by remember { mutableStateOf(30) }

    val presets = remember {
        listOf(
            Triple("🌧️ 雨夜壁炉", mapOf("rain" to 0.75f, "campfire" to 0.5f), Color(0xFFFF9800)),
            Triple("🌊 海滨微风", mapOf("waves" to 0.7f, "forest" to 0.45f), Color(0xFF00E5FF)),
            Triple("🌲 溪流细雨", mapOf("stream" to 0.65f, "light_rain" to 0.5f), Color(0xFF4CAF50)),
            Triple("🧘 深度安睡", mapOf("brown_noise" to 0.6f, "pink_noise" to 0.45f), Color(0xFF9C27B0))
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 背景水波流体视效 (播放时动态呼吸)
                if (isPlaying) {
                    FluidWaveVisualizer(
                        modifier = Modifier.fillMaxSize(),
                        amplitudeMultiplier = 1.1f,
                        baseColor = Color(0xFF00E5FF)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // 顶部标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎛️ 多轨助眠声景混音器",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeTracks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimaryAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${activeTracks.size} 轨混音中", color = PrimaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "支持多音轨自由混合与自然潮汐呼吸水纹",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 常用经典助眠预设组合
                    Text("经典多轨混音预设:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (title, map, accent) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCard)
                                    .border(0.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        soundscapeManager.applyPreset(map, selectedTimerMinutes)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 定时器选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动淡出停止:", color = TextSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                val isSelected = selectedTimerMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryAccent else DarkCard)
                                        .clickable {
                                            selectedTimerMinutes = mins
                                            if (isPlaying) {
                                                soundscapeManager.startTimer(mins)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${mins}分",
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    if (remainingSec > 0) {
                        val m = remainingSec / 60
                        val s = remainingSec % 60
                        Text(
                            text = "倒计时: %02d:%02d 缓淡关闭".format(m, s),
                            color = PrimaryAccent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 网格列表 (点击卡片切换选中/加入混音)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(SoundscapeManager.ALL_SOUNDSCAPES) { item ->
                            val isActive = activeTracks.containsKey(item.id)
                            val trackVol = activeTracks[item.id] ?: 0.7f

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .border(
                                        width = if (isActive) 2.dp else 0.dp,
                                        color = if (isActive) PrimaryAccent else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        soundscapeManager.toggleTrack(item, selectedTimerMinutes)
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = item.imageResId),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = if (isActive) 0.4f else 0.6f))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(13.dp))
                                                    .background(if (isActive) PrimaryAccent else Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isActive) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (isActive) {
                                            Column {
                                                Text("单轨音量 ${(trackVol * 100).toInt()}%", color = PrimaryAccent, fontSize = 9.sp)
                                                Slider(
                                                    value = trackVol,
                                                    onValueChange = { soundscapeManager.setTrackVolume(item.id, it) },
                                                    modifier = Modifier.height(20.dp),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = PrimaryAccent,
                                                        activeTrackColor = PrimaryAccent
                                                    )
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = item.description,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 9.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 底部控制
                    if (isPlaying) {
                        Button(
                            onClick = { soundscapeManager.stop() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("停止全部播放", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
