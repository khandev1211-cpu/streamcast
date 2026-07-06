package com.streamcast.feature.library.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamcast.core.player.MediaSource
import com.streamcast.feature.library.data.ConversionState
import com.streamcast.feature.library.domain.model.MediaFolder
import com.streamcast.feature.library.viewmodel.LibraryUiState
import com.streamcast.feature.library.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onVideoClick: (MediaSource) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()
    
    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }
    var isGridView by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // MX Player style sub-header for folder view
        if (selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = selectedFolder!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle View"
                    )
                }
            }
        }

        conversionState?.let { state ->
            when (state) {
                is ConversionState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is ConversionState.Success -> {
                    LaunchedEffect(Unit) {
                        // Show snackbar or toast?
                    }
                }
                is ConversionState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LibraryUiState.Success -> {
                    if (selectedFolder == null) {
                        FolderGrid(folders = state.folders) { selectedFolder = it }
                    } else {
                        if (isGridView) {
                            MediaGrid(
                                items = selectedFolder!!.items,
                                onVideoClick = onVideoClick,
                                onConvertClick = { viewModel.convertToAudio(it) }
                            )
                        } else {
                            MediaList(
                                items = selectedFolder!!.items,
                                onVideoClick = onVideoClick,
                                onConvertClick = { viewModel.convertToAudio(it) }
                            )
                        }
                    }
                }
                is LibraryUiState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun FolderGrid(folders: List<MediaFolder>, onFolderClick: (MediaFolder) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(folders) { folder ->
            FolderItem(folder = folder, onClick = { onFolderClick(folder) })
        }
    }
}

@Composable
fun FolderItem(folder: MediaFolder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.mediaCount} videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MediaGrid(
    items: List<MediaSource>,
    onVideoClick: (MediaSource) -> Unit,
    onConvertClick: (MediaSource) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { media ->
            MediaGridItem(media = media, onClick = { onVideoClick(media) })
        }
    }
}

@Composable
fun MediaGridItem(media: MediaSource, onClick: () -> Unit) {
    val isVideo = media.uri.toString().contains("video")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(16/9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (isVideo) {
                AsyncImage(
                    model = media.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Duration overlay placeholder
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("0:00", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = media.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun MediaList(
    items: List<MediaSource>,
    onVideoClick: (MediaSource) -> Unit,
    onConvertClick: (MediaSource) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items) { media ->
            val isVideo = media.uri.toString().contains("video")
            ListItem(
                headlineContent = { 
                    Text(media.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) 
                },
                supportingContent = { 
                    Text(if (isVideo) "Video" else "Audio", style = MaterialTheme.typography.labelSmall) 
                },
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(56.dp, 40.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (isVideo) {
                            AsyncImage(
                                model = media.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null)
                            }
                        }
                    }
                },
                trailingContent = {
                    if (isVideo) {
                        IconButton(onClick = { onConvertClick(media) }) {
                            Icon(Icons.Default.Transform, contentDescription = "Convert", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                modifier = Modifier.clickable { onVideoClick(media) }
            )
            Divider(modifier = Modifier.padding(start = 16.dp, end = 16.dp), thickness = 0.5.dp)
        }
    }
}
