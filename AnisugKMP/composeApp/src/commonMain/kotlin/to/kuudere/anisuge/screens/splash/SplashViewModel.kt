package to.kuudere.anisuge.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.services.AuthService
import to.kuudere.anisuge.data.models.SessionCheckResult
import to.kuudere.anisuge.data.services.UpdateService
import to.kuudere.anisuge.data.services.HomeService

sealed interface SplashDestination {
    data object Waiting : SplashDestination
    data object GoAuth  : SplashDestination
    data object GoHome  : SplashDestination
    /** We have a stored session but no internet – go home in offline mode */
    data object GoHomeOffline : SplashDestination
}

class SplashViewModel(
    private val authService: AuthService,
    private val updateService: UpdateService,
    private val homeService: HomeService
) : ViewModel() {
    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Waiting)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    private val _status = MutableStateFlow("Initializing...")
    val status: StateFlow<String> = _status.asStateFlow()

    init {
        performInitialChecks()
    }

    private fun performInitialChecks() = viewModelScope.launch {
        // Step 1: Verify User
        _status.value = "Verifying user..."
        val authResult = authService.checkSession()
        
        // Step 2: Check for Updates
        _status.value = "Checking for updates..."
        updateService.checkUpdate() // We just trigger it, navigation in App.kt handles destination
        
        // Step 3: Loading content (prefetch)
        if (authResult is SessionCheckResult.Valid) {
            _status.value = "Loading home data..."
            homeService.fetchHomeData()
        }

        _status.value = "Ready"
        
        // Final destination resolution
        _destination.value = when (authResult) {
            is SessionCheckResult.Valid        -> SplashDestination.GoHome
            is SessionCheckResult.NetworkError -> SplashDestination.GoHomeOffline
            else                               -> SplashDestination.GoAuth
        }
    }
}
