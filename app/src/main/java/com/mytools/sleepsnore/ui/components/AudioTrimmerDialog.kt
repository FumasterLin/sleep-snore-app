package com.mytools.sleepsnore.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.mytools.sleepsnore.audio.AudioConfig
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.ui.theme.DarkCard
import com.mytools.sleepsnore.ui.theme.DarkSurface
import com.mytools.sleepsnore.ui.theme.PrimaryAccent
import com.mytools.sleepsnore.ui.theme.TextPrimary
import com.mytools.sleepsnore.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 鼾声音频精准无损剪辑与快速标记弹窗
 * 支持毫秒级选择打鼾最典型区间，直接切片并导出分享给医生
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerDialog(
    sample: AudioSample,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val file = remember(sample) { File(sample.audioPath) }
    val totalSec = remember(sample, file) {
        if (file.exists() && file.length() > 44) {
            val pcmBytes = file.length() - 44
            (pcmBytes / (AudioConfig.SAMPLE_RATE * 2)).toFloat().coerceAtLeast(3f)
        } else sample.durationSeconds.toFloat().coerceAtLeast(3f)
    }

    var sliderRange by remember { mutableStateOf(0f..minOf(totalSec, 6f)) }
    var isPreviewing by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    fun stopPreview() {
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
        isPreviewing = false
    }

    fun startPreview() {
        stopPreview()
        if (!file.exists()) return

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                val startMs = (sliderRange.start * 1000).toInt()
                seekTo(startMs)
                start()
            }
            previewPlayer = player
            isPreviewing = true

            val endMs = (sliderRange.endInclusive * 1000).toInt()
            coroutineScope.launch {
                while (isPreviewing && player.isPlaying) {
                    if (player.currentPosition >= endMs) {
                        stopPreview()
                        break
                    }
                    delay(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPreview()
        }
    }

    fun exportAndShare() {
        stopPreview()
        if (!file.exists()) return

        try {
            val startSec = sliderRange.start
            val endSec = sliderRange.endInclusive
            val bytesPerSecond = AudioConfig.SAMPLE_RATE * 2 // 16kHz, 16-bit mono = 32000 bytes/sec
            val startByteOffset = 44L + (startSec * bytesPerSecond).toLong()
            val endByteOffset = minOf(file.length(), 44L + (endSec * bytesPerSecond).toLong())
            val pcmDataLength = maxOf(0L, endByteOffset - startByteOffset)

            val outFile = File(context.cacheDir, "snore_clip_${sample.id}_${startSec.toInt()}s-${endSec.toInt()}s.wav")
            RandomAccessFile(file, "r").use { reader ->
                reader.seek(startByteOffset)
                val buffer = ByteArray(pcmDataLength.toInt())
                reader.readFully(buffer)

                // 写入新的 44 字节标准 WAV 头
                FileOutputStream(outFile).use { fos ->
                    val header = buildWavHeader(pcmDataLength.toInt())
                    fos.write(header)
                    fos.write(buffer)
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "睡眠打鼾重点音频切片")
                putExtra(Intent.EXTRA_TEXT, "这是夜间打鼾典型片段 (${startSec.toInt()}s ~ ${endSec.toInt()}s，峰值分贝 ${sample.peakDb.toInt()}dB)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享打鼾切片"))
            onDismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(onDismissRequest = {
        stopPreview()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("✂️ 鼾声片段精准剪辑", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("截取打鼾最明显的数秒，直发专科医生", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = { stopPreview(); onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 区间时长指示
                val clipDuration = sliderRange.endInclusive - sliderRange.start
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.1f".format(sliderRange.start)}s  ➔  ${"%.1f".format(sliderRange.endInclusive)}s",
                            color = PrimaryAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "切片持续时长: ${"%.1f".format(clipDuration)} 秒",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 双向滑块 RangeSlider
                RangeSlider(
                    value = sliderRange,
                    onValueChange = { range ->
                        sliderRange = range
                    },
                    valueRange = 0f..totalSec,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 控制按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isPreviewing) stopPreview() else startPreview()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = PrimaryAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPreviewing) "停止" else "试听", color = TextPrimary)
                    }

                    Button(
                        onClick = { exportAndShare() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("导出并分享", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun buildWavHeader(pcmDataLen: Int): ByteArray {
    val totalDataLen = pcmDataLen + 36
    val sampleRate = AudioConfig.SAMPLE_RATE
    val channels = 1
    val byteRate = sampleRate * channels * 2

    val header = ByteArray(44)
    val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

    bb.put("RIFF".toByteArray())
    bb.putInt(totalDataLen)
    bb.put("WAVE".toByteArray())
    bb.put("fmt ".toByteArray())
    bb.putInt(16) // SubChunk1Size (16 for PCM)
    bb.putShort(1.toShort()) // AudioFormat (1 for PCM)
    bb.putShort(channels.toShort())
    bb.putInt(sampleRate)
    bb.putInt(byteRate)
    bb.putShort((channels * 2).toShort()) // BlockAlign
    bb.putShort(16.toShort()) // BitsPerSample
    bb.put("data".toByteArray())
    bb.putInt(pcmDataLen)

    return header
}
