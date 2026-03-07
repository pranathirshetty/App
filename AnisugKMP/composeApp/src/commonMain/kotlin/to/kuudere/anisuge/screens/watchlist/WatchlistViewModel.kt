package to.kuudere.anisuge.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import to.kuudere.anisuge.AppComponent
import to.kuudere.anisuge.data.models.AnimeItem

data class WatchlistState(
    val selectedFolder: String = "All",
    val items: List<AnimeItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null
)

class WatchlistViewModel : ViewModel() {
    private val watchlistService = AppComponent.watchlistService

    private val _uiState = MutableStateFlow(WatchlistState())
    val uiState: StateFlow<WatchlistState> = _uiState.asStateFlow()

    init {
        fetchWatchlist()
    }

    fun onFolderChange(folder: String) {
        _uiState.update { it.copy(selectedFolder = folder, items = emptyList()) }
        fetchWatchlist()
    }

    private fun fetchWatchlist(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val folderParam = if (_uiState.value.selectedFolder == "All") null else _uiState.value.selectedFolder
                val response = watchlistService.getWatchlist(page = page, folder = folderParam)
                if (response != null) {
                    _uiState.update { it.copy(
                        items = if (page == 1) response.items else it.items + response.items,
                        isLoading = false
                    ) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load watchlist") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateAnimeStatus(animeId: String, newFolder: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            val success = watchlistService.updateStatus(animeId, newFolder)
            if (success) {
                // Remove from list if current folder doesn't match new status
                val currentFolder = _uiState.value.selectedFolder
                if (currentFolder != "All" && currentFolder != newFolder) {
                    _uiState.update { state -> 
                        state.copy(items = state.items.filter { it.activeId != animeId && it.id != animeId }) 
                    }
                } else {
                    // Update item status in place
                    _uiState.update { state ->
                        val updatedItems = state.items.map { 
                            if (it.activeId == animeId || it.id == animeId) it.copy(folder = newFolder) else it 
                        }
                        state.copy(items = updatedItems)
                    }
                }
            }
            _uiState.update { it.copy(isUpdating = false) }
        }
    }
}
