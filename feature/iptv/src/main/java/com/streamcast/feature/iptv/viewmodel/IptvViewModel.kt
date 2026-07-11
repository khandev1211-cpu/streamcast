package com.streamcast.feature.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.feature.iptv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class IptvViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val sources: StateFlow<List<IptvSource>> = repository.getSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}

sealed class IptvUiState {
    object Idle : IptvUiState()
    object Loading : IptvUiState()
    object Success : IptvUiState()
    data class Error(val message: String) : IptvUiState()
}
