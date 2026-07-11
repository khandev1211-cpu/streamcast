package com.streamcast.feature.iptv.ui

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.streamcast.feature.iptv.viewmodel.IptvPlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun IptvPlayerScreen(
    mediaSource: MediaSource,
    playlist: List<MediaSource>,
    onBack: () -> Unit = {},
    viewModel: IptvPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val playbackState by viewModel.playbackState.collectAsState()
    val player by viewModel.player.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val resizeModeVm by viewModel.resizeMode.collectAsState()
    val autoSkip by viewModel.autoSkipEnabled.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var showOverflowGrid by remember { mutableStateOf(false) }
    var showStreamInfo by remember { mutableStateOf(false) }
    
    val mxBlue = Color(0xFF00A0E9)

    val currentResizeMode = when(resizeModeVm) {
        0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        2 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        3 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    LaunchedEffect(mediaSource) {
        viewModel.playChannel(mediaSource, playlist)
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls })
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
                playerView.resizeMode = currentResizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Spinner
        if (playbackState is PlaybackState.Buffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = mxBlue, modifier = Modifier.size(64.dp))
            }
        }

        // Error Message Overlay
        if (playbackState is PlaybackState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = (playbackState as PlaybackState.Error).message,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.playChannel(mediaSource, playlist) }) {
                            Text("Retry")
                        }
                        if (playlist.isNotEmpty()) {
                            Button(onClick = { viewModel.zapUp() }) {
                                Text("Next Channel")
                            }
                        }
                    }
                    TextButton(onClick = onBack) {
                        Text("Back", color = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IptvControlsOverlay(
                currentChannel = currentChannel ?: mediaSource,
                playbackState = playbackState,
                onBack = onBack,
                onZapUp = { viewModel.zapUp() },
                onZapDown = { viewModel.zapDown() },
                onShowSettings = { showOverflowGrid = true },
                onPlayPause = {
                    if (playbackState is PlaybackState.Playing) viewModel.pause()
                    else viewModel.resume()
                }
            )
        }

        if (showOverflowGrid) {
            IptvOverflowMenu(
                onDismiss = { showOverflowGrid = false },
                onToggleResize = { viewModel.toggleResizeMode() },
                autoSkipEnabled = autoSkip,
                onToggleAutoSkip = { viewModel.toggleAutoSkip() },
                onStreamInfoClick = { showStreamInfo = true }
            )
        }

        if (showStreamInfo) {
            StreamInfoDialog(
                mediaSource = currentChannel ?: mediaSource,
                playbackState = playbackState,
                onDismiss = { showStreamInfo = false }
            )
        }
    }
}

@Composable
fun StreamInfoDialog(
    mediaSource: MediaSource,
    playbackState: PlaybackState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stream Diagnostics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagnosticRow("Channel", mediaSource.displayName)
                DiagnosticRow("URL", mediaSource.uri.toString())
                DiagnosticRow("Status", when(playbackState) {
                    is PlaybackState.Playing -> "Active (Playing)"
                    is PlaybackState.Buffering -> "Buffering..."
                    is PlaybackState.Error -> "Error: ${playbackState.message}"
                    else -> "Idle"
                })
                mediaSource.headers?.let { headers ->
                    DiagnosticRow("Headers", headers.entries.joinToString("\n") { "${it.key}: ${it.value}" })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF00A0E9))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun IptvOverflowMenu(
    onDismiss: () -> Unit,
    onToggleResize: () -> Unit,
    autoSkipEnabled: Boolean = true,
    onToggleAutoSkip: () -> Unit = {},
    onStreamInfoClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(280.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("IPTV Settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    val items = listOf(
                        "Aspect Ratio" to Icons.Default.AspectRatio to onToggleResize,
                        "Stream Info" to Icons.Default.Info to onStreamInfoClick,
                        "Audio Tracks" to Icons.Default.MusicNote to {},
                        "Subtitles" to Icons.Default.Subtitles to {},
                        "Refresh EPG" to Icons.Default.Refresh to {},
                        "Add to Favourites" to Icons.Default.Favorite to {},
                        "Sleep Timer" to Icons.Default.Timer to {},
                        "Full Settings" to Icons.Default.Settings to {}
                    )

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(items) { item ->
                            val (pair, onClick) = item
                            val (label, icon) = pair
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .clickable { 
                                        onClick()
                                        onDismiss()
                                    }
                            ) {
                                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
                                Text(label, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Skip Dead Links", color = Color.White, fontSize = 12.sp)
                            Text("Try next channel on error", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = autoSkipEnabled,
                            onCheckedChange = { onToggleAutoSkip() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00A0E9))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IptvControlsOverlay(
    currentChannel: MediaSource,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onZapUp: () -> Unit,
    onZapDown: () -> Unit,
    onShowSettings: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentChannel.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onShowSettings) {
                Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Center Zapping Controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            IconButton(onClick = onZapDown, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Channel", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            
            IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                Icon(
                    imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(onClick = onZapUp, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Channel", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        // Bottom Info / EPG Placeholder
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text("Now: Loading program info...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = 0.3f, 
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = Color(0xFF00A0E9)
            )
            Text("Next: Up next program info", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
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
