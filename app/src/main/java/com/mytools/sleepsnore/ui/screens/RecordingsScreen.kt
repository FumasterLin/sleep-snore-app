package com.mytools.sleepsnore.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mytools.sleepsnore.data.db.SleepDao
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.data.model.SnoreIntensity
import com.mytools.sleepsnore.ui.components.AudioTrimmerDialog
import com.mytools.sleepsnore.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsScreen(
    sleepDao: SleepDao
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSamples by sleepDao.getAllAudioSamples().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, EPIC, LOUD, MILD, STARRED
    var currentPlayingId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playProgress by remember { mutableStateOf(0f) }
    var sampleToTrim by remember { mutableStateOf<AudioSample?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // 播放进度轮询
    LaunchedEffect(currentPlayingId) {
        while (currentPlayingId != null) {
            val player = mediaPlayer
            if (player != null && player.isPlaying && player.duration > 0) {
                playProgress = player.currentPosition.toFloat() / player.duration
            }
            delay(100)
        }
    }

    val filteredSamples = remember(allSamples, selectedFilter) {
        when (selectedFilter) {
            "EPIC" -> allSamples.filter { it.level == SnoreIntensity.EPIC }
            "LOUD" -> allSamples.filter { it.level == SnoreIntensity.LOUD }
            "MILD" -> allSamples.filter { it.level == SnoreIntensity.MILD }
            "STARRED" -> allSamples.filter { it.isFavorite }
            else -> allSamples
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 顶栏
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "打鼾录音片库 (Recordings)",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "共收录 ${allSamples.size} 段典型高保真鼾声切片，支持点播回放与导出",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // 筛选标签栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "全部 (${allSamples.size})",
                "EPIC" to "超大声",
                "LOUD" to "大声",
                "MILD" to "轻声",
                "STARRED" to "★ 收藏"
            ).forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryAccent else DarkCard)
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSamples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MicNone,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("当前筛选条件下无录音切片", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSamples) { sample ->
                    val isPlaying = currentPlayingId == sample.id

                    val levelColor = when (sample.level) {
                        SnoreIntensity.QUIET -> Color(0xFF4CAF50)
                        SnoreIntensity.MILD -> Color(0xFFFFEB3B)
                        SnoreIntensity.LOUD -> Color(0xFFFF9800)
                        SnoreIntensity.EPIC -> Color(0xFFF44336)
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 标题行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(levelColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(sample.timestampMs))
                                    Text(text = timeStr, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // 峰值分贝与等级标签
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(levelColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${sample.peakDb.toInt()} dB · ${sample.level.label}",
                                        color = levelColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 播放器控制栏与进度条
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 播放/暂停按钮
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            currentPlayingId = null
                                        } else {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            try {
                                                val player = MediaPlayer().apply {
                                                    setDataSource(sample.audioPath)
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        currentPlayingId = null
                                                        playProgress = 0f
                                                    }
                                                }
                                                mediaPlayer = player
                                                currentPlayingId = sample.id
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 进度条
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    if (isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(playProgress)
                                                .background(PrimaryAccent)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text("${sample.durationSeconds.toInt()}s", color = TextSecondary, fontSize = 11.sp)

                                // 收藏加星
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            sleepDao.setSampleFavorite(sample.id, !sample.isFavorite)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sample.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "收藏",
                                        tint = if (sample.isFavorite) Color(0xFFFFD700) else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 剪辑切片
                                IconButton(
                                    onClick = { sampleToTrim = sample },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCut, contentDescription = "剪辑", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                                }

                                // 导出分享
                                IconButton(
                                    onClick = {
                                        shareAudioFile(context, sample.audioPath)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "分享", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }

                                // 删除单条
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            File(sample.audioPath).delete()
                                            sleepDao.deleteAudioSample(sample)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "删除", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    sampleToTrim?.let { sample ->
        AudioTrimmerDialog(
            sample = sample,
            onDismiss = { sampleToTrim = null }
        )
    }
}

private fun shareAudioFile(context: Context, audioPath: String) {
    try {
        val file = File(audioPath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享打鼾音频切片"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
