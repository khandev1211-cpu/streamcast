package com.streamcast.core.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.streamcast.core.player.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerManager, Player.Listener {

    private var exoPlayer: ExoPlayer? = null
    
    private val _playerState = MutableStateFlow<Player?>(null)
    override val playerState: StateFlow<Player?> = _playerState.asStateFlow()

    private var currentMediaSource: MediaSource? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // A-B Repeat
    private val _abRepeatRange = MutableStateFlow<Pair<Long, Long>?>(null)
    override val abRepeatRange: StateFlow<Pair<Long, Long>?> = _abRepeatRange.asStateFlow()
    
    private var repeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun ensurePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .build().also {
                it.addListener(this)
                exoPlayer = it
                _playerState.value = it
            }
    }

    override fun play(source: MediaSource) {
        playPlaylist(listOf(source), 0)
    }

    override fun playPlaylist(sources: List<MediaSource>, startIndex: Int) {
        val player = ensurePlayer()
        player.clearMediaItems()
        val mediaItems = sources.map { 
            MediaItem.Builder()
                .setUri(it.uri)
                .setMediaId(it.id)
                .setTag(it)
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()
        
        currentMediaSource = sources[startIndex]

        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun resume() {
        exoPlayer?.play()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
    }

    override fun release() {
        repeatJob?.cancel()
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = null
        _playbackState.value = PlaybackState.Idle
    }

    override fun setAbRepeatRange(startMs: Long, endMs: Long) {
        _abRepeatRange.value = startMs to endMs
        exoPlayer?.seekTo(startMs)
        startRepeatMonitor()
    }

    override fun clearAbRepeatRange() {
        _abRepeatRange.value = null
        repeatJob?.cancel()
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    override fun setShuffleMode(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: Int) {
        exoPlayer?.repeatMode = mode
    }

    private fun startRepeatMonitor() {
        repeatJob?.cancel()
        repeatJob = scope.launch {
            while (true) {
                val range = _abRepeatRange.value
                val player = exoPlayer
                if (range != null && player != null) {
                    if (player.currentPosition >= range.second) {
                        player.seekTo(range.first)
                    }
                } else {
                    break
                }
                delay(100)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItem?.localConfiguration?.tag?.let {
            if (it is MediaSource) {
                currentMediaSource = it
            }
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        val source = currentMediaSource ?: return
        when (state) {
            Player.STATE_BUFFERING -> _playbackState.value = PlaybackState.Buffering
            Player.STATE_READY -> {
                if (exoPlayer?.isPlaying == true) {
                    _playbackState.value = PlaybackState.Playing(source, exoPlayer?.currentPosition ?: 0)
                } else {
                    _playbackState.value = PlaybackState.Paused(source, exoPlayer?.currentPosition ?: 0)
                }
            }
            Player.STATE_ENDED -> _playbackState.value = PlaybackState.Ended
            Player.STATE_IDLE -> _playbackState.value = PlaybackState.Idle
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val source = currentMediaSource ?: return
        if (isPlaying) {
            _playbackState.value = PlaybackState.Playing(source, exoPlayer?.currentPosition ?: 0)
        } else {
            if (exoPlayer?.playbackState == Player.STATE_READY) {
                _playbackState.value = PlaybackState.Paused(source, exoPlayer?.currentPosition ?: 0)
            }
        }
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        _playbackState.value = PlaybackState.Error(error.message ?: "Unknown playback error")
    }
}
