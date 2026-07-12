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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val sportsFolders by viewModel.sportsFolders.collectAsState()
    val movieFolders by viewModel.movieFolders.collectAsState()
    val seriesFolders by viewModel.seriesFolders.collectAsState()
    val countryFolders by viewModel.countryFolders.collectAsState()
    
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val userLiveChannels by viewModel.userLiveChannels.collectAsState()
    
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.resumeHealthChecks()
        onDispose {
            viewModel.pauseHealthChecks()
        }
    }

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
                    val currentTitle = when {
                        selectedCountry != null -> "Country: $selectedCountry"
                        selectedCategory != null -> selectedCategory!!
                        else -> "IPTV & Live"
                    }
                    
                    TopAppBar(
                        title = { Text(currentTitle) },
                        navigationIcon = {
                            if (selectedCategory != null || selectedCountry != null) {
                                IconButton(onClick = { 
                                    viewModel.onCategorySelected(null)
                                    viewModel.onCountrySelected(null)
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { viewModel.refreshAllSources() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                            IconButton(onClick = { showAddSourceDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    )
                }
                
                if (selectedCategory == null && selectedCountry == null) {
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
                    if (searchQuery.isNotEmpty() || selectedCategory != null || selectedCountry != null) {
                        val displayChannels = when {
                            selectedCategory == "Favorites" -> favoriteChannels
                            selectedCategory == "Recently Played" -> recentChannels
                            selectedCategory == "All Channels" -> filteredChannels
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Special Folders
                                item(key = "fav_folder") {
                                    CategoryFolderItem("Favorites", favoriteChannels.size, Icons.Default.Favorite, Color.Red) { viewModel.onCategorySelected("Favorites") }
                                }
                                item(key = "recent_folder") {
                                    CategoryFolderItem("Recent", recentChannels.size, Icons.Default.History, Color.Green) { viewModel.onCategorySelected("Recently Played") }
                                }
                                item(span = { GridItemSpan(2) }, key = "all_folder") {
                                    CategoryFolderItem("All Channels", filteredChannels.size, Icons.Default.Dashboard) { viewModel.onCategorySelected("All Channels") }
                                }

                                if (liveFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, key = "live_header") { SectionHeader("Live TV") }
                                    items(liveFolders, key = { "live_cat_${it.name}" }) { category ->
                                        CategoryFolderItem(category.name, category.channelCount) { viewModel.onCategorySelected(category.name) }
                                    }
                                }

                                if (sportsFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, key = "sports_header") { SectionHeader("Sports") }
                                    items(sportsFolders, key = { "sports_cat_${it.name}" }) { category ->
                                        CategoryFolderItem(category.name, category.channelCount, Icons.Default.SportsSoccer, Color(0xFF2196F3)) { viewModel.onCategorySelected(category.name) }
                                    }
                                }

                                if (countryFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, key = "country_header") { SectionHeader("Browse by Country") }
                                    items(countryFolders, key = { "country_cat_${it.name}" }) { country ->
                                        CategoryFolderItem(country.name.uppercase(), country.channelCount, Icons.Default.Public, Color(0xFF4CAF50)) { viewModel.onCountrySelected(country.name) }
                                    }
                                }

                                if (movieFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, key = "movie_header") { SectionHeader("Movies") }
                                    items(movieFolders, key = { "movie_cat_${it.name}" }) { category ->
                                        CategoryFolderItem(category.name, category.channelCount, Icons.Default.Movie, Color(0xFFFFA000)) { viewModel.onCategorySelected("Movies: ${category.name}") }
                                    }
                                }

                                if (seriesFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, key = "series_header") { SectionHeader("Series") }
                                    items(seriesFolders, key = { "series_cat_${it.name}" }) { category ->
                                        CategoryFolderItem(category.name, category.channelCount, Icons.Default.Tv, Color(0xFF7B1FA2)) { viewModel.onCategorySelected("Series: ${category.name}") }
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
                                Icon(Icons.Default.LiveTv, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Text("No live streams", color = Color.Gray)
                            }
                        }
                    } else {
                        ChannelList(userLiveChannels, onChannelClick, viewModel)
                    }
                }
            }
        }
    }

    if (showAddSourceDialog) {
        if (selectedTabIndex == 0) {
            AddSourceDialog({ showAddSourceDialog = false }) { n, h, u, p ->
                viewModel.addXtreamSource(n, h, u, p)
                showAddSourceDialog = false
            }
        } else {
            AddLiveUrlDialog({ showAddSourceDialog = false }) { n, u ->
                viewModel.addLiveStream(n, u)
                showAddSourceDialog = false
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00A0E9), modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun CategoryFolderItem(name: String, count: Int, icon: ImageVector = Icons.Default.Folder, iconColor: Color = Color(0xFF00A0E9), onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = iconColor)
            Spacer(Modifier.height(4.dp))
            Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text("$count items", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (MediaSource, List<MediaSource>) -> Unit, viewModel: IptvViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels, key = { it.id }) { channel ->
            ChannelItem(channel) { 
                viewModel.preparePlaylist(channels)
                viewModel.markAsPlayed(channel.id)
                onChannelClick(MediaSource(
                    id = channel.id, 
                    uri = android.net.Uri.parse(channel.streamUrl), 
                    type = SourceType.IPTV, 
                    displayName = channel.name, 
                    isCacheable = false, 
                    headers = channel.headers
                ), emptyList())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            TextField(value = query, onValueChange = onQueryChange, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent), singleLine = true)
        },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }
    )
}

@Composable
fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(channel.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(modifier = Modifier.size(8.dp).background(if (channel.lastCheckStatus == 1) Color.Green else if (channel.lastCheckStatus == 2) Color.Red else Color.Gray, CircleShape))
            }
        },
        supportingContent = { 
            Text(listOfNotNull(channel.category, channel.country?.uppercase()).joinToString(" • "), fontSize = 12.sp)
        },
        leadingContent = {
            Surface(modifier = Modifier.size(40.dp), shape = MaterialTheme.shapes.small, color = Color.Gray.copy(alpha = 0.2f)) {
                if (channel.logoUrl != null) AsyncImage(channel.logoUrl, null, modifier = Modifier.fillMaxSize())
                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Tv, null, modifier = Modifier.size(24.dp)) }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var h by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Xtream") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(n, { n = it }, label = { Text("Name") }); TextField(h, { h = it }, label = { Text("Host") })
            TextField(u, { u = it }, label = { Text("User") }); TextField(p, { p = it }, label = { Text("Pass") })
        }
    }, confirmButton = { Button({ onAdd(n, h, u, p) }) { Text("Add") } })
}

@Composable
fun AddLiveUrlDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Live") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(n, { n = it }, label = { Text("Name") }); TextField(u, { u = it }, label = { Text("URL") })
        }
    }, confirmButton = { Button({ onAdd(n, u) }) { Text("Add") } })
}
