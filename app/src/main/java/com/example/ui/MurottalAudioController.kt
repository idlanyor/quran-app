package com.example.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.local.AyatEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AudioPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val surahNomor: Int = 0,
    val surahNamaLatin: String = "",
    val currentAyatNomor: Int = 0,
    val totalAyatInQueue: Int = 0,
    val errorMessage: String? = null
)

class MurottalAudioController {

    private var mediaPlayer: MediaPlayer? = null
    private var currentQueue: List<AyatEntity> = emptyList()
    private var currentQueueIndex: Int = -1
    private var currentSurahLatin: String = ""
    private var autoNextEnabled: Boolean = true

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun setAutoNext(enabled: Boolean) {
        autoNextEnabled = enabled
    }

    fun playVerseFromSurah(
        surahNomor: Int,
        surahNamaLatin: String,
        ayats: List<AyatEntity>,
        startAyatNomor: Int
    ) {
        if (ayats.isEmpty()) return
        val index = ayats.indexOfFirst { it.nomorAyat == startAyatNomor }.coerceAtLeast(0)
        currentQueue = ayats
        currentQueueIndex = index
        currentSurahLatin = surahNamaLatin
        playCurrentQueueItem()
    }

    fun playFullSurah(
        surahNomor: Int,
        surahNamaLatin: String,
        ayats: List<AyatEntity>
    ) {
        if (ayats.isEmpty()) return
        currentQueue = ayats
        currentQueueIndex = 0
        currentSurahLatin = surahNamaLatin
        playCurrentQueueItem()
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
            } else {
                mp.start()
                _state.update { it.copy(isPlaying = true, errorMessage = null) }
            }
        } catch (_: Exception) {
            playCurrentQueueItem()
        }
    }

    fun playNextVerse() {
        if (currentQueue.isEmpty()) return
        if (currentQueueIndex + 1 < currentQueue.size) {
            currentQueueIndex++
            playCurrentQueueItem()
        } else {
            stop()
        }
    }

    fun playPreviousVerse() {
        if (currentQueue.isEmpty()) return
        if (currentQueueIndex - 1 >= 0) {
            currentQueueIndex--
            playCurrentQueueItem()
        } else {
            playCurrentQueueItem()
        }
    }

    fun stop() {
        releasePlayer()
        currentQueue = emptyList()
        currentQueueIndex = -1
        _state.value = AudioPlaybackState()
    }

    private fun playCurrentQueueItem() {
        val item = currentQueue.getOrNull(currentQueueIndex) ?: run {
            stop()
            return
        }
        val url = item.audioUrl.trim()
        if (url.isBlank()) {
            _state.update {
                it.copy(
                    isActive = true,
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Audio tidak tersedia untuk ayat ini"
                )
            }
            return
        }

        releasePlayer()

        _state.value = AudioPlaybackState(
            isActive = true,
            isPlaying = false,
            isBuffering = true,
            surahNomor = item.surahNomor,
            surahNamaLatin = currentSurahLatin,
            currentAyatNomor = item.nomorAyat,
            totalAyatInQueue = currentQueue.size,
            errorMessage = null
        )

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    player.start()
                    _state.update {
                        it.copy(isPlaying = true, isBuffering = false, errorMessage = null)
                    }
                }
                setOnCompletionListener {
                    if (autoNextEnabled && currentQueueIndex + 1 < currentQueue.size) {
                        currentQueueIndex++
                        playCurrentQueueItem()
                    } else {
                        _state.update { it.copy(isPlaying = false, isBuffering = false) }
                    }
                }
                setOnErrorListener { _, _, _ ->
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = "Gagal memutar audio murottal (periksa jaringan)"
                        )
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Gagal menyiapkan pemutar audio"
                )
            }
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }
}
