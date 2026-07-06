package com.streamcast.feature.library.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamcast.core.player.MediaSource
import com.streamcast.feature.library.data.ConversionState
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StreamCast Library") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Conversion Progress indicator
            conversionState?.let { state ->
                when (state) {
                    is ConversionState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is ConversionState.Success -> Text("Conversion complete!", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                    is ConversionState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is LibraryUiState.Success -> {
                        if (state.media.isEmpty()) {
                            Text(
                                text = "No media found on device",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.media) { media ->
                                    VideoItem(
                                        video = media,
                                        onClick = { onVideoClick(media) },
                                        onConvertClick = { viewModel.convertToAudio(media) }
                                    )
                                }
                            }
                        }
                    }
                    is LibraryUiState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItem(
    video: MediaSource,
    onClick: () -> Unit,
    onConvertClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (video.uri.toString().contains("video")) "Video" else "Audio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (video.uri.toString().contains("video")) {
                Button(onClick = onConvertClick) {
                    Text("to Audio", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
