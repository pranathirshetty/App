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

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val schedule: Map<String, List<ScheduleAnime>> = emptyMap(),
    val error: String? = null,
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = scheduleService.fetchSchedule()
                _uiState.update { it.copy(isLoading = false, schedule = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
