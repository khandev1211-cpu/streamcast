package com.streamcast.feature.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    
    val liveFolders by viewModel.liveFolders.collectAsState()
    val movieFolders by viewModel.movieFolders.collectAsState()
    val seriesFolders by viewModel.seriesFolders.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val userLiveChannels by viewModel.userLiveChannels.collectAsState()
    
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
                            Text(if (selectedCategory != null) selectedCategory!! else "IPTV & Live") 
                        },
                        navigationIcon = {
                            if (selectedCategory != null) {
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
                
                if (selectedCategory == null) {
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
                        val displayChannels = when(selectedCategory) {
                            "Favorites" -> favoriteChannels
                            "Recently Played" -> recentChannels
                            "All Channels" -> filteredChannels
                            else -> filteredChannels
                        }
                        ChannelList(channels = displayChannels, onChannelClick = onChannelClick, viewModel = viewModel)
                    } else {
                        // Folder View Mode
                        if (liveFolders.isEmpty() && movieFolders.isEmpty() && seriesFolders.isEmpty()) {
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
                                // 1. Special Folders
                                item(span = { GridItemSpan(1) }) {
                                    CategoryFolderItem(
                                        name = "Favorites",
                                        count = favoriteChannels.size,
                                        icon = Icons.Default.Favorite,
                                        iconColor = Color.Red,
                                        onClick = { viewModel.onCategorySelected("Favorites") }
                                    )
                                }
                                item(span = { GridItemSpan(1) }) {
                                    CategoryFolderItem(
                                        name = "Recently Played",
                                        count = recentChannels.size,
                                        icon = Icons.Default.History,
                                        iconColor = Color.Green,
                                        onClick = { viewModel.onCategorySelected("Recently Played") }
                                    )
                                }
                                item(span = { GridItemSpan(2) }) {
                                    CategoryFolderItem(
                                        name = "All Channels",
                                        count = liveFolders.sumOf { it.channelCount } + movieFolders.sumOf { it.channelCount } + seriesFolders.sumOf { it.channelCount },
                                        icon = Icons.Default.Dashboard,
                                        onClick = { viewModel.onCategorySelected("All Channels") }
                                    )
                                }

                                // 2. Live TV Section
                                if (liveFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        SectionHeader("Live TV")
                                    }
                                    items(liveFolders) { category ->
                                        CategoryFolderItem(
                                            name = category.name,
                                            count = category.channelCount,
                                            onClick = { viewModel.onCategorySelected(category.name) }
                                        )
                                    }
                                }

                                // 3. VOD / Movies Section
                                if (movieFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        SectionHeader("Movies")
                                    }
                                    items(movieFolders) { category ->
                                        CategoryFolderItem(
                                            name = category.name,
                                            count = category.channelCount,
                                            icon = Icons.Default.Movie,
                                            iconColor = Color(0xFFFFA000),
                                            onClick = { viewModel.onCategorySelected("Movies: ${category.name}") }
                                        )
                                    }
                                }

                                // 4. TV Series Section
                                if (seriesFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        SectionHeader("Series")
                                    }
                                    items(seriesFolders) { category ->
                                        CategoryFolderItem(
                                            name = category.name,
                                            count = category.channelCount,
                                            icon = Icons.Default.Tv,
                                            iconColor = Color(0xFF7B1FA2),
                                            onClick = { viewModel.onCategorySelected("Series: ${category.name}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (userLiveChannels.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Text("No live streams added yet", color = Color.Gray)
                                TextButton(onClick = { showAddSourceDialog = true }) {
                                    Text("Add Your First Stream")
                                }
                            }
                        }
                    } else {
                        ChannelList(channels = userLiveChannels, onChannelClick = onChannelClick, viewModel = viewModel)
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
            AddLiveUrlDialog(
                onDismiss = { showAddSourceDialog = false },
                onAdd = { name, url ->
                    viewModel.addLiveStream(name, url)
                    showAddSourceDialog = false
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        color = Color(0xFF00A0E9)
    )
}

@Composable
fun CategoryFolderItem(
    name: String, 
    count: Int, 
    icon: ImageVector = Icons.Default.Folder,
    iconColor: Color = Color(0xFF00A0E9),
    onClick: () -> Unit
) {
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
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconColor
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
                text = "$count items",
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
                    viewModel.markAsPlayed(channel.id)
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
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(channel.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val statusColor = when(channel.lastCheckStatus) {
                    1 -> Color.Green
                    2 -> Color.Red
                    else -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        },
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

@Composable
fun AddLiveUrlDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Live Stream") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Channel Name") })
                TextField(value = url, onValueChange = { url = it }, label = { Text("Stream URL (.m3u8, .mpd)") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, url) }) {
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
