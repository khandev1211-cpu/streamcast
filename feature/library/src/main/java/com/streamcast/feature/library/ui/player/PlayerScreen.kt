package com.streamcast.feature.library.ui.player

import android.app.Activity
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.text.style.TextAlign
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
import com.streamcast.core.player.model.SubtitleSegment
import com.streamcast.feature.library.viewmodel.PlayerViewModel
import com.streamcast.feature.library.viewmodel.SubtitleUiState
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
    val abRange by viewModel.abRepeatRange.collectAsState()
    val sleepTimer by viewModel.sleepTimerMillis.collectAsState()
    val subtitleState by viewModel.subtitleUiState.collectAsState()
    val activeSegments by viewModel.activeSegments.collectAsState()
    val subtitleOffset by viewModel.subtitleSyncOffset.collectAsState()
    val resumePos by viewModel.resumePosition.collectAsState()
    val isTimeRemainingMode by viewModel.isTimeRemainingMode.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var scale by remember { mutableStateOf(1f) }

    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableStateOf(0) }

    var gestureType by remember { mutableStateOf("") } 
    var gestureProgress by remember { mutableStateOf(0f) }
    var seekTarget by remember { mutableStateOf(0L) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showABRepeatControls by remember { mutableStateOf(false) }
    var showTrackSelectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            delay(10000)
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
                    },
                    onLongPress = {
                        viewModel.setPlaybackSpeed(2.0f)
                    },
                    onPress = {
                        tryAwaitRelease()
                        viewModel.setPlaybackSpeed(1.0f)
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
                        if (gestureType == "Volume") {
                            val vol = gestureProgress * 2.0f
                            viewModel.setVolume(vol)
                        } else if (gestureType == "Brightness") {
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

        if (gestureType.isNotEmpty()) {
            if (gestureType == "Seek") {
                SeekIndicator(seekTarget)
            } else {
                GestureIndicator(type = gestureType, progress = gestureProgress)
            }
        }

        if (playbackSpeed > 1.0f && !showControls) {
             Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Speed: ${playbackSpeed}x", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                abRange = abRange,
                sleepTimerRemaining = sleepTimer,
                isTimeRemainingMode = isTimeRemainingMode,
                onBack = onBack,
                onPlayPause = {
                    if (playbackState is PlaybackState.Playing) viewModel.pause()
                    else viewModel.resume()
                },
                onLockToggle = { isLocked = !isLocked },
                onSeek = { viewModel.seekTo(it) },
                onGenerateSubtitles = { viewModel.generateSubtitles("en") },
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
                onPiP = { activity?.enterPictureInPictureMode() },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onABRepeatClick = { showABRepeatControls = !showABRepeatControls },
                onTrackSelectionClick = { showTrackSelectionDialog = true },
                onToggleTimeMode = { viewModel.toggleTimeMode() },
                subtitleOffset = subtitleOffset,
                onAdjustOffset = { viewModel.adjustSubtitleSyncOffset(it) }
            )
        }

        if (showABRepeatControls && !isLocked) {
            ABRepeatControls(
                abRange = abRange,
                onSetA = { viewModel.setAbRepeat(currentPos, abRange?.second ?: (currentPos + 5000)) },
                onSetB = { viewModel.setAbRepeat(abRange?.first ?: 0L, currentPos) },
                onClear = { viewModel.clearAbRepeat() },
                modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp)
            )
        }

        SubtitleOverlay(
            currentPosition = currentPos,
            syncOffset = subtitleOffset,
            segments = activeSegments,
            state = subtitleState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSelect = { 
                viewModel.setSleepTimer(it)
                showSleepTimerDialog = false
            }
        )
    }

    if (showTrackSelectionDialog) {
        TrackSelectionDialog(
            player = player,
            onDismiss = { showTrackSelectionDialog = false }
        )
    }

    resumePos?.let { pos ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissResume() },
            title = { Text("Resume Playback") },
            text = { Text("Do you want to resume from ${formatTime(pos)}?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmResume() }) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResume() }) {
                    Text("Start from beginning")
                }
            }
        )
    }
}

@Composable
fun SubtitleOverlay(
    currentPosition: Long,
    syncOffset: Long,
    segments: List<SubtitleSegment>,
    state: SubtitleUiState,
    modifier: Modifier = Modifier
) {
    val currentSecond = (currentPosition + syncOffset) / 1000f
    val activeSegment = segments.find { currentSecond >= it.start && currentSecond <= it.end }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is SubtitleUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            }
            is SubtitleUiState.Active -> {
                activeSegment?.let { segment ->
                    Text(
                        text = segment.text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            is SubtitleUiState.Error -> {
                Text(state.message, color = Color.Red, style = MaterialTheme.typography.labelSmall)
            }
            else -> {}
        }
    }
}

