package com.irengcloud.quran.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.irengcloud.quran.data.local.AyatEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.FileInputStream

data class AudioPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val surahNomor: Int = 0,
    val surahNamaLatin: String = "",
    val currentAyatNomor: Int = 0,
    val totalAyatInQueue: Int = 0,
    val errorMessage: String? = null,
    val shuffleEnabled: Boolean = false,
    val repeatVerse: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val usesLocalAudio: Boolean = false
)

data class AudioProgress(val positionMs: Int = 0, val durationMs: Int = 0)

class MurottalAudioController(
    private val localAudioPath: (Int, Int) -> String? = { _, _ -> null }
) {
    private var mediaPlayer: MediaPlayer? = null
    private var originalQueue: List<AyatEntity> = emptyList()
    private var currentQueue: List<AyatEntity> = emptyList()
    private var currentQueueIndex: Int = -1
    private var currentSurahLatin: String = ""
    private var autoNextEnabled: Boolean = true

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(AudioProgress())
    val progress: StateFlow<AudioProgress> = _progress.asStateFlow()

    fun setAutoNext(enabled: Boolean) {
        autoNextEnabled = enabled
    }

    fun setRepeatVerse(enabled: Boolean) {
        _state.update { it.copy(repeatVerse = enabled) }
        if (!_state.value.isBuffering) runCatching { mediaPlayer?.isLooping = enabled }
    }

    fun setShuffle(enabled: Boolean) {
        if (enabled == _state.value.shuffleEnabled) return
        val current = currentQueue.getOrNull(currentQueueIndex)
        if (current != null) {
            if (enabled) {
                currentQueue = currentQueue.take(currentQueueIndex + 1) +
                    currentQueue.drop(currentQueueIndex + 1).shuffled()
            } else {
                currentQueue = originalQueue
                currentQueueIndex = currentQueue.indexOf(current)
            }
        }
        _state.update {
            it.copy(shuffleEnabled = enabled, hasNext = hasNext(), hasPrevious = currentQueueIndex > 0)
        }
    }

    fun playVerseFromSurah(
        surahNomor: Int,
        surahNamaLatin: String,
        ayats: List<AyatEntity>,
        startAyatNomor: Int
    ) {
        if (ayats.isEmpty() || ayats.first().surahNomor != surahNomor) return
        val index = ayats.indexOfFirst { it.nomorAyat == startAyatNomor }.coerceAtLeast(0)
        originalQueue = ayats
        if (_state.value.shuffleEnabled) {
            currentQueue = listOf(ayats[index]) + ayats.filterIndexed { i, _ -> i != index }.shuffled()
            currentQueueIndex = 0
        } else {
            currentQueue = ayats
            currentQueueIndex = index
        }
        currentSurahLatin = surahNamaLatin
        playCurrentQueueItem()
    }

    fun playFullSurah(surahNomor: Int, surahNamaLatin: String, ayats: List<AyatEntity>) {
        playVerseFromSurah(surahNomor, surahNamaLatin, ayats, ayats.firstOrNull()?.nomorAyat ?: return)
    }

    fun togglePlayPause() {
        if (_state.value.isBuffering) return
        val player = mediaPlayer
        if (player == null || _state.value.errorMessage != null) {
            if (currentQueue.isNotEmpty()) playCurrentQueueItem()
            return
        }
        try {
            val play = !player.isPlaying
            if (play) player.start() else player.pause()
            _state.update { it.copy(isPlaying = play) }
            refreshProgress()
        } catch (_: Exception) {
            playCurrentQueueItem()
        }
    }

    fun seekTo(positionMs: Int) {
        val duration = _progress.value.durationMs
        if (duration <= 0 || _state.value.isBuffering) return
        val target = positionMs.coerceIn(0, duration)
        runCatching {
            val player = mediaPlayer ?: return
            player.seekTo(target)
            _progress.update { it.copy(positionMs = target) }
        }
    }

    fun refreshProgress() {
        val player = mediaPlayer ?: return
        if (_state.value.isBuffering || _state.value.errorMessage != null) return
        runCatching {
            _progress.value = AudioProgress(player.currentPosition.coerceAtLeast(0), player.duration.coerceAtLeast(0))
        }
    }

    fun playNextVerse() {
        if (currentQueue.isEmpty()) return
        if (hasNext()) {
            currentQueueIndex++
            playCurrentQueueItem()
        } else {
            stop()
        }
    }

    fun playPreviousVerse() {
        if (currentQueue.isEmpty()) return
        if (currentQueueIndex > 0) currentQueueIndex--
        playCurrentQueueItem()
    }

    fun stop() {
        releasePlayer()
        originalQueue = emptyList()
        currentQueue = emptyList()
        currentQueueIndex = -1
        _progress.value = AudioProgress()
        _state.update { AudioPlaybackState(shuffleEnabled = it.shuffleEnabled, repeatVerse = it.repeatVerse) }
    }

    private fun hasNext() = currentQueueIndex + 1 < currentQueue.size

    private fun playCurrentQueueItem() {
        val item = currentQueue.getOrNull(currentQueueIndex) ?: return
        releasePlayer()
        _progress.value = AudioProgress()
        val localPath = localAudioPath(item.surahNomor, item.nomorAyat)
        val url = item.audioUrl.trim()
        val available = localPath != null || url.isNotBlank()
        _state.update {
            AudioPlaybackState(
                isActive = true,
                isBuffering = available,
                surahNomor = item.surahNomor,
                surahNamaLatin = currentSurahLatin,
                currentAyatNomor = item.nomorAyat,
                totalAyatInQueue = currentQueue.size,
                errorMessage = if (available) null else "Audio tidak tersedia untuk ayat ini",
                shuffleEnabled = it.shuffleEnabled,
                repeatVerse = it.repeatVerse,
                hasNext = hasNext(),
                hasPrevious = currentQueueIndex > 0,
                usesLocalAudio = localPath != null
            )
        }
        if (!available) return

        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            if (localPath != null) {
                FileInputStream(localPath).use { player.setDataSource(it.fd) }
            } else {
                player.setDataSource(url)
            }
            player.setOnPreparedListener { prepared ->
                if (mediaPlayer !== prepared) return@setOnPreparedListener
                try {
                    prepared.isLooping = _state.value.repeatVerse
                    prepared.start()
                    _state.update { it.copy(isPlaying = true, isBuffering = false) }
                    refreshProgress()
                } catch (_: Exception) {
                    playbackFailed()
                }
            }
            player.setOnSeekCompleteListener { seeking ->
                if (mediaPlayer === seeking) refreshProgress()
            }
            player.setOnCompletionListener { completed ->
                if (mediaPlayer !== completed) return@setOnCompletionListener
                if (autoNextEnabled && hasNext()) {
                    currentQueueIndex++
                    playCurrentQueueItem()
                } else {
                    _state.update { it.copy(isPlaying = false, isBuffering = false) }
                    refreshProgress()
                }
            }
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) playbackFailed()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            playbackFailed()
        }
    }

    private fun playbackFailed() {
        releasePlayer()
        _progress.value = AudioProgress()
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = if (it.usesLocalAudio) "Audio tersimpan tidak dapat diputar"
                    else "Gagal memutar audio murottal. Periksa jaringan."
            )
        }
    }

    private fun releasePlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        runCatching { player?.release() }
    }
}
