package to.kuudere.anisuge.screens.update

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import to.kuudere.anisuge.platform.AppVersion
import to.kuudere.anisuge.platform.AppBuildNumber

data class UpdateState(
    val currentVersion: String = "$AppVersion ($AppBuildNumber)",
    val newVersion: String = "v1.1.0 (2)",
    val changelog: List<String> = listOf(
        "Smoother playback on Linux and Windows",
        "New 'Latest' tab for recently aired anime",
        "Improved search with better genre filtering",
        "Fixed memory leak in video player",
        "Added support for external subtitles"
    ),
    val isUpdateAvailable: Boolean = true
)

class UpdateViewModel : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()
}
