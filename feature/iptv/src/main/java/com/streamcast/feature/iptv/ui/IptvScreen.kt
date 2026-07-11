package com.streamcast.feature.iptv.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    val categoryFolders by viewModel.categoryFolders.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                if (isSearching) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onClose = { 
                            isSearching = false
                            viewModel.onSearchQueryChanged("")
                        }
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Text(if (selectedCategory != null && selectedCategory != "All") selectedCategory!! else "IPTV & Live") 
                        },
                        navigationIcon = {
                            if (selectedCategory != null && selectedCategory != "All") {
                                IconButton(onClick = { viewModel.onCategorySelected(null) }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { 
                                viewModel.importLocalDirectory("C:/Users/CHAND COMPUTER/Desktop/iptv-master/streams")
                            }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Import Local")
                            }
                            IconButton(onClick = { showAddSourceDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    )
                }
                
                if (selectedCategory == null || selectedCategory == "All") {
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> {
                    if (searchQuery.isNotEmpty() || selectedCategory != null) {
                        // Channel List Mode (Search or Selected Category)
                        ChannelList(channels = filteredChannels, onChannelClick = onChannelClick, viewModel = viewModel)
                    } else {
                        // Folder View Mode
                        if (categoryFolders.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Master Folder
                                item {
                                    CategoryFolderItem(
                                        name = "All Channels",
                                        count = categoryFolders.sumOf { it.channelCount },
                                        onClick = { viewModel.onCategorySelected("All") }
                                    )
                                }
                                items(categoryFolders) { category ->
                                    CategoryFolderItem(
                                        name = category.name,
                                        count = category.channelCount,
                                        onClick = { viewModel.onCategorySelected(category.name) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
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
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onAdd = { name, host, user, pass ->
                viewModel.addXtreamSource(name, host, user, pass)
                showAddSourceDialog = false
            }
        )
    }
}

@Composable
fun CategoryFolderItem(name: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF00A0E9)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$count channels",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (MediaSource, List<MediaSource>) -> Unit, viewModel: IptvViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels) { channel ->
            ChannelItem(
                channel = channel,
                onClick = { 
                    viewModel.preparePlaylist(channels)
                    val currentMedia = MediaSource(
                        id = channel.id,
                        uri = android.net.Uri.parse(channel.streamUrl),
                        type = SourceType.IPTV,
                        displayName = channel.name,
                        isCacheable = false,
                        headers = channel.headers
                    )
                    onChannelClick(currentMedia, emptyList())
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search channels...") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(channel.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = { 
            val meta = listOfNotNull(channel.category, channel.country?.uppercase()).joinToString(" • ")
            Text(meta.ifEmpty { "No category" }, fontSize = 12.sp)
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = Color.Gray.copy(alpha = 0.2f)
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
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
