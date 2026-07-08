package com.streamcast.feature.library.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MeScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Me", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        
        ListItem(
            headlineContent = { Text("VPS Configuration") },
            leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
            modifier = Modifier.clickable { /* TODO */ }
        )
        ListItem(
            headlineContent = { Text("Subtitle Defaults") },
            leadingContent = { Icon(Icons.Default.Subtitles, contentDescription = null) },
            modifier = Modifier.clickable { /* TODO */ }
        )
        ListItem(
            headlineContent = { Text("Storage Management") },
            leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
            modifier = Modifier.clickable { /* TODO */ }
        )
    }
}
