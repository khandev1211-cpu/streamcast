package com.streamcast.feature.library.ui.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.feature.library.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaSource: MediaSource,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val playbackState by viewModel.playbackState.collectAsState()
    val player by viewModel.player.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val decoderType by viewModel.decoderType.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var scale by remember { mutableStateOf(1f) }

    // System info states
    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableStateOf(0) }

    // Gesture status
    var gestureType by remember { mutableStateOf("") } 
    var gestureProgress by remember { mutableStateOf(0f) }
    var seekTarget by remember { mutableStateOf(0L) }

    // Update time and battery
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            delay(10000) // Update every 10s
        }
    }

    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(5000)
            showControls = false
        }
    }

    LaunchedEffect(mediaSource) {
        viewModel.play(mediaSource)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > size.width / 2
                        if (isRightSide) viewModel.seekTo(currentPos + 10000)
                        else viewModel.seekTo(currentPos - 10000)
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                }
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        gestureType = if (offset.x < size.width / 2) "Brightness" else "Volume"
                    },
                    onDragEnd = { gestureType = "" },
                    onVerticalDrag = { _, dragAmount ->
                        val delta = -dragAmount / 500f
                        gestureProgress = (gestureProgress + delta).coerceIn(0f, 1f)
                        if (gestureType == "Volume") player?.volume = gestureProgress
                        else if (gestureType == "Brightness") {
                            activity?.window?.let { window ->
                                val params = window.attributes
                                params.screenBrightness = gestureProgress
                                window.attributes = params
                            }
                        }
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { gestureType = "Seek"; seekTarget = currentPos },
                    onDragEnd = { 
                        viewModel.seekTo(seekTarget)
                        gestureType = "" 
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val delta = (dragAmount * 200).toLong()
                        seekTarget = (seekTarget + delta).coerceIn(0L, duration)
                        gestureProgress = if (duration > 0) seekTarget.toFloat() / duration else 0f
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
        )

        // Gesture Overlay
        if (gestureType.isNotEmpty()) {
            if (gestureType == "Seek") {
                SeekIndicator(seekTarget, duration)
            } else {
                GestureIndicator(type = gestureType, progress = gestureProgress)
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                mediaSource = mediaSource,
                playbackState = playbackState,
                currentPosition = currentPos,
                duration = duration,
                playbackSpeed = playbackSpeed,
                decoderType = decoderType,
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isLocked = isLocked,
                onBack = onBack,
                onPlayPause = {
                    if (playbackState is PlaybackState.Playing) viewModel.pause()
                    else viewModel.resume()
                },
                onLockToggle = { isLocked = !isLocked },
                onSeek = { viewModel.seekTo(it) },
                onGenerateSubtitles = { /* AI */ },
                onToggleSpeed = { viewModel.togglePlaybackSpeed() },
                onToggleDecoder = { viewModel.toggleDecoder() },
                onToggleResize = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) scale = 1f
                },
                onPiP = { activity?.enterPictureInPictureMode() }
            )
        }
    }
}

@Composable
fun SeekIndicator(target: Long, total: Long) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            Text(
                text = formatTime(target),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[ ${formatTime(target - total)} ]", // Relative time
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun GestureIndicator(type: String, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = if (type == "Brightness") Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                .padding(vertical = 16.dp)
        ) {
            Icon(
                imageVector = if (type == "Brightness") Icons.Default.BrightnessMedium else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(150.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), shape = MaterialTheme.shapes.extraSmall)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progress)
                        .align(Alignment.BottomCenter)
                        .background(Color.White, shape = MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}

@Composable
fun PlayerControlsOverlay(
    mediaSource: MediaSource,
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    decoderType: String,
    currentTime: String,
    batteryLevel: Int,
    isLocked: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onLockToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onGenerateSubtitles: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleDecoder: () -> Unit,
    onToggleResize: () -> Unit,
    onPiP: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLocked) {
            // Advanced Top Bar (MX Player style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Info Bar (Clock, Battery)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BatteryStd, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(" $batteryLevel%", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(currentTime, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = mediaSource.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Decoder
                    TextButton(onClick = onToggleDecoder) {
                        Text(decoderType, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Speed
                    TextButton(onClick = onToggleSpeed) {
                        Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onPiP) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                    }

                    IconButton(onClick = { /* More Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                    }
                }
            }

            // Center Controls
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                IconButton(onClick = { onSeek(currentPosition - 10000) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    Icon(
                        imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(onClick = { onSeek(currentPosition + 10000) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            // Advanced Bottom Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { onSeek((it * duration).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.Gray
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLockToggle) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }
                    
                    Row {
                        IconButton(onClick = onGenerateSubtitles) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Subtitles", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { /* Audio Menu */ }) {
                            Icon(Icons.Default.AudioFile, contentDescription = "Audio", tint = Color.White)
                        }
                        IconButton(onClick = { /* Subtitle Menu */ }) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                    }
                    
                    IconButton(onClick = onToggleResize) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.White)
                    }
                }
            }
        } else {
            // Locked UI - Only show lock button
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = Math.abs(ms) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val sign = if (ms < 0) "-" else ""
    return "$sign%02d:%02d".format(minutes, seconds)
}
