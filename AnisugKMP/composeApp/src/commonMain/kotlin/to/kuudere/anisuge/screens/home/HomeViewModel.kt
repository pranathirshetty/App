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

import to.kuudere.anisuge.data.models.SessionCheckResult
import to.kuudere.anisuge.data.models.UserProfile
import to.kuudere.anisuge.data.services.AuthService

import kotlinx.coroutines.flow.update
import to.kuudere.anisuge.data.services.InfoService

data class HomeUiState(
    val isLoading:        Boolean              = true,
    val isLoggingOut:     Boolean              = false,
    val userProfile:      UserProfile?         = null,
    val topAiring:        List<AnimeItem>       = emptyList(),
    val latestEpisodes:   List<AnimeItem>       = emptyList(),
    val newOnSite:        List<AnimeItem>       = emptyList(),
    val topUpcoming:      List<AnimeItem>       = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val isUpdatingWatchlist: Boolean            = false,
    val error:            String?               = null,
)

class HomeViewModel(
    private val homeService: HomeService,
    private val authService: AuthService,
    private val infoService: InfoService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init { refresh() }

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userRes = authService.checkSession()
                val userProfile = (userRes as? SessionCheckResult.Valid)?.user
                
                val homeData = homeService.fetchHomeData()
                val continueWatching = homeService.fetchContinueWatching()
                _uiState.update { 
                    it.copy(
                        isLoading        = false,
                        userProfile      = userProfile,
                        topAiring        = homeData?.topAired    ?: emptyList(),
                        latestEpisodes   = homeData?.latestEps   ?: emptyList(),
                        newOnSite        = homeData?.lastUpdated ?: emptyList(),
                        topUpcoming      = homeData?.topUpcoming ?: emptyList(),
                        continueWatching = continueWatching,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateWatchlist(animeId: String, folder: String) {
        scope.launch {
            _uiState.update { it.copy(isUpdatingWatchlist = true) }
            try {
                infoService.updateWatchlistStatus(animeId, folder)
            } finally {
                _uiState.update { it.copy(isUpdatingWatchlist = false) }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            try {
                authService.logout()
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
                onComplete()
            }
        }
    }
}
