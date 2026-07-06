package com.streamcast.feature.library.ui

import androidx.activity.compose.BackHandler
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onVideoClick: (MediaSource) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()
    val folderStack by viewModel.folderStack.collectAsState()
    val isHierarchical by viewModel.isHierarchical.collectAsState()
    
    var isGridView by remember { mutableStateOf(true) }
    val currentFolder = folderStack.lastOrNull()

    BackHandler(enabled = folderStack.isNotEmpty()) {
        viewModel.navigateBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { 
                Text(
                    text = currentFolder?.name ?: if (isHierarchical) "Folders" else "All Folders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (folderStack.isNotEmpty()) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(if (isGridView) Icons.Default.List else Icons.Default.GridView, contentDescription = "Toggle Grid")
                }
                IconButton(onClick = { viewModel.toggleViewMode() }) {
                    Icon(if (isHierarchical) Icons.Default.AccountTree else Icons.Default.FilterNone, contentDescription = "Toggle Hierarchy")
                }
                IconButton(onClick = { viewModel.loadMedia() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        conversionState?.let { state ->
            when (state) {
                is ConversionState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is ConversionState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                else -> {}
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LibraryUiState.Success -> {
                    if (isGridView) {
                        MediaGridContent(
                            folders = state.folders,
                            mediaItems = currentFolder?.items ?: emptyList(),
                            onFolderClick = { viewModel.navigateInto(it) },
                            onMediaClick = onVideoClick
                        )
                    } else {
                        MediaListContent(
                            folders = state.folders,
                            mediaItems = currentFolder?.items ?: emptyList(),
                            onFolderClick = { viewModel.navigateInto(it) },
                            onMediaClick = onVideoClick,
                            onConvertClick = { viewModel.convertToAudio(it) }
                        )
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
fun MediaGridContent(
    folders: List<MediaFolder>,
    mediaItems: List<MediaSource>,
    onFolderClick: (MediaFolder) -> Unit,
    onMediaClick: (MediaSource) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show folders first
        items(folders) { folder ->
            FolderItemGrid(folder = folder, onClick = { onFolderClick(folder) })
        }
        
        // Then show media items
        items(mediaItems) { media ->
            MediaGridItem(media = media, onClick = { onMediaClick(media) })
        }
    }
}

@Composable
fun MediaListContent(
    folders: List<MediaFolder>,
    mediaItems: List<MediaSource>,
    onFolderClick: (MediaFolder) -> Unit,
    onMediaClick: (MediaSource) -> Unit,
    onConvertClick: (MediaSource) -> Unit
) {
    LazyColumn {
        items(folders) { folder ->
            ListItem(
                headlineContent = { Text(folder.name, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("${folder.mediaCount} items") },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onFolderClick(folder) }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        
        items(mediaItems) { media ->
            val isVideo = media.uri.toString().contains("video")
            ListItem(
                headlineContent = { Text(media.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(if (isVideo) "Video" else "Audio", style = MaterialTheme.typography.labelSmall) },
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(56.dp, 40.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (isVideo) {
                            AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Crop)
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
                modifier = Modifier.clickable { onMediaClick(media) }
            )
            Divider(modifier = Modifier.padding(start = 88.dp, end = 16.dp))
        }
    }
}

@Composable
fun FolderItemGrid(folder: MediaFolder, onClick: () -> Unit) {
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
                modifier = Modifier.size(64.dp),
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
                text = "${folder.mediaCount} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = media.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
