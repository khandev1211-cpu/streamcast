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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry = _selectedCountry.asStateFlow()

    private val allChannels = repository.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<IptvUiState>(IptvUiState.Idle)
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    val sources: StateFlow<List<IptvSource>> = repository.getSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Combined init logic
        viewModelScope.launch {
            // 1. Generate Mock EPG off-thread
            val currentTime = System.currentTimeMillis()
            val mockPrograms = mutableListOf<EpgProgram>()
            repository.getAllChannels().first().take(20).forEach { channel ->
                 mockPrograms.add(EpgProgram(
                    channelId = channel.id,
                    title = "Current: ${channel.name} Special",
                    description = "Watching live broadcast.",
                    startTime = currentTime - 1800000,
                    endTime = currentTime + 1800000
                ))
            }
            epgDao.insertAll(mockPrograms)

            // 2. Preload defaults if empty
            repository.getSources().first().let { list ->
                if (list.isEmpty()) {
                    preloadDefaults()
                }
            }
        }
        resumeHealthChecks()
    }

    private fun generateMockEpg() {
        // Mocking moved to init block
    }

    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        _searchQuery,
        _selectedCategory,
        _selectedCountry
    ) { channels, query, category, country ->
        // Perform filtering in a background-friendly way (not actually on IO here but Flow handles it)
        channels.filter { channel ->
            val matchesQuery = query.isEmpty() || channel.name.contains(query, ignoreCase = true)
            
            val isSportsByName = channel.name.contains("Sports", ignoreCase = true) || 
                               channel.name.contains("Ten Sports", ignoreCase = true) ||
                               channel.name.contains("PTV Sports", ignoreCase = true)

            val matchesCategory = query.isNotEmpty() || 
                                 category == null || 
                                 category == "All" || 
                                 category == "All Channels" || 
                                 category == "Favorites" || 
                                 category == "Recently Played" || 
                                 channel.category == category ||
                                 (category == "Sports" && isSportsByName)
            
            val matchesCountry = when (country) {
                null -> true
                "Unknown" -> channel.country == null
                else -> channel.country == country
            }

            matchesQuery && matchesCategory && matchesCountry
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class IptvCategory(val name: String, val channelCount: Int)

    // Move heavy grouping to background thread
    val categoryFolders: StateFlow<List<IptvCategory>> = allChannels
        .map { channels ->
            if (channels.isEmpty()) return@map emptyList()
            // Offload heavy grouping
            channels.groupBy { it.category ?: "Uncategorized" }
                .map { (name, list) -> IptvCategory(name, list.size) }
                .sortedBy { it.name }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveFolders = categoryFolders.map { list ->
        list.filter { 
            !it.name.startsWith("Movies:") && 
            !it.name.startsWith("Series:") && 
            !it.name.contains("Sports", ignoreCase = true) 
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sportsFolders = categoryFolders.map { list ->
        list.filter { it.name.contains("Sports", ignoreCase = true) }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieFolders = categoryFolders.map { list ->
        list.filter { it.name.startsWith("Movies:") }
            .map { it.copy(name = it.name.removePrefix("Movies: ")) }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesFolders = categoryFolders.map { list ->
        list.filter { it.name.startsWith("Series:") }
            .map { it.copy(name = it.name.removePrefix("Series: ")) }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countryFolders: StateFlow<List<IptvCategory>> = allChannels.map { channels ->
        if (channels.isEmpty()) return@map emptyList()
        channels.groupBy { it.country ?: "Unknown" }
            .map { (name, list) -> IptvCategory(name, list.size) }
            .sortedBy { it.name }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels = repository.getRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userLiveChannels: StateFlow<List<Channel>> = allChannels.map { channels ->
        channels.filter { it.sourceId == "user_live_streams" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var healthCheckJob: Job? = null

    fun pauseHealthChecks() {
        healthCheckJob?.cancel()
    }

    // Turbo-charged Health System: Prioritizes visible channels while scrolling
    fun resumeHealthChecks() {
        if (healthCheckJob?.isActive == true) return
        healthCheckJob = viewModelScope.launch {
            // Faster startup delay
            delay(2000) 
            
            combine(allChannels, filteredChannels) { all, filtered ->
                all to filtered
            }.collect { (all, filtered) ->
                // Priority 1: Check channels the user is currently looking at (the "Filtered" list)
                // We check 5 at a time now for a "Faster" feel
                val visibleToCheck = filtered.filter { it.lastCheckStatus == 0 }.take(8)
                
                if (visibleToCheck.isNotEmpty()) {
                    visibleToCheck.forEach {
                        repository.checkChannelHealth(it)
                        delay(100) // Micro-delay to prevent UI jank
                    }
                } else {
                    // Priority 2: If visible are done, check background channels in bulk
                    val backgroundToCheck = all.filter { it.lastCheckStatus == 0 }.take(5)
                    backgroundToCheck.forEach {
                        repository.checkChannelHealth(it)
                        delay(200)
                    }
                }
                
                // Wait a bit before next scan cycle to keep battery usage low
                delay(3000)
            }
        }
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
            ),
            IptvSource(
                id = "default_sports",
                name = "Global Sports",
                type = "M3U_REMOTE",
                playlistUrl = "https://iptv-org.github.io/iptv/categories/sports.m3u",
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
        
        // Add specific working PTV/ARY links to Live tab for testing
        addLiveStream("ARY Digital (HD)", "https://6zklx4wryw9b-hls-live.5centscdn.com/arydigital/498f1704b692c3ad4dbfdf5ba5d04536.sdp/playlist.m3u8")
        addLiveStream("PTV Sports (Premium)", "https://tvsen5.aynaott.com/Ptvsports/index.m3u8")
        addLiveStream("Ten Sports (Premium)", "http://121.91.61.106:8000/play/a04h/index.m3u8")
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
