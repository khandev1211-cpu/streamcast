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

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

    @UnstableApi
    private fun ensurePlayer(): ExoPlayer {
        return exoPlayer ?: run {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val dataSourceFactory = androidx.media3.datasource.DataSource.Factory {
                val dataSource = OkHttpDataSource.Factory(okHttpClient)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .createDataSource()
                
                val source = currentMediaSource
                if (source?.headers?.isNotEmpty() == true) {
                    android.util.Log.d("ExoPlayerManager", "Applying headers for ${source.displayName}: ${source.headers}")
                    source.headers.forEach { (key, value) ->
                        dataSource.setRequestProperty(key, value)
                    }
                }
                dataSource
            }

            // Optimized LoadControl for unreliable live streams
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    20000, // Min buffer 20s
                    60000, // Max buffer 60s
                    3000,  // Buffer for playback 3s
                    6000   // Buffer for playback after re-buffer 6s
                )
                .build()

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build().also {
                    it.addListener(this)
                    exoPlayer = it
                    _playerState.value = it
                }
        }
    }

    override fun play(source: MediaSource) {
        playPlaylist(listOf(source), 0)
    }

    @UnstableApi
    override fun playPlaylist(sources: List<MediaSource>, startIndex: Int) {
        android.util.Log.d("ExoPlayerManager", "Playing playlist with ${sources.size} items at index $startIndex")
        val player = ensurePlayer()
        currentMediaSource = sources[startIndex]
        player.clearMediaItems()
        val mediaItems = sources.map { source ->
            android.util.Log.d("ExoPlayerManager", "Adding MediaItem: ${source.uri}")
            val builder = MediaItem.Builder()
                .setUri(source.uri)
                .setMediaId(source.id)
                .setTag(source)

            // Explicitly set mime types for IPTV/Live if it's a known format
            val uriString = source.uri.toString()
            when {
                uriString.contains(".m3u8") -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                uriString.contains(".mpd") -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
            }
            
            // Set custom headers if provided by the source
            // We'll pass them in the tag so the DataSourceFactory can extract them if needed,
            // or we use RequestMetadata (some data sources support this)
            // Builder doesn't have a direct setHeaders. 
            // One way is using MediaItem.Builder.setMediaMetadata or setTag.
            
            builder.build()
        }
        player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()

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
        val message = when (error.errorCode) {
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val cause = error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                when (cause?.responseCode) {
                    403 -> "Access Denied (403): Often due to geo-blocking or missing tokens. Try 'Sports Pro' profile."
                    404 -> "Not Found (404): The stream link has expired or moved."
                    500, 503 -> "Server Offline: The provider's server is currently down."
                    else -> "Network Error: ${cause?.responseCode ?: "Unknown status"}"
                }
            }
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No Internet: Please check your connection."
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Timeout: The stream server is taking too long to respond."
            androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Codec Error: This format is not supported by your device. Try an external player."
            else -> error.message ?: "Unknown playback error"
        }
        _playbackState.value = PlaybackState.Error(message)
    }
}
