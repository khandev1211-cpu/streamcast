package com.streamcast.feature.library.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.streamcast.core.player.MediaSource

@Composable
fun LocalScreen(
    onMediaClick: (MediaSource) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Videos", "IPTV", "Live")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTabIndex) {
            0 -> LibraryScreen(onVideoClick = onMediaClick)
            1 -> IptvSubTab(onChannelClick = onMediaClick)
            2 -> LiveSubTab(onChannelClick = onMediaClick)
        }
    }
}

@Composable
fun IptvSubTab(onChannelClick: (MediaSource) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Text("IPTV Sub-tab Content (Placeholder)", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}

@Composable
fun LiveSubTab(onChannelClick: (MediaSource) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Text("Live Sub-tab Content (Placeholder)", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}
