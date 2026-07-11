package com.streamcast.feature.library.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.PlaybackState
import com.streamcast.feature.library.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    mediaSource: MediaSource,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val shuffleEnabled by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playlist by viewModel.currentFolderItems.collectAsState()
    
    val mxBlue = Color(0xFF00A0E9)
    val sheetState = rememberModalBottomSheetState()
    var showPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(mediaSource) {
        viewModel.play(mediaSource)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred Background
        val albumArtUrl = mediaSource.metadata?.thumbnailUrl
        if (albumArtUrl != null) {
            AsyncImage(
                model = albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = { /* More Options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Album Art
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.Gray.copy(alpha = 0.2f),
                tonalElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (albumArtUrl != null) {
                        AsyncImage(
                            model = albumArtUrl,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title & Artist
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mediaSource.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mediaSource.metadata?.artist ?: "Unknown Artist",
                    color = mxBlue,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress Slider
            Column {
                Slider(
                    value = if (duration > 0) currentPos.toFloat() / duration else 0f,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatAudioTime(currentPos), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    Text(formatAudioTime(duration), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) mxBlue else Color.White.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    
                    Surface(
                        onClick = {
                            if (playbackState is PlaybackState.Playing) viewModel.pause()
                            else viewModel.resume()
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }

                IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                    Icon(
                        imageVector = when(repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) mxBlue else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.togglePlaybackSpeed() }) {
                    Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = { /* Timer logic */ }) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = Color.White)
                }
                IconButton(onClick = { showPlaylist = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White)
                }
            }
        }

        if (showPlaylist) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylist = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1C1C1E),
                contentColor = Color.White
            ) {
                PlaylistContent(
                    items = playlist,
                    onItemClick = { index ->
                        viewModel.playPlaylist(playlist, index)
                        showPlaylist = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistContent(items: List<MediaSource>, onItemClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Current Queue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            itemsIndexed(items) { index, item ->
                ListItem(
                    headlineContent = { Text(item.displayName, color = Color.White) },
                    supportingContent = { Text(item.metadata?.artist ?: "Unknown Artist", color = Color.White.copy(alpha = 0.6f)) },
                    leadingContent = {
                        val thumb = item.metadata?.thumbnailUrl
                        if (thumb != null) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable { onItemClick(index) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

fun formatAudioTime(ms: Long): String {
    val totalSeconds = Math.abs(ms) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val sign = if (ms < 0) "-" else ""
    return "$sign%02d:%02d".format(minutes, seconds)
}
