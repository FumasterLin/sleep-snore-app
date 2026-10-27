package com.mytools.sleepsnore.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.mytools.sleepsnore.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SoundscapeItem(
    val id: String,
    val name: String,
    val description: String,
    @RawRes val soundResId: Int,
    @DrawableRes val imageResId: Int
)

class SoundscapeManager(private val context: Context) {

    companion object {
        val ALL_SOUNDSCAPES = listOf(
            SoundscapeItem("waves", "海洋波浪", "柔和的海浪拍岸声，抚平焦虑助眠", R.raw.soundscape_waves, R.drawable.soundscape_waves),
            SoundscapeItem("thunderstorm", "夏夜雷雨", "远方沉闷雷鸣与连绵淅沥雨滴", R.raw.soundscape_thunderstorm, R.drawable.soundscape_thunderstorm),
            SoundscapeItem("forest", "森林虫鸣", "静谧山林中的夜风与蟋蟀鸣叫", R.raw.soundscape_forest, R.drawable.soundscape_forest),
            SoundscapeItem("campfire", "温暖篝火", "木柴静静燃烧的噼啪声与微风", R.raw.soundscape_campfire, R.drawable.soundscape_fire),
            SoundscapeItem("rain", "窗外雨声", "雨滴敲击窗棂的均匀白噪音", R.raw.soundscape_rain, R.drawable.soundscape_rain),
            SoundscapeItem("stream", "高山溪流", "清冽泉水流经鹅卵石的潺潺水声", R.raw.soundscape_stream, R.drawable.soundscape_stream),
            SoundscapeItem("light_rain", "轻柔细雨", "温和微细的春雨沙沙声", R.raw.soundscape_light_rain, R.drawable.soundscape_light_rain),
            SoundscapeItem("waterfall", "清凉瀑布", "气势澎湃的宽频瀑布水雾声", R.raw.soundscape_waterfall, R.drawable.soundscape_waterfall),
            SoundscapeItem("brown_noise", "棕色噪音", "低频浑厚更深沉的深度入眠声波", R.raw.soundscape_brown_noise, R.drawable.soundscape_brown_noise),
            SoundscapeItem("pink_noise", "粉红噪音", "符合自然界 1/f 律的平衡脑波白噪音", R.raw.soundscape_pink_noise, R.drawable.soundscape_pink_noise)
        )
    }

    private val activePlayers = mutableMapOf<String, MediaPlayer>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _currentSoundscape = MutableStateFlow<SoundscapeItem?>(null)
    val currentSoundscape: StateFlow<SoundscapeItem?> = _currentSoundscape.asStateFlow()

    private val _activeTracks = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeTracks: StateFlow<Map<String, Float>> = _activeTracks.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    fun toggleTrack(item: SoundscapeItem, timerMinutes: Int = 30) {
        val currentMap = _activeTracks.value.toMutableMap()
        if (currentMap.containsKey(item.id)) {
            // Remove track
            activePlayers[item.id]?.let { player ->
                try {
                    player.stop()
                    player.release()
                } catch (e: Exception) {}
            }
            activePlayers.remove(item.id)
            currentMap.remove(item.id)
        } else {
            // Add track
            try {
                val player = MediaPlayer.create(context, item.soundResId)?.apply {
                    isLooping = true
                    setVolume(0.7f, 0.7f)
                    start()
                }
                if (player != null) {
                    activePlayers[item.id] = player
                    currentMap[item.id] = 0.7f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _activeTracks.value = currentMap
        val hasActive = currentMap.isNotEmpty()
        _isPlaying.value = hasActive
        _currentSoundscape.value = if (hasActive) {
            ALL_SOUNDSCAPES.find { it.id == currentMap.keys.first() }
        } else null

        if (hasActive && _remainingSeconds.value <= 0) {
            startTimer(timerMinutes)
        } else if (!hasActive) {
            timerJob?.cancel()
            _remainingSeconds.value = 0
        }
    }

    fun setTrackVolume(soundId: String, vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        val currentMap = _activeTracks.value.toMutableMap()
        if (currentMap.containsKey(soundId)) {
            currentMap[soundId] = clamped
            _activeTracks.value = currentMap
            activePlayers[soundId]?.setVolume(clamped, clamped)
        }
    }

    fun applyPreset(presetTracks: Map<String, Float>, timerMinutes: Int = 30) {
        stop()
        val newMap = mutableMapOf<String, Float>()
        presetTracks.forEach { (id, vol) ->
            val sound = ALL_SOUNDSCAPES.find { it.id == id }
            if (sound != null) {
                try {
                    val player = MediaPlayer.create(context, sound.soundResId)?.apply {
                        isLooping = true
                        setVolume(vol, vol)
                        start()
                    }
                    if (player != null) {
                        activePlayers[id] = player
                        newMap[id] = vol
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        _activeTracks.value = newMap
        val hasActive = newMap.isNotEmpty()
        _isPlaying.value = hasActive
        _currentSoundscape.value = if (hasActive) ALL_SOUNDSCAPES.find { it.id == newMap.keys.first() } else null
        if (hasActive) {
            startTimer(timerMinutes)
        }
    }

    fun play(item: SoundscapeItem, timerMinutes: Int = 30) {
        toggleTrack(item, timerMinutes)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            // Pause all
            activePlayers.values.forEach {
                try { if (it.isPlaying) it.pause() } catch (e: Exception) {}
            }
            _isPlaying.value = false
        } else {
            // Resume all
            activePlayers.values.forEach {
                try { it.start() } catch (e: Exception) {}
            }
            _isPlaying.value = activePlayers.isNotEmpty()
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        activePlayers.values.forEach {
            try { it.setVolume(clamped, clamped) } catch (e: Exception) {}
        }
    }

    fun startTimer(minutes: Int) {
        timerJob?.cancel()
        if (minutes <= 0) {
            _remainingSeconds.value = 0
            return
        }

        _remainingSeconds.value = minutes * 60
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
                // 临近结束 15 秒逐渐淡出
                if (_remainingSeconds.value in 1..15) {
                    val fadeRatio = _remainingSeconds.value / 15f
                    _activeTracks.value.forEach { (id, baseVol) ->
                        val fadeVol = baseVol * fadeRatio
                        activePlayers[id]?.setVolume(fadeVol, fadeVol)
                    }
                }
            }
            stop()
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        activePlayers.values.forEach { player ->
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {}
        }
        activePlayers.clear()
        _activeTracks.value = emptyMap()
        _isPlaying.value = false
        _currentSoundscape.value = null
        _remainingSeconds.value = 0
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
