package com.streamcast.feature.library.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.feature.library.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaSource: MediaSource,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val player by viewModel.player.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    
    var gestureType by remember { mutableStateOf("") }
    var gestureValue by remember { mutableStateOf(0f) }

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
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        gestureType = if (offset.x < size.width / 2) "Brightness" else "Volume"
                    },
                    onDragEnd = { gestureType = "" },
                    onVerticalDrag = { _, dragAmount ->
                        gestureValue -= dragAmount / 100f
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { gestureType = "Seek" },
                    onDragEnd = { gestureType = "" },
                    onHorizontalDrag = { _, dragAmount ->
                        val seekDelta = (dragAmount * 100).toLong()
                        viewModel.seekTo(currentPos + seekDelta)
                    }
                )
            }
            .clickable { 
                if (!isLocked) showControls = !showControls 
                else showControls = true
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
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Indicator
        if (gestureType.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text(gestureType, color = Color.White)
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
                isLocked = isLocked,
                onBack = onBack,
                onPlayPause = {
                    if (playbackState is PlaybackState.Playing) viewModel.pause()
                    else viewModel.resume()
                },
                onLockToggle = { isLocked = !isLocked },
                onSeek = { viewModel.seekTo(it) },
                onGenerateSubtitles = { /* Launch Subtitle Generation */ }
            )
        }
    }
}

@Composable
fun PlayerControlsOverlay(
    mediaSource: MediaSource,
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    isLocked: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onLockToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onGenerateSubtitles: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLocked) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
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
                
                // Flagship Feature: Generate Subtitles
                Button(
                    onClick = onGenerateSubtitles,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Subtitles", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(onClick = { /* More Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            }

            // Center Controls (MX Player: Previous, Play/Pause, Next)
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = { /* Previous */ }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(onClick = { /* Next */ }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            // Bottom Bar
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
                        IconButton(onClick = { /* Subtitle Language */ }) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                        IconButton(onClick = { /* Audio Track */ }) {
                            Icon(Icons.Default.AudioFile, contentDescription = "Audio", tint = Color.White)
                        }
                    }
                    
                    IconButton(onClick = { /* Resize */ }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.White)
                    }
                }
            }
        } else {
            // Locked UI
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
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
