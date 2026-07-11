package com.streamcast.feature.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.core.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IptvPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.playerState

    private val _currentChannel = MutableStateFlow<MediaSource?>(null)
    val currentChannel: StateFlow<MediaSource?> = _currentChannel.asStateFlow()

    private val _channels = MutableStateFlow<List<MediaSource>>(emptyList())
    private var currentIndex = -1

    private val _resizeMode = MutableStateFlow(0) // 0: Fit, 1: Fill, 2: Stretch, 3: Zoom
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    fun playChannel(mediaSource: MediaSource, playlist: List<MediaSource>) {
        _currentChannel.value = mediaSource
        _channels.value = playlist
        currentIndex = playlist.indexOfFirst { it.id == mediaSource.id }
        playerManager.play(mediaSource)
    }

    fun zapUp() {
        if (currentIndex < _channels.value.size - 1) {
            currentIndex++
            val next = _channels.value[currentIndex]
            _currentChannel.value = next
            playerManager.play(next)
        }
    }

    fun zapDown() {
        if (currentIndex > 0) {
            currentIndex--
            val prev = _channels.value[currentIndex]
            _currentChannel.value = prev
            playerManager.play(prev)
        }
    }

    fun toggleResizeMode() {
        _resizeMode.value = (_resizeMode.value + 1) % 4
    }

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()

    override fun onCleared() {
        super.onCleared()
        // playerManager.stop() // Optional: stop playback when leaving player
    }
}