@Composable
fun ABRepeatControls(
    abRange: Pair<Long, Long>?,
    onSetA: () -> Unit,
    onSetB: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("A-B Loop", color = Color.White, style = MaterialTheme.typography.labelSmall)
        Button(onClick = onSetA, colors = ButtonDefaults.buttonColors(containerColor = if (abRange?.first != null) MaterialTheme.colorScheme.primary else Color.Gray)) {
            Text("Set A")
        }
        Button(onClick = onSetB, colors = ButtonDefaults.buttonColors(containerColor = if (abRange?.second != null) MaterialTheme.colorScheme.primary else Color.Gray)) {
            Text("Set B")
        }
        TextButton(onClick = onClear) {
            Text("Clear", color = Color.White)
        }
    }
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(0, 15, 30, 60, 90).forEach { mins ->
                    val label = if (mins == 0) "Off" else "$mins minutes"
                    ListItem(
                        headlineContent = { Text(label) },
                        modifier = Modifier.clickable { onSelect(mins) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SeekIndicator(target: Long) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
    abRange: Pair<Long, Long>?,
    sleepTimerRemaining: Long?,
    isTimeRemainingMode: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onLockToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onGenerateSubtitles: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleDecoder: () -> Unit,
    onToggleResize: () -> Unit,
    onPiP: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onABRepeatClick: () -> Unit,
    onTrackSelectionClick: () -> Unit,
    onToggleTimeMode: () -> Unit,
    subtitleOffset: Long = 0,
    onAdjustOffset: (Long) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLocked) {
            // TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Info Bar (Battery, Time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sleepTimerRemaining != null) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(" ${sleepTimerRemaining / 1000 / 60}m", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                    }
                    Icon(Icons.Default.BatteryStd, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(" $batteryLevel%", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(currentTime, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                // Controls Bar
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
                    
                    IconButton(onClick = onTrackSelectionClick) {
                        Icon(Icons.Default.SettingsVoice, contentDescription = "Audio Tracks", tint = Color.White)
                    }

                    IconButton(onClick = onGenerateSubtitles) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = if (abRange != null) MaterialTheme.colorScheme.primary else Color.White)
                    }

                    TextButton(onClick = onToggleDecoder) {
                        Text(decoderType, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { /* More Menu - containing Speed, PiP, Sleep etc */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                    }
                }
            }

            // Sync Controls
            if (subtitleOffset != 0L) {
                 Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.small)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync: ${subtitleOffset}ms", color = Color.White, fontSize = 10.sp)
                    IconButton(onClick = { onAdjustOffset(-100) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "-100ms", tint = Color.White)
                    }
                    IconButton(onClick = { onAdjustOffset(100) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "+100ms", tint = Color.White)
                    }
                }
            }

            // BOTTOM CONTROLS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp)
            ) {
                // Seekbar and Time
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { onToggleTimeMode() }
                        )
                        Text(
                            text = if (isTimeRemainingMode) "-${formatTime(duration - currentPosition)}" else formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { onToggleTimeMode() }
                        )
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
                }

                // Playback Buttons Row (MX PLAYER STYLE)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Corner: Lock
                    IconButton(onClick = onLockToggle) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }

                    // Center: Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { /* Previous */ }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                        }
                        IconButton(onClick = { onSeek(currentPosition - 10000) }) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White)
                        }
                        
                        IconButton(onClick = onPlayPause, modifier = Modifier.size(56.dp)) {
                            Icon(
                                imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        IconButton(onClick = { onSeek(currentPosition + 10000) }) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White)
                        }
                        IconButton(onClick = { /* Next */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                        }
                    }

                    // Right Corner: Features (Speed / Pip / Resize)
                    Row {
                        IconButton(onClick = onToggleSpeed) {
                            Text("${playbackSpeed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onToggleResize) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.White)
                        }
                    }
                }
            }
        } else {
            // Locked State Overlay
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = "Unlock", 
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
    
    // Silence warnings for now by putting these in a logic block that's always true or similar if needed, 
    // or better, actually use them in a "More Menu" or similar.
    // I'll add them to a simple dropdown for now.
}

fun formatTime(ms: Long): String {
    val totalSeconds = Math.abs(ms) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val sign = if (ms < 0) "-" else ""
    return "$sign%02d:%02d".format(minutes, seconds)
}
