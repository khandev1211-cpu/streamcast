package com.streamcast.feature.iptv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import com.streamcast.feature.iptv.viewmodel.IptvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvScreen(
    onChannelClick: (MediaSource, List<MediaSource>) -> Unit,
    viewModel: IptvViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("IPTV", "Live")
    
    val sources by viewModel.sources.collectAsState()
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("IPTV & Live") },
                    actions = {
                        IconButton(onClick = { 
                            // This points to your desktop path as requested
                            viewModel.importLocalDirectory("C:/Users/CHAND COMPUTER/Desktop/iptv-master/streams")
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Import Local")
                        }
                        IconButton(onClick = { showAddSourceDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> {
                    // IPTV Content
                    if (sources.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (selectedSourceId == null) {
                        LazyColumn {
                            items(sources) { source ->
                                ListItem(
                                    headlineContent = { Text(source.name) },
                                    supportingContent = { Text(source.type) },
                                    leadingContent = { Icon(Icons.Default.Tv, contentDescription = null) },
                                    modifier = Modifier.clickable { selectedSourceId = source.id }
                                )
                            }
                        }
                    } else {
                        val channels by viewModel.getChannels(selectedSourceId!!).collectAsState(emptyList())
                        LazyColumn {
                            item {
                                TextButton(onClick = { selectedSourceId = null }) {
                                    Text("< Back to Sources")
                                }
                            }
                            items(channels) { channel ->
                                ListItem(
                                    headlineContent = { Text(channel.name) },
                                    supportingContent = { 
                                        val meta = listOfNotNull(channel.category, channel.country?.uppercase()).joinToString(" • ")
                                        Text(meta.ifEmpty { "No category" })
                                    },
                                    leadingContent = {
                                        if (channel.logoUrl != null) {
                                            AsyncImage(
                                                model = channel.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        } else {
                                            Icon(Icons.Default.Tv, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.clickable { 
                                        val mediaList = channels.map {
                                            MediaSource(
                                                id = it.id,
                                                uri = android.net.Uri.parse(it.streamUrl),
                                                type = SourceType.IPTV,
                                                displayName = it.name,
                                                isCacheable = false
                                            )
                                        }
                                        val currentMedia = MediaSource(
                                            id = channel.id,
                                            uri = android.net.Uri.parse(channel.streamUrl),
                                            type = SourceType.IPTV,
                                            displayName = channel.name,
                                            isCacheable = false
                                        )
                                        onChannelClick(currentMedia, mediaList)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Live Content (Placeholder for now)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(64.dp))
                            Text("User-added Live URLs will appear here")
                        }
                    }
                }
            }
        }
    }

    if (showAddSourceDialog) {
        if (selectedTabIndex == 0) {
            AddSourceDialog(
                onDismiss = { showAddSourceDialog = false },
                onAdd = { name, host, user, pass ->
                    viewModel.addXtreamSource(name, host, user, pass)
                    showAddSourceDialog = false
                }
            )
        } else {
            // TODO: AddLiveUrlDialog
            showAddSourceDialog = false
        }
    }
}

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Xtream Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = host, onValueChange = { host = it }, label = { Text("Host (http://...)") })
                TextField(value = user, onValueChange = { user = it }, label = { Text("Username") })
                TextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, host, user, pass) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
