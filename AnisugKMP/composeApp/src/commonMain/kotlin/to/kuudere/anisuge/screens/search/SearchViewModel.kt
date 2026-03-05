package to.kuudere.anisuge.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.AnimeItem
import to.kuudere.anisuge.data.services.SearchService

data class SearchUiState(
    val results: List<AnimeItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val keyword: String = "",
    val selectedGenres: List<String> = emptyList(),
    val selectedSeason: String? = null,
    val selectedYear: String? = null,
    val selectedType: String? = null,
    val selectedStatus: String? = null,
    val selectedLanguage: String? = null,
    val selectedRating: String? = null,
)

class SearchViewModel(private val searchService: SearchService) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Initial search
        search()
    }

    fun onKeywordChange(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }

    fun onGenreToggle(genre: String) {
        val current = _uiState.value.selectedGenres
        val next = if (current.contains(genre)) current - genre else current + genre
        _uiState.value = _uiState.value.copy(selectedGenres = next)
    }

    fun onSeasonChange(season: String?) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun onYearChange(year: String?) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
    }

    fun onTypeChange(type: String?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun onStatusChange(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

    fun onLanguageChange(lang: String?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun onRatingChange(rating: String?) {
        _uiState.value = _uiState.value.copy(selectedRating = rating)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedGenres = emptyList(),
            selectedSeason = null,
            selectedYear = null,
            selectedType = null,
            selectedStatus = null,
            selectedLanguage = null,
            selectedRating = null
        )
        search()
    }

    fun search(loadMore: Boolean = false) = viewModelScope.launch {
        val state = _uiState.value
        if (loadMore) {
            if (state.isLoadingMore || !state.hasMore) return@launch
            _uiState.value = state.copy(isLoadingMore = true)
        } else {
            _uiState.value = state.copy(isLoading = true, results = emptyList(), currentPage = 1)
        }

        val currentState = _uiState.value
        val response = searchService.search(
            keyword = currentState.keyword.ifBlank { null },
            page = currentState.currentPage,
            genres = currentState.selectedGenres,
            season = currentState.selectedSeason,
            year = currentState.selectedYear,
            type = currentState.selectedType,
            status = currentState.selectedStatus,
            language = currentState.selectedLanguage,
            rating = currentState.selectedRating
        )

        if (response != null && response.success) {
            val newItems = response.animeData ?: emptyList()
            _uiState.value = _uiState.value.copy(
                results = if (loadMore) _uiState.value.results + newItems else newItems,
                isLoading = false,
                isLoadingMore = false,
                hasMore = response.hasMore ?: false,
                currentPage = if (newItems.isNotEmpty() && (response.hasMore ?: false)) currentState.currentPage + 1 else currentState.currentPage
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false
            )
        }
    }
}
