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
import androidx.compose.ui.unit.sp
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
    onVideoClick: (MediaSource, List<MediaSource>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()
    val folderStack by viewModel.folderStack.collectAsState()
    val isHierarchical by viewModel.isHierarchical.collectAsState()
    
    var isGridView by remember { mutableStateOf(false) } // Default to list view as per ref
    val currentFolder = folderStack.lastOrNull()

    BackHandler(enabled = folderStack.isNotEmpty()) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentFolder?.name ?: if (isHierarchical) "Folders" else "Local",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (folderStack.isNotEmpty()) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = { /* Menu */ }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Folder Browse */ }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                    }
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(if (isGridView) Icons.Default.List else Icons.Default.GridView, contentDescription = "Toggle View")
                    }
                }
            )
        },
        floatingActionButton = {
            val successState = uiState as? LibraryUiState.Success
            val lastPlayed = successState?.folders?.flatMap { it.items ?: emptyList() }?.firstOrNull() // Simplified: first item
            
            FloatingActionButton(
                onClick = { 
                    lastPlayed?.let { onVideoClick(it, emptyList()) }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                        val foldersToShow = if (!isHierarchical && currentFolder != null) {
                            emptyList()
                        } else {
                            state.folders
                        }

                    if (isGridView) {
                        MediaGridContent(
                            folders = foldersToShow,
                            mediaItems = currentFolder?.items ?: emptyList(),
                            onFolderClick = { viewModel.navigateInto(it) },
                            onMediaClick = { onVideoClick(it, currentFolder?.items ?: emptyList()) }
                        )
                    } else {
                        MediaListContent(
                            folders = foldersToShow,
                            mediaItems = currentFolder?.items ?: emptyList(),
                            onFolderClick = { viewModel.navigateInto(it) },
                            onMediaClick = { onVideoClick(it, currentFolder?.items ?: emptyList()) },
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
        if (folders.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text("Folders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(folders) { folder ->
                FolderItemGrid(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
        
        if (mediaItems.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text("Videos", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }
            items(mediaItems) { media ->
                MediaGridItem(media = media, onClick = { onMediaClick(media) })
            }
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
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        if (folders.isNotEmpty()) {
            item {
                Text("Folders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
            items(folders) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${folder.mediaCount} folders · 0 GB") }, // Placeholder size
                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
                    modifier = Modifier.clickable { onFolderClick(folder) }
                )
            }
        }
        
        if (mediaItems.isNotEmpty()) {
            item {
                Text("Videos", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
            items(mediaItems) { media ->
                val isVideo = true // Assuming videos in this sub-tab
                ListItem(
                    headlineContent = { Text(media.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("5.3 MB · 22 Jun") }, // Placeholder metadata
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(80.dp, 56.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Crop)
                            }
                            // Duration badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                            ) {
                                Text("01:18", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { /* More Menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    },
                    modifier = Modifier.clickable { onMediaClick(media) }
                )
            }
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
