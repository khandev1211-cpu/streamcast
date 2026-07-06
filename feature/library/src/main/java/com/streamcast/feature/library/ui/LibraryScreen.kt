package com.streamcast.feature.library.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    
    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFolder?.name ?: "StreamCast") },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMedia() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                conversionState?.let { state ->
                    when (state) {
                        is ConversionState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is ConversionState.Success -> Text("Conversion complete!", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                        is ConversionState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                    }
                }

                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is LibraryUiState.Success -> {
                        if (selectedFolder == null) {
                            FolderList(folders = state.folders) { selectedFolder = it }
                        } else {
                            MediaList(
                                items = selectedFolder!!.items,
                                onVideoClick = onVideoClick,
                                onConvertClick = { viewModel.convertToAudio(it) }
                            )
                        }
                    }
                    is LibraryUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderList(folders: List<MediaFolder>, onFolderClick: (MediaFolder) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(folders) { folder ->
            ListItem(
                headlineContent = { Text(folder.name, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("${folder.mediaCount} items") },
                leadingContent = { 
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onFolderClick(folder) }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
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
                    Text(media.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                },
                supportingContent = { 
                    Text(if (isVideo) "Video" else "Audio", style = MaterialTheme.typography.labelSmall) 
                },
                leadingContent = {
                    if (isVideo) {
                        // In a real app, we'd use MediaStore to get thumbnails
                        Icon(Icons.Default.Movie, contentDescription = null)
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    }
                },
                trailingContent = {
                    if (isVideo) {
                        IconButton(onClick = { onConvertClick(media) }) {
                            Icon(Icons.Default.Transform, contentDescription = "Convert")
                        }
                    }
                },
                modifier = Modifier.clickable { onVideoClick(media) }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}
