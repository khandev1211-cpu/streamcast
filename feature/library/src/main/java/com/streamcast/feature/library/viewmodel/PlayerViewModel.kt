package com.streamcast.feature.library.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.database.dao.LocalMediaDao
import com.streamcast.core.database.entities.LocalMedia
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
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
    private val playerManager: PlayerManager,
    private val localMediaDao: LocalMediaDao
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

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isTimeRemainingMode = MutableStateFlow(false)
    val isTimeRemainingMode: StateFlow<Boolean> = _isTimeRemainingMode.asStateFlow()

    // Subtitle Styling
    private val _subtitleFontSize = MutableStateFlow(18f)
    val subtitleFontSize = _subtitleFontSize.asStateFlow()

    private val _subtitleColor = MutableStateFlow(0xFFFFFFFF) // White
    val subtitleColor = _subtitleColor.asStateFlow()

    private val _subtitleBackgroundOpacity = MutableStateFlow(0.6f)
    val subtitleBackgroundOpacity = _subtitleBackgroundOpacity.asStateFlow()

    private val _currentFolderItems = MutableStateFlow<List<MediaSource>>(emptyList())
    private var currentMediaIndex = -1

    // Subtitle Sync Offset (ms)
    private val _subtitleSyncOffset = MutableStateFlow(0L)
    val subtitleSyncOffset: StateFlow<Long> = _subtitleSyncOffset.asStateFlow()

    // Resume Position
    private val _resumePosition = MutableStateFlow<Long?>(null)
    val resumePosition: StateFlow<Long?> = _resumePosition.asStateFlow()

    // Sleep Timer
    private val _sleepTimerMillis = MutableStateFlow<Long?>(null)
    val sleepTimerMillis: StateFlow<Long?> = _sleepTimerMillis.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Subtitles
    private val _subtitleUiState = MutableStateFlow<SubtitleUiState>(SubtitleUiState.Idle)
    val subtitleUiState: StateFlow<SubtitleUiState> = _subtitleUiState.asStateFlow()

    private val _activeSegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val activeSegments: StateFlow<List<SubtitleSegment>> = _activeSegments.asStateFlow()

    private var lastSavedPosition = 0L

    init {
        viewModelScope.launch {
            while (true) {
                val playerInstance = player.value
                if (playerInstance != null) {
                    _currentPosition.value = playerInstance.currentPosition
                    _duration.value = playerInstance.duration
                    
                    // Periodically save position (every 10 seconds)
                    if (Math.abs(_currentPosition.value - lastSavedPosition) > 10000) {
                        saveCurrentPosition()
                    }
                }
                delay(500)
            }
        }
    }

    fun play(source: MediaSource) {
        viewModelScope.launch {
            // Check for resume position
            val savedMedia = localMediaDao.getById(source.id)
            if (savedMedia != null && savedMedia.lastPositionMs > 5000) { // Only resume if > 5s
                _resumePosition.value = savedMedia.lastPositionMs
            }
            
            playerManager.play(source)
        }
    }

    fun confirmResume() {
        _resumePosition.value?.let { 
            seekTo(it)
            _resumePosition.value = null
        }
    }

    fun dismissResume() {
        _resumePosition.value = null
    }

    private fun saveCurrentPosition() {
        val currentSource = playerManager.playbackState.value.let { state ->
            when (state) {
                is PlaybackState.Playing -> state.source
                is PlaybackState.Paused -> state.source
                else -> null
            }
        }

        currentSource?.let { source ->
            val pos = _currentPosition.value
            lastSavedPosition = pos
            viewModelScope.launch {
                localMediaDao.insert(
                    LocalMedia(
                        id = source.id,
                        filePath = source.uri.toString(),
                        displayName = source.displayName,
                        durationMs = _duration.value,
                        lastPositionMs = pos,
                        lastPlayedAt = System.currentTimeMillis(),
                        addedAt = System.currentTimeMillis() // TODO: get from source if possible
                    )
                )
            }
        }
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

    fun setVolume(volume: Float) {
        _volume.value = volume
        playerManager.setVolume(volume)
    }

    fun setSubtitleStyle(size: Float? = null, color: Long? = null, opacity: Float? = null) {
        size?.let { _subtitleFontSize.value = it }
        color?.let { _subtitleColor.value = it }
        opacity?.let { _subtitleBackgroundOpacity.value = it }
    }

    fun playNext() {
        if (currentMediaIndex < _currentFolderItems.value.size - 1) {
            currentMediaIndex++
            play(_currentFolderItems.value[currentMediaIndex])
        }
    }

    fun playPrevious() {
        if (currentMediaIndex > 0) {
            currentMediaIndex--
            play(_currentFolderItems.value[currentMediaIndex])
        }
    }

    fun setPlaylist(items: List<MediaSource>, startIndex: Int) {
        _currentFolderItems.value = items
        currentMediaIndex = startIndex
    }

    fun toggleTimeMode() {
        _isTimeRemainingMode.value = !_isTimeRemainingMode.value
    }

    fun setSubtitleSyncOffset(offsetMs: Long) {
        _subtitleSyncOffset.value = offsetMs
        // Media3 sync logic would go here if we were using its subtitle view
    }

    fun adjustSubtitleSyncOffset(deltaMs: Long) {
        _subtitleSyncOffset.value += deltaMs
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
            _activeSegments.value = mockSegments
            _subtitleUiState.value = SubtitleUiState.Active(mockSegments)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We don't release the player here if we want background play
        // But we should probably provide a way to stop it
    }
}
