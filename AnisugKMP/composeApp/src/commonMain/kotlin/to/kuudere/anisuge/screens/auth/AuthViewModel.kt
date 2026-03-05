package to.kuudere.anisuge.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.services.AuthService

data class AuthUiState(
    val isLogin: Boolean       = true,
    val email: String          = "",
    val password: String       = "",
    val displayName: String    = "",
    val isLoading: Boolean     = false,
    val errorMessage: String?  = null,
    val isSuccess: Boolean     = false,
)

class AuthViewModel(private val authService: AuthService) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isLogin      = !_uiState.value.isLogin,
            errorMessage = null,
        )
    }

    fun onEmailChange(v: String)       { _uiState.value = _uiState.value.copy(email = v) }
    fun onPasswordChange(v: String)    { _uiState.value = _uiState.value.copy(password = v) }
    fun onDisplayNameChange(v: String) { _uiState.value = _uiState.value.copy(displayName = v) }
    fun clearError()                   { _uiState.value = _uiState.value.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }
        if (!state.isLogin && state.displayName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your display name")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                if (state.isLogin) {
                    authService.login(state.email, state.password)
                } else {
                    authService.register(state.email, state.password, state.displayName)
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = e.message ?: "Something went wrong",
                )
            }
        }
    }
}
