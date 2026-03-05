package to.kuudere.anisuge.screens.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.AnimeItem
import to.kuudere.anisuge.data.models.ContinueWatchingItem
import to.kuudere.anisuge.data.services.HomeService

data class HomeUiState(
    val isLoading:        Boolean              = true,
    val topAiring:        List<AnimeItem>       = emptyList(),
    val latestEpisodes:   List<AnimeItem>       = emptyList(),
    val newOnSite:        List<AnimeItem>       = emptyList(),
    val topUpcoming:      List<AnimeItem>       = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val error:            String?               = null,
)

class HomeViewModel(private val homeService: HomeService) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init { refresh() }

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val homeData = homeService.fetchHomeData()
                val continueWatching = homeService.fetchContinueWatching()
                _uiState.value = HomeUiState(
                    isLoading        = false,
                    topAiring        = homeData?.topAired    ?: emptyList(),
                    latestEpisodes   = homeData?.latestEps   ?: emptyList(),
                    newOnSite        = homeData?.lastUpdated ?: emptyList(),
                    topUpcoming      = homeData?.topUpcoming ?: emptyList(),
                    continueWatching = continueWatching,
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false, error = e.message)
            }
        }
    }
}
