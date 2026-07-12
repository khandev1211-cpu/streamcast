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
    val countryFolders by viewModel.countryFolders.collectAsState()
    
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val userLiveChannels by viewModel.userLiveChannels.collectAsState()
    
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
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
                            IconButton(onClick = { showCountryPickerDialog = true }) {
                                Icon(Icons.Default.Language, contentDescription = "All Countries")
                            }
                            IconButton(onClick = { viewModel.refreshAllSources() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                                        count = filteredChannels.size,
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

                                // 3. Browse by Country
                                if (countryFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        SectionHeader("Browse by Country")
                                    }
                                    items(countryFolders) { country ->
                                        CategoryFolderItem(
                                            name = country.name.uppercase(),
                                            count = country.channelCount,
                                            icon = Icons.Default.Public,
                                            iconColor = Color(0xFF4CAF50),
                                            onClick = { viewModel.onCountrySelected(country.name) }
                                        )
                                    }
                                }

                                // 4. VOD / Movies Section
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

                                // 5. TV Series Section
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

    if (showCountryPickerDialog) {
        CountryPickerDialog(
            onDismiss = { showCountryPickerDialog = false },
            onSelect = { 
                viewModel.importRemoteCountry(it)
                showCountryPickerDialog = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val countries = listOf(
        "af" to "Afghanistan", "al" to "Albania", "dz" to "Algeria", "as" to "American Samoa", "ad" to "Andorra",
        "ao" to "Angola", "ai" to "Anguilla", "aq" to "Antarctica", "ag" to "Antigua and Barbuda", "ar" to "Argentina",
        "am" to "Armenia", "aw" to "Aruba", "au" to "Australia", "at" to "Austria", "az" to "Azerbaijan",
        "bs" to "Bahamas", "bh" to "Bahrain", "bd" to "Bangladesh", "bb" to "Barbados", "by" to "Belarus",
        "be" to "Belgium", "bz" to "Belize", "bj" to "Benin", "bm" to "Bermuda", "bt" to "Bhutan",
        "bo" to "Bolivia", "ba" to "Bosnia and Herzegovina", "bw" to "Botswana", "br" to "Brazil", "io" to "British Indian Ocean Territory",
        "bn" to "Brunei Darussalam", "bg" to "Bulgaria", "bf" to "Burkina Faso", "bi" to "Burundi", "kh" to "Cambodia",
        "cm" to "Cameroon", "ca" to "Canada", "cv" to "Cape Verde", "ky" to "Cayman Islands", "cf" to "Central African Republic",
        "td" to "Chad", "cl" to "Chile", "cn" to "China", "cx" to "Christmas Island", "cc" to "Cocos (Keeling) Islands",
        "co" to "Colombia", "km" to "Comoros", "cg" to "Congo", "cd" to "Congo, The Democratic Republic of The", "ck" to "Cook Islands",
        "cr" to "Costa Rica", "ci" to "Cote D'ivoire", "hr" to "Croatia", "cu" to "Cuba", "cy" to "Cyprus",
        "cz" to "Czech Republic", "dk" to "Denmark", "dj" to "Djibouti", "dm" to "Dominica", "do" to "Dominican Republic",
        "ec" to "Ecuador", "eg" to "Egypt", "sv" to "El Salvador", "gq" to "Equatorial Guinea", "er" to "Eritrea",
        "ee" to "Estonia", "et" to "Ethiopia", "fk" to "Falkland Islands (Malvinas)", "fo" to "Faroe Islands", "fj" to "Fiji",
        "fi" to "Finland", "fr" to "France", "gf" to "French Guiana", "pf" to "French Polynesia", "tf" to "French Southern Territories",
        "ga" to "Gabon", "gm" to "Gambia", "ge" to "Georgia", "de" to "Germany", "gh" to "Ghana",
        "gi" to "Gibraltar", "gr" to "Greece", "gl" to "Greenland", "gd" to "Grenada", "gp" to "Guadeloupe",
        "gu" to "Guam", "gt" to "Guatemala", "gn" to "Guinea", "gw" to "Guinea-bissau", "gy" to "Guyana",
        "ht" to "Haiti", "hm" to "Heard Island and Mcdonald Islands", "va" to "Holy See (Vatican City State)", "hn" to "Honduras", "hk" to "Hong Kong",
        "hu" to "Hungary", "is" to "Iceland", "in" to "India", "id" to "Indonesia", "ir" to "Iran, Islamic Republic of",
        "iq" to "Iraq", "ie" to "Ireland", "il" to "Israel", "it" to "Italy", "jm" to "Jamaica",
        "jp" to "Japan", "jo" to "Jordan", "kz" to "Kazakhstan", "ke" to "Kenya", "ki" to "Kiribati",
        "kp" to "Korea, Democratic People's Republic of", "kr" to "Korea, Republic of", "kw" to "Kuwait", "kg" to "Kyrgyzstan", "la" to "Lao People's Democratic Republic",
        "lv" to "Latvia", "lb" to "Lebanon", "ls" to "Lesotho", "lr" to "Liberia", "ly" to "Libyan Arab Jamahiriya",
        "li" to "Liechtenstein", "lt" to "Lithuania", "lu" to "Luxembourg", "mo" to "Macao", "mk" to "Macedonia, The Former Yugoslav Republic of",
        "mg" to "Madagascar", "mw" to "Malawi", "my" to "Malaysia", "mv" to "Maldives", "ml" to "Mali",
        "mt" to "Malta", "mh" to "Marshall Islands", "mq" to "Martinique", "mr" to "Mauritania", "mu" to "Mauritius",
        "yt" to "Mayotte", "mx" to "Mexico", "fm" to "Micronesia, Federated States of", "md" to "Moldova, Republic of", "mc" to "Monaco",
        "mn" to "Mongolia", "ms" to "Montserrat", "ma" to "Morocco", "mz" to "Mozambique", "mm" to "Myanmar",
        "na" to "Namibia", "nr" to "Nauru", "np" to "Nepal", "nl" to "Netherlands", "an" to "Netherlands Antilles",
        "nc" to "New Caledonia", "nz" to "New Zealand", "ni" to "Nicaragua", "ne" to "Niger", "ng" to "Nigeria",
        "nu" to "Niue", "nf" to "Norfolk Island", "mp" to "Northern Mariana Islands", "no" to "Norway", "om" to "Oman",
        "pk" to "Pakistan", "pw" to "Palau", "ps" to "Palestinian Territory, Occupied", "pa" to "Panama", "pg" to "Papua New Guinea",
        "py" to "Paraguay", "pe" to "Peru", "ph" to "Philippines", "pn" to "Pitcairn", "pl" to "Poland",
        "pt" to "Portugal", "pr" to "Puerto Rico", "qa" to "Qatar", "re" to "Reunion", "ro" to "Romania",
        "ru" to "Russian Federation", "rw" to "Rwanda", "sh" to "Saint Helena", "kn" to "Saint Kitts and Nevis", "lc" to "Saint Lucia",
        "pm" to "Saint Pierre and Miquelon", "vc" to "Saint Vincent and The Grenadines", "ws" to "Samoa", "sm" to "San Marino", "st" to "Sao Tome and Principe",
        "sa" to "Saudi Arabia", "sn" to "Senegal", "cs" to "Serbia and Montenegro", "sc" to "Seychelles", "sl" to "Sierra Leone",
        "sg" to "Singapore", "sk" to "Slovakia", "si" to "Slovenia", "sb" to "Solomon Islands", "so" to "Somalia",
        "za" to "South Africa", "gs" to "South Georgia and The South Sandwich Islands", "es" to "Spain", "lk" to "Sri Lanka", "sd" to "Sudan",
        "sr" to "Suriname", "sj" to "Svalbard and Jan Mayen", "sz" to "Swaziland", "se" to "Sweden", "ch" to "Switzerland",
        "sy" to "Syrian Arab Republic", "tw" to "Taiwan, Province of China", "tj" to "Tajikistan", "tz" to "Tanzania, United Republic of", "th" to "Thailand",
        "tl" to "Timor-leste", "tg" to "Togo", "tk" to "Tokelau", "to" to "Tonga", "tt" to "Trinidad and Tobago",
        "tn" to "Tunisia", "tr" to "Turkey", "tm" to "Turkmenistan", "tc" to "Turks and Caicos Islands", "tv" to "Tuvalu",
        "ug" to "Uganda", "ua" to "Ukraine", "ae" to "United Arab Emirates", "uk" to "United Kingdom", "us" to "United States",
        "um" to "United States Minor Outlying Islands", "uy" to "Uruguay", "uz" to "Uzbekistan", "vu" to "Vanuatu", "ve" to "Venezuela",
        "vn" to "Viet Nam", "vg" to "Virgin Islands, British", "vi" to "Virgin Islands, U.S.", "wf" to "Wallis and Futuna", "eh" to "Western Sahara",
        "ye" to "Yemen", "zm" to "Zambia", "zw" to "Zimbabwe"
    )

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isEmpty()) countries
        else countries.filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subscribe to Countries") },
        text = {
            Column {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(filtered) { (code, name) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(code.uppercase()) },
                            modifier = Modifier.clickable { onSelect(code) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp),
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
