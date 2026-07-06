package com.streamcast.core.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the playback controls and state observation for StreamCast.
 */
interface PlayerManager {
    val playbackState: StateFlow<PlaybackState>
    val player: Player?
    
    fun play(source: MediaSource)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
}

/**
 * Represents the current state of the player.
 */
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Buffering : PlaybackState()
    data class Playing(val source: MediaSource, val currentPosition: Long) : PlaybackState()
    data class Paused(val source: MediaSource, val currentPosition: Long) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
    object Ended : PlaybackState()
}
