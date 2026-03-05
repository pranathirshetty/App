package to.kuudere.anisuge.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.services.AuthService
import to.kuudere.anisuge.data.models.SessionCheckResult

sealed interface SplashDestination {
    data object Waiting : SplashDestination
    data object GoAuth  : SplashDestination
    data object GoHome  : SplashDestination
}

class SplashViewModel(private val authService: AuthService) : ViewModel() {
    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Waiting)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() = viewModelScope.launch {
        _destination.value = when (authService.checkSession()) {
            is SessionCheckResult.Valid -> SplashDestination.GoHome
            else                        -> SplashDestination.GoAuth
        }
    }
}
