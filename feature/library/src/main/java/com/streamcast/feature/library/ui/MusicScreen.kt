package com.streamcast.feature.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamcast.core.player.MediaSource
import com.streamcast.feature.library.viewmodel.LibraryViewModel

@Composable
fun MusicScreen(
    onMediaClick: (MediaSource, List<MediaSource>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.setMediaType("audio")
    }
    // Reuse LibraryScreen logic but filtered for audio
    LibraryScreen(onVideoClick = onMediaClick, viewModel = viewModel)
}
