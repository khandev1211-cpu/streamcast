package com.streamcast.feature.library.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.streamcast.core.player.MediaSource

@Composable
fun IptvScreen(
    onChannelClick: (MediaSource) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "IPTV & Live Streams", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
