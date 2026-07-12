package com.streamcast.feature.iptv.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.feature.iptv.viewmodel.IptvPlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun openExternalPlayer(context: Context, mediaSource: MediaSource) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(mediaSource.uri.toString()), "video/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open with..."))
    } catch (e: Exception) {
        // Fallback or Toast
    }
}

@OptIn(UnstableApi::class)
@Composable
fun IptvPlayerScreen(
    mediaSource: MediaSource,
    onBack: () -> Unit = {},
    viewModel: IptvPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val playbackState by viewModel.playbackState.collectAsState()
    val player by viewModel.player.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val channelStatuses by viewModel.channelStatuses.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val currentEpg by viewModel.currentEpg.collectAsState()
    val resizeModeVm by viewModel.resizeMode.collectAsState()
    val autoSkip by viewModel.autoSkipEnabled.collectAsState()
    val uaProfile by viewModel.userAgentProfile.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var showOverflowGrid by remember { mutableStateOf(false) }
    var showStreamInfo by remember { mutableStateOf(false) }
    var showSideList by remember { mutableStateOf(false) }
    var showTrackSelection by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    var gestureType by remember { mutableStateOf("") } 
    var gestureProgress by remember { mutableStateOf(0f) }
    
    val mxBlue = Color(0xFF00A0E9)

    val currentResizeMode = when(resizeModeVm) {
        0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        2 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        3 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    LaunchedEffect(mediaSource) {
        viewModel.playChannel(mediaSource)
        // We'll mark as played via a separate ViewModel call or shared logic
    }

    LaunchedEffect(showControls) {
        if (showControls && !showSideList && !showOverflowGrid) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { 
                    if (showSideList) showSideList = false
                    else showControls = !showControls 
                })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount: Float ->
                    if (dragAmount < -20f && !showSideList) {
                        showSideList = true
                        showControls = false
                    }
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        gestureType = if (offset.x < size.width / 2) "Brightness" else "Volume"
                    },
                    onDragEnd = { gestureType = "" },
                    onVerticalDrag = { _, dragAmount ->
                        val delta = -dragAmount / 500f
                        gestureProgress = (gestureProgress + delta).coerceIn(0f, 1f)
                        if (gestureType == "Volume") {
                            viewModel.setVolume(gestureProgress * 2.0f)
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

        if (gestureType.isNotEmpty()) {
            GestureIndicator(type = gestureType, progress = gestureProgress)
        }

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
                        Button(onClick = { viewModel.playChannel(mediaSource) }) {
                            Text("Retry")
                        }
                        Button(onClick = { openExternalPlayer(context, currentChannel ?: mediaSource) }) {
                            Text("External")
                        }
                        Button(onClick = { viewModel.zapUp() }) {
                            Text("Next")
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
                isFavorite = isFavorite,
                currentEpg = currentEpg,
                onBack = onBack,
                onZapUp = { viewModel.zapUp() },
                onZapDown = { viewModel.zapDown() },
                onShowSettings = { showOverflowGrid = true },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onShowList = { showSideList = true },
                onPlayPause = {
                    if (playbackState is PlaybackState.Playing) viewModel.pause()
                    else viewModel.resume()
                }
            )
        }

        // Side Channel List
        AnimatedVisibility(
            visible = showSideList,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            SideChannelList(
                channels = channels,
                statuses = channelStatuses,
                currentChannelId = currentChannel?.id ?: "",
                onChannelSelect = {
                    viewModel.playChannel(it)
                    showSideList = false
                },
                onDismiss = { showSideList = false }
            )
        }

        if (showOverflowGrid) {
            IptvOverflowMenu(
                onDismiss = { showOverflowGrid = false },
                onToggleResize = { viewModel.toggleResizeMode() },
                autoSkipEnabled = autoSkip,
                onToggleAutoSkip = { viewModel.toggleAutoSkip() },
                onStreamInfoClick = { showStreamInfo = true },
                onExternalPlayerClick = { openExternalPlayer(context, currentChannel ?: mediaSource) },
                onShowTracks = { showTrackSelection = true },
                onShowSleepTimer = { showSleepTimer = true },
                uaProfile = uaProfile,
                onCycleUA = { viewModel.cycleUserAgent() }
            )
        }

        if (showStreamInfo) {
            StreamInfoDialog(
                mediaSource = currentChannel ?: mediaSource,
                playbackState = playbackState,
                onDismiss = { showStreamInfo = false }
            )
        }

        if (showTrackSelection) {
            TrackSelectionDialog(
                player = player,
                onDismiss = { showTrackSelection = false }
            )
        }

        if (showSleepTimer) {
             SleepTimerDialog(
                onDismiss = { showSleepTimer = false },
                onSelect = { 
                    viewModel.setSleepTimer(it)
                    showSleepTimer = false
                }
            )
        }
    }
}

