package com.streamcast.feature.iptv.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
        containerColor = Color.Black,
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
                        title = { Text(currentTitle, fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            if (selectedCategory != null || selectedCountry != null) {
                                IconButton(onClick = { 
                                    viewModel.onCategorySelected(null)
                                    viewModel.onCountrySelected(null)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Black,
                        contentColor = Color(0xFF00A0E9),
                        divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        title, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTabIndex == index) Color(0xFF00A0E9) else Color.Gray
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
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
                            IptvShimmerGrid()
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Special Folders
                                item(key = "fav_folder") {
                                    CategoryFolderItem("Favorites", favoriteChannels.size, Icons.Default.Favorite, Color(0xFFFF4B4B)) { viewModel.onCategorySelected("Favorites") }
                                }
                                item(key = "recent_folder") {
                                    CategoryFolderItem("Recent", recentChannels.size, Icons.Default.History, Color(0xFF4CAF50)) { viewModel.onCategorySelected("Recently Played") }
                                }
                                item(span = { GridItemSpan(2) }, key = "all_folder") {
                                    CategoryFolderItem("All Channels", filteredChannels.size, Icons.Default.Dashboard, Color(0xFF00A0E9), isFullWidth = true) { viewModel.onCategorySelected("All Channels") }
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        Box(modifier = Modifier.size(width = 4.dp, height = 18.dp).background(Color(0xFF00A0E9), RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            title, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = Color.White
        )
    }
}

@Composable
fun CategoryFolderItem(
    name: String, 
    count: Int, 
    icon: ImageVector = Icons.Default.Folder, 
    iconColor: Color = Color(0xFF00A0E9),
    isFullWidth: Boolean = false,
    onClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isFullWidth) 80.dp else 110.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(gradient)) {
            if (isFullWidth) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("$count channels", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = Color.White)
                    Text("$count items", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun IptvShimmerGrid() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (MediaSource, List<MediaSource>) -> Unit, viewModel: IptvViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
            TextField(value = query, onValueChange = onQueryChange, placeholder = { Text("Search channels...") }, modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true)
        },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
    )
}

@Composable
fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    val statusColor = when (channel.lastCheckStatus) {
        1 -> Color.Green
        2 -> Color.Red
        else -> Color.Gray
    }

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name, 
                    fontWeight = FontWeight.SemiBold, 
                    modifier = Modifier.weight(1f), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis, 
                    color = Color.White
                )
                // Pulsing Green dot for active channels
                if (channel.lastCheckStatus == 1) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor.copy(alpha = alpha), CircleShape)
                            .border(1.dp, statusColor, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                }
            }
        },
        supportingContent = { 
            Text(listOfNotNull(channel.category, channel.country?.uppercase()).joinToString(" • "), fontSize = 12.sp, color = Color.Gray)
        },
        leadingContent = {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.05f), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
                if (channel.logoUrl != null) AsyncImage(channel.logoUrl, null, modifier = Modifier.fillMaxSize())
                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Tv, null, modifier = Modifier.size(24.dp), tint = Color.Gray) }
            }
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Black)
    )
}

@Composable
fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var h by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Add Xtream Source") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(n, { n = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()); 
                TextField(h, { h = it }, label = { Text("Host (e.g. http://host:port)") }, modifier = Modifier.fillMaxWidth())
                TextField(u, { u = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth()); 
                TextField(p, { p = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            }
        }, 
        confirmButton = { Button({ onAdd(n, h, u, p) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddLiveUrlDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Add Live Stream") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(n, { n = it }, label = { Text("Stream Name") }, modifier = Modifier.fillMaxWidth()); 
                TextField(u, { u = it }, label = { Text("Stream URL") }, modifier = Modifier.fillMaxWidth())
            }
        }, 
        confirmButton = { Button({ onAdd(n, u) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
