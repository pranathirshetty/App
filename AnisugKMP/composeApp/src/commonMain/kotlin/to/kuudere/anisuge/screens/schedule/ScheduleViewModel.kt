package to.kuudere.anisuge.screens.schedule

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.ScheduleAnime
import to.kuudere.anisuge.data.services.ScheduleService
import to.kuudere.anisuge.utils.isNetworkError

private const val PAGE_SIZE = 3

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val schedule: Map<String, List<ScheduleAnime>> = emptyMap(),
    val hasMore: Boolean = false,
    val loadedDates: Int = 0,
    val totalDates: Int = 0,
    val error: String? = null,
    val isOffline: Boolean = false,
)

class ScheduleViewModel(
    private val scheduleService: ScheduleService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState

    init { refresh() }

    fun refresh() {
        scope.launch {
            _uiState.update { ScheduleUiState(isLoading = true) }
            try {
                val resp = scheduleService.fetchSchedule(limit = PAGE_SIZE, offset = 0)
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        schedule     = resp.data,
                        hasMore      = resp.hasMore,
                        isOffline    = false,
                        loadedDates  = resp.loadedDates ?: resp.data.size,
                        totalDates   = resp.totalDates ?: resp.data.size,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isOffline = e.isNetworkError(), error = if (e.isNetworkError()) null else e.message) }
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore) return
        scope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val resp = scheduleService.fetchSchedule(limit = PAGE_SIZE, offset = current.loadedDates)
                _uiState.update { state ->
                    state.copy(
                        isLoadingMore = false,
                        schedule      = state.schedule + resp.data,   // merge
                        hasMore       = resp.hasMore,
                        loadedDates   = state.loadedDates + (resp.loadedDates ?: resp.data.size),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}
