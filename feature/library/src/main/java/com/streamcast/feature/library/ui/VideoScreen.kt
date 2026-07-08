package com.streamcast.feature.library.ui

import androidx.compose.runtime.Composable
import com.streamcast.core.player.MediaSource
import com.streamcast.feature.library.viewmodel.LibraryViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VideoScreen(
    onVideoClick: (MediaSource) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    // Reuse LibraryScreen logic but filtered for video
    LibraryScreen(onVideoClick = onVideoClick, viewModel = viewModel)
}
