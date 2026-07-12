package com.streamcast.feature.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.database.dao.EpgDao
import com.streamcast.core.database.entities.EpgProgram
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.core.player.PlayerManager
import com.streamcast.feature.iptv.data.IptvPlaylistManager
import com.streamcast.feature.iptv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IptvPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playlistManager: IptvPlaylistManager,
    private val repository: IptvRepository,
    private val epgDao: EpgDao
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.playerState

    private val _currentChannel = MutableStateFlow<MediaSource?>(null)
    val currentChannel: StateFlow<MediaSource?> = _currentChannel.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentEpg: StateFlow<List<EpgProgram>> = _currentChannel.flatMapLatest { channel ->
        if (channel != null) {
            epgDao.getCurrentAndNext(channel.id, System.currentTimeMillis())
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _channels = MutableStateFlow<List<MediaSource>>(emptyList())
    val channels: StateFlow<List<MediaSource>> = _channels.asStateFlow()

    val channelStatuses = repository.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentIndex = -1

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _resizeMode = MutableStateFlow(0) // 0: Fit, 1: Fill, 2: Stretch, 3: Zoom
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _userAgentProfile = MutableStateFlow("Default")
    val userAgentProfile = _userAgentProfile.asStateFlow()

    private val _autoSkipEnabled = MutableStateFlow(true)
    val autoSkipEnabled = _autoSkipEnabled.asStateFlow()

    private val _sleepTimerMillis = MutableStateFlow<Long?>(null)
    val sleepTimerMillis: StateFlow<Long?> = _sleepTimerMillis.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val sessionBlacklist = mutableSetOf<String>()

    init {
        monitorPlaybackErrors()
    }

    private fun monitorPlaybackErrors() {
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state is PlaybackState.Error && _autoSkipEnabled.value) {
                    _currentChannel.value?.id?.let { sessionBlacklist.add(it) }
                    kotlinx.coroutines.delay(2000) // Wait 2s to show error before skipping
                    zapUp()
                }
            }
        }
    }

    fun toggleAutoSkip() {
        _autoSkipEnabled.value = !_autoSkipEnabled.value
    }

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

    fun cycleUserAgent() {
        val profiles = listOf("Default", "Android TV", "iPhone", "Samsung TV", "JioTV", "Pakistan Zap", "TiviMate Pro", "Smarters Pro", "Xtream Pro", "Sports Boost", "Sports Pro", "Ultra Sports")
        val nextIndex = (profiles.indexOf(_userAgentProfile.value) + 1) % profiles.size
        _userAgentProfile.value = profiles[nextIndex]
        // Reload current channel with new UA
        _currentChannel.value?.let { playChannel(it) }
    }

    fun playChannel(mediaSource: MediaSource) {
        val playlist = playlistManager.getPlaylist()
        
        // Apply UA Profile
        val customHeaders = mediaSource.headers?.toMutableMap() ?: mutableMapOf()
        val uriStr = mediaSource.uri.toString()

        when (_userAgentProfile.value) {
            "Android TV" -> customHeaders["User-Agent"] = "AndroidTV/1.0 (Google; Pixel TV)"
            "iPhone" -> customHeaders["User-Agent"] = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
            "Samsung TV" -> customHeaders["User-Agent"] = "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/537.36 (KHTML, like Gecko) 71.0.3578.49/6.0 TV Safari/537.36"
            "JioTV" -> {
                customHeaders["User-Agent"] = "JioTV/2.3.0 (Linux; Android 10)"
                customHeaders["Referer"] = "https://www.jiotv.com/"
            }
            "Pakistan Zap" -> {
                customHeaders["User-Agent"] = "VLC/3.0.11 LibVLC/3.0.11" // VLC is widely trusted by PK headends
                if (uriStr.contains("aryzap") || uriStr.contains("5centscdn")) {
                    customHeaders["Referer"] = "https://live.arydigital.tv/"
                } else if (uriStr.contains("mjunoon")) {
                    customHeaders["Referer"] = "https://www.mjunoon.tv/"
                }
            }
            "TiviMate Pro" -> {
                customHeaders["User-Agent"] = "TiviMate/4.7.0 (Linux; Android 11)"
            }
            "Smarters Pro" -> {
                customHeaders["User-Agent"] = "IPTVSmartersPlayer/3.0.0 (Linux; Android 12)"
            }
            "Xtream Pro" -> {
                customHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) IPTV/1.0"
            }
            "Sports Boost" -> {
                customHeaders["User-Agent"] = "VLC/3.0.11 LibVLC/3.0.11"
                if (uriStr.contains("103.250") || uriStr.contains("121.91") || uriStr.contains("103.213")) {
                    // These are common PK headend IPs (PTV/Ten) - They often need specific referers
                    customHeaders["Referer"] = "http://ptvsports.com.pk/"
                    customHeaders["Origin"] = "http://ptvsports.com.pk"
                } else {
                    customHeaders["Referer"] = "https://www.espn.com/"
                }
            }
            "Sports Pro" -> {
                customHeaders["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                customHeaders["Referer"] = "https://www.espn.com/"
            }
            "Ultra Sports" -> {
                customHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                customHeaders["Origin"] = "https://www.google.com"
            }
        }
        
        val sourceWithHeaders = mediaSource.copy(headers = customHeaders)
        
        _currentChannel.value = sourceWithHeaders
        _channels.value = playlist
        currentIndex = playlist.indexOfFirst { it.id == mediaSource.id }
        playerManager.play(sourceWithHeaders)
        checkFavoriteStatus(mediaSource.id)
        
        viewModelScope.launch {
            repository.updateLastPlayed(mediaSource.id)
        }
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
            if (sessionBlacklist.contains(next.id) && currentIndex < _channels.value.size - 1) {
                zapUp() // Recursively skip blacklisted
                return
            }
            _currentChannel.value = next
            playChannel(next)
        }
    }

    fun zapDown() {
        if (currentIndex > 0) {
            currentIndex--
            val prev = _channels.value[currentIndex]
            if (sessionBlacklist.contains(prev.id) && currentIndex > 0) {
                zapDown()
                return
            }
            _currentChannel.value = prev
            playChannel(prev)
        }
    }

    fun toggleResizeMode() {
        _resizeMode.value = (_resizeMode.value + 1) % 4
    }

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()

    override fun onCleared() {
        super.onCleared()
        // playerManager.stop() // Optional: stop playback when leaving player
    }
}
