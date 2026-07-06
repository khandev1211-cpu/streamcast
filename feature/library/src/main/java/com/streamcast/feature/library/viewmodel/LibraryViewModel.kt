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

    // Navigation stack for folders
    private val _folderStack = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folderStack: StateFlow<List<MediaFolder>> = _folderStack.asStateFlow()

    private val _isHierarchical = MutableStateFlow(false) // Default to MX Player's "All Folders" flat view
    val isHierarchical: StateFlow<Boolean> = _isHierarchical.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val currentPath = _folderStack.value.lastOrNull()?.path
                val folders = repository.getMediaFolders(currentPath, _isHierarchical.value)
                _uiState.value = LibraryUiState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load media")
            }
        }
    }

    fun navigateInto(folder: MediaFolder) {
        _folderStack.value = _folderStack.value + folder
        loadMedia()
    }

    fun navigateBack(): Boolean {
        if (_folderStack.value.isNotEmpty()) {
            _folderStack.value = _folderStack.value.dropLast(1)
            loadMedia()
            return true
        }
        return false
    }

    fun toggleViewMode() {
        _isHierarchical.value = !_isHierarchical.value
        _folderStack.value = emptyList()
        loadMedia()
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