@Composable
fun SideChannelList(
    channels: List<MediaSource>,
    statuses: List<Channel>,
    currentChannelId: String,
    onChannelSelect: (MediaSource) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChannels = remember(searchQuery, channels) {
        if (searchQuery.isEmpty()) channels
        else channels.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = Color.Black.copy(alpha = 0.9f),
        tonalElevation = 12.dp
    ) {
        Column {
            // Header with Search
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Channels", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color(0xFF00A0E9),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, tint = Color.Gray) } }
                    } else null
                )
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChannels) { channel ->
                    val isSelected = channel.id == currentChannelId
                    val status = statuses.find { it.id == channel.id }?.lastCheckStatus ?: 0
                    val statusColor = when(status) {
                        1 -> Color.Green
                        2 -> Color.Red
                        else -> Color.Gray
                    }
                    
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    channel.displayName, 
                                    color = if (isSelected) Color(0xFF00A0E9) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                ) 
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, shape = CircleShape)
                                )
                            }
                        },
                        supportingContent = { Text(channel.metadata?.description ?: "", color = Color.Gray, fontSize = 10.sp, maxLines = 1) },
                        leadingContent = {
                            if (isSelected) Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00A0E9))
                            else Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.clickable { onChannelSelect(channel) },
                        colors = ListItemDefaults.colors(containerColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                    )
                }
            }
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
    onStreamInfoClick: () -> Unit = {},
    onExternalPlayerClick: () -> Unit = {},
    onShowTracks: () -> Unit = {},
    onShowSleepTimer: () -> Unit = {},
    uaProfile: String = "Default",
    onCycleUA: () -> Unit = {}
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
                        "External Player" to Icons.Default.OpenInNew to onExternalPlayerClick,
                        "UA: $uaProfile" to Icons.Default.Phonelink to onCycleUA,
                        "Stream Info" to Icons.Default.Info to onStreamInfoClick,
                        "Audio Tracks" to Icons.Default.MusicNote to onShowTracks,
                        "Subtitles" to Icons.Default.Subtitles to {},
                        "Refresh EPG" to Icons.Default.Refresh to {},
                        "Sleep Timer" to Icons.Default.Timer to onShowSleepTimer,
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
    isFavorite: Boolean,
    currentEpg: List<com.streamcast.core.database.entities.EpgProgram> = emptyList(),
    onBack: () -> Unit,
    onZapUp: () -> Unit,
    onZapDown: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowList: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ... (top bar and center controls)
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
            
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.White
                )
            }
            IconButton(onClick = onShowList) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Channel List", tint = Color.White)
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

        // Bottom Info / EPG
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            val nowProgram = currentEpg.firstOrNull()
            val nextProgram = if (currentEpg.size > 1) currentEpg[1] else null

            if (nowProgram != null) {
                Text("Now: ${nowProgram.title}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                val total = nowProgram.endTime - nowProgram.startTime
                val elapsed = System.currentTimeMillis() - nowProgram.startTime
                val progress = if (total > 0) elapsed.toFloat() / total else 0f
                
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f), 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Color(0xFF00A0E9)
                )
            } else {
                Text("Now: Loading program info...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = 0.3f, 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Color(0xFF00A0E9)
                )
            }

            if (nextProgram != null) {
                Text("Next: ${nextProgram.title}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Next: Up next program info", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
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

@Composable
fun GestureIndicator(type: String, progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (type == "Brightness") Icons.Default.BrightnessHigh else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color.White, shape = CircleShape)
                )
            }
        }
    }
}
