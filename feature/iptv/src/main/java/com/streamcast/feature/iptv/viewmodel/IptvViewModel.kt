package com.streamcast.feature.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.dao.EpgDao
import com.streamcast.core.database.entities.EpgProgram
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import com.streamcast.feature.iptv.data.IptvPlaylistManager
import com.streamcast.feature.iptv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class IptvViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val playlistManager: IptvPlaylistManager,
    private val epgDao: EpgDao
) : ViewModel() {

    init {
        generateMockEpg()
    }

    private fun generateMockEpg() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val mockPrograms = mutableListOf<EpgProgram>()
            // Generate generic mock data for channels
            // (Real implementation would sync from XMLTV/Xtream)
            repository.getAllChannels().first().take(20).forEach { channel: Channel ->
                 mockPrograms.add(EpgProgram(
                    channelId = channel.id,
                    title = "Current: ${channel.name} Special",
                    description = "Watching live broadcast.",
                    startTime = currentTime - 1800000,
                    endTime = currentTime + 1800000
                ))
                mockPrograms.add(EpgProgram(
                    channelId = channel.id,
                    title = "Next: World News Tonight",
                    description = "Evening report.",
                    startTime = currentTime + 1800000,
                    endTime = currentTime + 5400000
                ))
            }
            epgDao.insertAll(mockPrograms)
        }
    }

    val sources: StateFlow<List<IptvSource>> = repository.getSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry = _selectedCountry.asStateFlow()

    private val allChannels = repository.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        _searchQuery,
        _selectedCategory,
        _selectedCountry
    ) { channels, query, category, country ->
        channels.filter { channel ->
            val matchesQuery = query.isEmpty() || channel.name.contains(query, ignoreCase = true)
            // If searching, ignore category filter. Otherwise, check match.
            val matchesCategory = query.isNotEmpty() || 
                                 category == null || 
                                 category == "All" || 
                                 category == "All Channels" || 
                                 category == "Favorites" || 
                                 category == "Recently Played" || 
                                 channel.category == category
            
            val matchesCountry = when (country) {
                null -> true
                "Unknown" -> channel.country == null
                else -> channel.country == country
            }

            matchesQuery && matchesCategory && matchesCountry
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class IptvCategory(val name: String, val channelCount: Int)

    val categoryFolders: StateFlow<List<IptvCategory>> = allChannels.map { channels ->
        val groups = channels.groupBy { it.category ?: "Uncategorized" }
        groups.map { (name, list) -> IptvCategory(name, list.size) }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveFolders = categoryFolders.map { list ->
        list.filter { !it.name.startsWith("Movies:") && !it.name.startsWith("Series:") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieFolders = categoryFolders.map { list ->
        list.filter { it.name.startsWith("Movies:") }
            .map { it.copy(name = it.name.removePrefix("Movies: ")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesFolders = categoryFolders.map { list ->
        list.filter { it.name.startsWith("Series:") }
            .map { it.copy(name = it.name.removePrefix("Series: ")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countryFolders: StateFlow<List<IptvCategory>> = allChannels.map { channels ->
        val groups = channels.groupBy { it.country ?: "Unknown" }
        groups.map { (name, list) -> IptvCategory(name, list.size) }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels = repository.getRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userLiveChannels: StateFlow<List<Channel>> = allChannels.map { channels ->
        channels.filter { it.sourceId == "user_live_streams" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun startBackgroundHealthChecks() {
        viewModelScope.launch {
            allChannels.collect { channels ->
                delay(1000) // Be polite to the UI and network
                // Check channels with unknown status
                // Priority 1: Current filtered channels (what user sees)
                val prioritised = filteredChannels.value.filter { it.lastCheckStatus == 0 }.take(5)
                // Priority 2: Any other unknown channels
                val others = channels.filter { it.lastCheckStatus == 0 }.take(5 - prioritised.size)
                
                (prioritised + others).forEach {
                    repository.checkChannelHealth(it)
                }
            }
        }
    }

    private val _uiState = MutableStateFlow<IptvUiState>(IptvUiState.Idle)
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSources().collect { list ->
                if (list.isEmpty()) {
                    preloadDefaults()
                }
            }
        }
        startBackgroundHealthChecks()
    }

    private suspend fun preloadDefaults() {
        val defaultSources = listOf(
            IptvSource(
                id = "default_news",
                name = "Global News",
                type = "M3U_REMOTE",
                playlistUrl = "https://iptv-org.github.io/iptv/categories/news.m3u",
                host = null,
                username = null,
                password = null,
                lastSyncedAt = null
            ),
            IptvSource(
                id = "default_movies",
                name = "Global Movies",
                type = "M3U_REMOTE",
                playlistUrl = "https://iptv-org.github.io/iptv/categories/movies.m3u",
                host = null,
                username = null,
                password = null,
                lastSyncedAt = null
            ),
            IptvSource(
                id = "default_india",
                name = "India (Premium)",
                type = "M3U_REMOTE",
                playlistUrl = "https://iptv-org.github.io/iptv/countries/in.m3u",
                host = null,
                username = null,
                password = null,
                lastSyncedAt = null
            )
        )
        defaultSources.forEach { 
            repository.addSource(it)
            repository.refreshSource(it.id)
        }
    }

    fun addXtreamSource(name: String, host: String, user: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = IptvUiState.Loading
            val source = IptvSource(
                id = UUID.randomUUID().toString(),
                name = name,
                type = "XTREAM",
                playlistUrl = null,
                host = host,
                username = user,
                password = pass,
                lastSyncedAt = null
            )
            repository.addSource(source)
            repository.refreshSource(source.id)
            _uiState.value = IptvUiState.Success
        }
    }

    fun getChannels(sourceId: String): Flow<List<Channel>> {
        return repository.getChannelsForSource(sourceId)
    }

    fun importLocalDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = IptvUiState.Loading
            try {
                repository.importFromDirectory(path)
                _uiState.value = IptvUiState.Success
            } catch (e: Exception) {
                _uiState.value = IptvUiState.Error(e.message ?: "Failed to import directory")
            }
        }
    }

    fun refreshAllSources() {
        viewModelScope.launch {
            _uiState.value = IptvUiState.Loading
            sources.value.forEach { source ->
                repository.refreshSource(source.id)
            }
            _uiState.value = IptvUiState.Success
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        _selectedCountry.value = null // Clear country when category is picked
    }

    fun onCountrySelected(country: String?) {
        _selectedCountry.value = country
        _selectedCategory.value = null // Clear category when country is picked
    }

    fun importRemoteCountry(countryCode: String) {
        viewModelScope.launch {
            val url = "https://iptv-org.github.io/iptv/countries/${countryCode.lowercase()}.m3u"
            val sourceId = "remote_country_$countryCode"
            repository.addSource(
                IptvSource(
                    id = sourceId,
                    name = "Country: ${countryCode.uppercase()}",
                    type = "M3U_REMOTE",
                    playlistUrl = url,
                    host = null,
                    username = null,
                    password = null,
                    lastSyncedAt = null
                )
            )
            repository.refreshSource(sourceId)
        }
    }

    fun markAsPlayed(channelId: String) {
        viewModelScope.launch {
            repository.updateLastPlayed(channelId)
        }
    }

    fun addLiveStream(name: String, url: String) {
        viewModelScope.launch {
            val sourceId = "user_live_streams"
            // Ensure source exists
            repository.addSource(
                IptvSource(
                    id = sourceId,
                    name = "My Live Streams",
                    type = "LIVE_URL",
                    playlistUrl = null,
                    host = null,
                    username = null,
                    password = null,
                    lastSyncedAt = null
                )
            )
            
            val channel = Channel(
                id = UUID.randomUUID().toString(),
                sourceId = sourceId,
                name = name,
                logoUrl = null,
                category = "Live",
                country = "User",
                streamUrl = url,
                epgChannelId = null
            )
            repository.addChannel(channel)
        }
    }

    fun preparePlaylist(channels: List<Channel>) {
        val mediaList = channels.map {
            MediaSource(
                id = it.id,
                uri = android.net.Uri.parse(it.streamUrl),
                type = SourceType.IPTV,
                displayName = it.name,
                isCacheable = false,
                headers = it.headers
            )
        }
        playlistManager.setPlaylist(mediaList)
    }
}

sealed class IptvUiState {
    object Idle : IptvUiState()
    object Loading : IptvUiState()
    object Success : IptvUiState()
    data class Error(val message: String) : IptvUiState()
}
