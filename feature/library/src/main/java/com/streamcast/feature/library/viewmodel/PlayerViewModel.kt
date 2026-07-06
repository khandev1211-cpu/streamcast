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
    val player = playerManager.player

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentPosition.value = player?.currentPosition ?: 0L
                _duration.value = player?.duration ?: 0L
                delay(1000)
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

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
