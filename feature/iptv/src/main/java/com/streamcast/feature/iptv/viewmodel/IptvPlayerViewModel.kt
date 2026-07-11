package com.streamcast.feature.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.core.player.PlayerManager
import com.streamcast.feature.iptv.data.IptvPlaylistManager
import com.streamcast.feature.iptv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IptvPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playlistManager: IptvPlaylistManager,
    private val repository: IptvRepository
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.playerState

    private val _currentChannel = MutableStateFlow<MediaSource?>(null)
    val currentChannel: StateFlow<MediaSource?> = _currentChannel.asStateFlow()

    private val _channels = MutableStateFlow<List<MediaSource>>(emptyList())
    val channels: StateFlow<List<MediaSource>> = _channels.asStateFlow()

    private var currentIndex = -1

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _resizeMode = MutableStateFlow(0) // 0: Fit, 1: Fill, 2: Stretch, 3: Zoom
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _userAgentProfile = MutableStateFlow("Default")
    val userAgentProfile = _userAgentProfile.asStateFlow()

    private val _autoSkipEnabled = MutableStateFlow(true)
    val autoSkipEnabled = _autoSkipEnabled.asStateFlow()

    init {
        monitorPlaybackErrors()
    }

    private fun monitorPlaybackErrors() {
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state is PlaybackState.Error && _autoSkipEnabled.value) {
                    kotlinx.coroutines.delay(2000) // Wait 2s to show error before skipping
                    zapUp()
                }
            }
        }
    }

    fun toggleAutoSkip() {
        _autoSkipEnabled.value = !_autoSkipEnabled.value
    }

    fun cycleUserAgent() {
        val profiles = listOf("Default", "Android TV", "iPhone", "Samsung TV")
        val nextIndex = (profiles.indexOf(_userAgentProfile.value) + 1) % profiles.size
        _userAgentProfile.value = profiles[nextIndex]
        // Reload current channel with new UA
        _currentChannel.value?.let { playChannel(it) }
    }

    fun playChannel(mediaSource: MediaSource) {
        val playlist = playlistManager.getPlaylist()
        
        // Apply UA Profile
        val customHeaders = mediaSource.headers?.toMutableMap() ?: mutableMapOf()
        when (_userAgentProfile.value) {
            "Android TV" -> customHeaders["User-Agent"] = "AndroidTV/1.0 (Google; Pixel TV)"
            "iPhone" -> customHeaders["User-Agent"] = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
            "Samsung TV" -> customHeaders["User-Agent"] = "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/537.36 (KHTML, like Gecko) 71.0.3578.49/6.0 TV Safari/537.36"
        }
        
        val sourceWithHeaders = mediaSource.copy(headers = customHeaders)
        
        _currentChannel.value = sourceWithHeaders
        _channels.value = playlist
        currentIndex = playlist.indexOfFirst { it.id == mediaSource.id }
        playerManager.play(sourceWithHeaders)
        checkFavoriteStatus(mediaSource.id)
    }

    private fun checkFavoriteStatus(channelId: String) {
        viewModelScope.launch {
            val allChannels = repository.getAllChannels().first()
            val channel = allChannels.find { it.id == channelId }
            _isFavorite.value = channel?.isFavorite ?: false
        }
    }

    fun toggleFavorite() {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            val newStatus = !_isFavorite.value
            repository.updateFavorite(channel.id, newStatus)
            _isFavorite.value = newStatus
        }
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
