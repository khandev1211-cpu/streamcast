package com.streamcast.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.core.player.MediaSource
import com.streamcast.feature.library.data.ConversionState
import com.streamcast.feature.library.data.LibraryRepository
import com.streamcast.feature.library.data.MediaConverter
import com.streamcast.feature.library.domain.model.MediaFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val converter: MediaConverter
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState?>(null)
    val conversionState: StateFlow<ConversionState?> = _conversionState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val folders = repository.getMediaFolders()
                _uiState.value = LibraryUiState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load media")
            }
        }
    }

    fun convertToAudio(video: MediaSource) {
        viewModelScope.launch {
            converter.convertVideoToAudio(video.uri, "Converted_${video.displayName}")
                .collectLatest { state ->
                    _conversionState.value = state
                    if (state is ConversionState.Success) {
                        loadMedia()
                    }
                }
        }
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val folders: List<MediaFolder>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
