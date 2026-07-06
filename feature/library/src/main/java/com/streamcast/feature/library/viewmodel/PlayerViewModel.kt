package com.streamcast.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.playerState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.value?.currentPosition ?: 0L
                _duration.value = player.value?.duration ?: 0L
                delay(500) // Faster updates for smoother slider
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
        player.value?.let { 
            it.setPlaybackSpeed(speed)
        }
    }

    fun togglePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f, 0.75f)
        val currentIndex = speeds.indexOf(_playbackSpeed.value)
        val nextIndex = (currentIndex + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
