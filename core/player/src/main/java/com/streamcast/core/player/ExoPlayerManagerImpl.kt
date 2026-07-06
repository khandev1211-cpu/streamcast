package com.streamcast.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private fun ensurePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also {
            it.addListener(this)
            exoPlayer = it
            _playerState.value = it
        }
    }

    override fun play(source: MediaSource) {
        val player = ensurePlayer()
        currentMediaSource = source
        val mediaItem = MediaItem.fromUri(source.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = null
        _playbackState.value = PlaybackState.Idle
    }

    // Player.Listener implementation
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
