package com.streamcast.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playbackState = playerManager.playbackState
    val player = playerManager.player

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
