package to.kuudere.anisuge.screens.update

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import to.kuudere.anisuge.platform.AppVersion
import to.kuudere.anisuge.platform.AppBuildNumber
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.services.UpdateService

data class UpdateState(
    val currentVersion: String = "$AppVersion ($AppBuildNumber)",
    val newVersion: String = "",
    val changelog: List<String> = emptyList(),
    val isUpdateAvailable: Boolean? = null, // null = checking, true = yes, false = no
    val downloadUrl: String? = null,
    val isCritical: Boolean = false
)

class UpdateViewModel(private val updateService: UpdateService) : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    init {
        checkUpdate()
    }

    private fun checkUpdate() = viewModelScope.launch {
        val response = updateService.checkUpdate()
        
        if (response == null || response.success == false) {
            _state.value = _state.value.copy(isUpdateAvailable = false)
            return@launch
        }

        val remoteBuild = response.build ?: 0
        val isAvailable = remoteBuild > AppBuildNumber

        _state.value = _state.value.copy(
            newVersion = "${response.version ?: ""} (${response.build ?: ""})",
            changelog = response.changelog ?: response.message ?: emptyList(),
            isUpdateAvailable = isAvailable,
            downloadUrl = response.downloadUrl,
            isCritical = response.critical == true
        )
    }
}
