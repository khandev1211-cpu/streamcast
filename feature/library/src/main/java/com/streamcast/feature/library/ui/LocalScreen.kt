package com.streamcast.feature.library.ui

import androidx.compose.runtime.Composable
import com.streamcast.core.player.MediaSource

@Composable
fun LocalScreen(
    onMediaClick: (MediaSource, List<MediaSource>) -> Unit
) {
    LibraryScreen(onVideoClick = onMediaClick)
}
