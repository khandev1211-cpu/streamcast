package com.streamcast.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlayerManager
import com.streamcast.core.player.model.SubtitleSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SubtitleUiState {
    object Idle : SubtitleUiState()
    object Loading : SubtitleUiState()
    data class Active(val segments: List<SubtitleSegment>) : SubtitleUiState()
    data class Error(val message: String) : SubtitleUiState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.playerState
    val abRepeatRange = playerManager.abRepeatRange

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _decoderType = MutableStateFlow("HW")
    val decoderType: StateFlow<String> = _decoderType.asStateFlow()

    // Sleep Timer
    private val _sleepTimerMillis = MutableStateFlow<Long?>(null)
    val sleepTimerMillis: StateFlow<Long?> = _sleepTimerMillis.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Subtitles
    private val _subtitleUiState = MutableStateFlow<SubtitleUiState>(SubtitleUiState.Idle)
    val subtitleUiState: StateFlow<SubtitleUiState> = _subtitleUiState.asStateFlow()

    private val _activeSubtitleSegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val activeSubtitleSegments: StateFlow<List<SubtitleSegment>> = _activeSubtitleSegments.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.value?.currentPosition ?: 0L
                _duration.value = player.value?.duration ?: 0L
                delay(500)
            }
        }
    }

    fun play(source: MediaSource) {
        playerManager.play(source)
    }

    fun pause() {
        playerManager.pause()
    }

    fun resume() {
        playerManager.resume()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.value?.setPlaybackSpeed(speed)
    }

    fun togglePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f, 0.75f)
        val currentIndex = speeds.indexOf(_playbackSpeed.value)
        val nextIndex = (currentIndex + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }

    fun toggleDecoder() {
        _decoderType.value = if (_decoderType.value == "HW") "SW" else "HW"
    }

    // A-B Repeat
    fun setAbRepeat(startMs: Long, endMs: Long) {
        playerManager.setAbRepeatRange(startMs, endMs)
    }

    fun clearAbRepeat() {
        playerManager.clearAbRepeatRange()
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerMillis.value = null
            return
        }
        val targetMillis = minutes * 60 * 1000L
        _sleepTimerMillis.value = targetMillis
        sleepTimerJob = viewModelScope.launch {
            var remaining = targetMillis
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerMillis.value = remaining
            }
            pause()
            _sleepTimerMillis.value = null
        }
    }

    fun generateSubtitles(language: String) {
        viewModelScope.launch {
            _subtitleUiState.value = SubtitleUiState.Loading
            // TODO: Call SubtitleRepository via UseCase
            delay(2000) // Simulate network
            val mockSegments = listOf(
                SubtitleSegment(0f, 5f, "Hello, welcome to StreamCast!"),
                SubtitleSegment(5.5f, 10f, "This is an AI-generated subtitle.")
            )
            _activeSubtitleSegments.value = mockSegments
            _subtitleUiState.value = SubtitleUiState.Active(mockSegments)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We don't release the player here if we want background play
        // But we should probably provide a way to stop it
    }
}
