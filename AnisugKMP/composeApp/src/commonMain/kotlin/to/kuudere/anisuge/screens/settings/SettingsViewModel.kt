package to.kuudere.anisuge.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.MfaStatusData
import to.kuudere.anisuge.data.models.SessionInfoResponse
import to.kuudere.anisuge.data.models.TotpSetupData
import to.kuudere.anisuge.data.models.UserPreferences
import to.kuudere.anisuge.data.services.SettingsService
import to.kuudere.anisuge.data.services.SettingsStore

data class SettingsUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Preferences
    val preferences: UserPreferences = UserPreferences(),
    val hasPreferencesChanges: Boolean = false,

    // Sessions
    val sessions: List<SessionInfoResponse> = emptyList(),
    val currentSession: SessionInfoResponse? = null,

    // MFA/Security
    val mfaStatus: MfaStatusData? = null,
    val isLoadingMfa: Boolean = false,
    val totpSetupData: TotpSetupData? = null,
    val recoveryCodes: List<String> = emptyList(),
    val showRecoveryCodes: Boolean = false,

    // Password change
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
)

sealed class SettingsTab {
    data object Preferences : SettingsTab()
    data object Sessions : SettingsTab()
    data object Security : SettingsTab()
    data object About : SettingsTab()
}

class SettingsViewModel(
    private val settingsService: SettingsService,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var originalPreferences: UserPreferences = UserPreferences()

    init {
        loadPreferences()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ==================== Preferences ====================

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = settingsService.getPreferences()
            if (response?.success == true && response.preferences != null) {
                originalPreferences = response.preferences
                _uiState.update {
                    it.copy(
                        preferences = response.preferences,
                        isLoading = false,
                        hasPreferencesChanges = false
                    )
                }
                // Also sync local settings store
                response.preferences.let { prefs ->
                    settingsStore.setAutoPlay(prefs.autoPlay)
                    settingsStore.setAutoNext(prefs.autoNext)
                    settingsStore.setAutoSkipIntro(prefs.skipIntro)
                    settingsStore.setAutoSkipOutro(prefs.skipOutro)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response?.message ?: "Failed to load preferences"
                    )
                }
            }
        }
    }

    fun savePreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val response = settingsService.updatePreferences(_uiState.value.preferences)
            if (response?.success == true) {
                originalPreferences = _uiState.value.preferences
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasPreferencesChanges = false,
                        successMessage = "Preferences saved successfully"
                    )
                }
                // Sync local settings
                _uiState.value.preferences.let { prefs ->
                    settingsStore.setAutoPlay(prefs.autoPlay)
                    settingsStore.setAutoNext(prefs.autoNext)
                    settingsStore.setAutoSkipIntro(prefs.skipIntro)
                    settingsStore.setAutoSkipOutro(prefs.skipOutro)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = response?.message ?: "Failed to save preferences"
                    )
                }
            }
        }
    }

    fun updatePreference(updater: (UserPreferences) -> UserPreferences) {
        _uiState.update { state ->
            val newPrefs = updater(state.preferences)
            state.copy(
                preferences = newPrefs,
                hasPreferencesChanges = newPrefs != originalPreferences
            )
        }
    }

    fun setDefaultComments(enabled: Boolean) = updatePreference { it.copy(defaultComments = enabled) }
    fun setDefaultLang(enabled: Boolean) = updatePreference { it.copy(defaultLang = enabled) }
    fun setAutoNext(enabled: Boolean) = updatePreference { it.copy(autoNext = enabled) }
    fun setAutoPlay(enabled: Boolean) = updatePreference { it.copy(autoPlay = enabled) }
    fun setSkipIntro(enabled: Boolean) = updatePreference { it.copy(skipIntro = enabled) }
    fun setSkipOutro(enabled: Boolean) = updatePreference { it.copy(skipOutro = enabled) }
    fun setSyncPercentage(percentage: Int) = updatePreference { it.copy(syncPercentage = percentage.coerceIn(50, 100)) }

    // ==================== Sessions ====================

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = settingsService.getSessions()
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        sessions = response.sessions,
                        currentSession = response.currentSession,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response?.message ?: "Failed to load sessions"
                    )
                }
            }
        }
    }

    fun deleteSession(sessionId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = settingsService.deleteSession(sessionId)
            if (response?.success == true) {
                // Remove from list locally
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filter { it.id != sessionId && it.id != "current" },
                        isLoading = false,
                        successMessage = "Session ended successfully"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response?.message ?: "Failed to end session"
                    )
                }
            }
        }
    }

    fun deleteAllSessions(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = settingsService.deleteAllSessions()
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        sessions = emptyList(),
                        isLoading = false,
                        successMessage = "All sessions ended"
                    )
                }
                onLoggedOut()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response?.message ?: "Failed to end all sessions"
                    )
                }
            }
        }
    }

    // ==================== MFA / Security ====================

    fun loadMfaStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMfa = true) }
            val response = settingsService.getMfaStatus()
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        mfaStatus = response.data,
                        isLoadingMfa = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMfa = false,
                        errorMessage = response?.message ?: "Failed to load MFA status"
                    )
                }
            }
        }
    }

    fun toggleMfa(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMfa = true) }
            val response = settingsService.toggleMfa(enabled)
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        mfaStatus = response.data,
                        isLoadingMfa = false,
                        successMessage = if (enabled) "MFA enabled" else "MFA disabled"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMfa = false,
                        errorMessage = response?.message ?: "Failed to update MFA"
                    )
                }
            }
        }
    }

    fun setupTotp(onQrCodeReady: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMfa = true) }
            val response = settingsService.setupTotp()
            val totpData = response?.data?.authenticator
            if (response?.success == true && totpData != null) {
                _uiState.update {
                    it.copy(
                        totpSetupData = totpData,
                        isLoadingMfa = false
                    )
                }
                onQrCodeReady(totpData.qrCode)
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMfa = false,
                        errorMessage = response?.message ?: "Failed to setup TOTP"
                    )
                }
            }
        }
    }

    fun verifyTotp(code: String, onVerified: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMfa = true) }
            val response = settingsService.verifyTotp(code)
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        isLoadingMfa = false,
                        successMessage = "TOTP verified successfully",
                        totpSetupData = null
                    )
                }
                loadMfaStatus()
                loadRecoveryCodes()
                onVerified()
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMfa = false,
                        errorMessage = response?.message ?: "Invalid verification code"
                    )
                }
            }
        }
    }

    fun loadRecoveryCodes() {
        viewModelScope.launch {
            val response = settingsService.getRecoveryCodes()
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        recoveryCodes = response.codes,
                        showRecoveryCodes = true
                    )
                }
            }
        }
    }

    fun dismissRecoveryCodes() {
        _uiState.update { it.copy(showRecoveryCodes = false) }
    }

    fun dismissTotpSetup() {
        _uiState.update { it.copy(totpSetupData = null) }
    }

    // ==================== Password Change ====================

    fun setCurrentPassword(password: String) {
        _uiState.update { it.copy(currentPassword = password) }
    }

    fun setNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password) }
    }

    fun setConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password) }
    }

    fun changePassword(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.newPassword != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "Passwords do not match") }
                return@launch
            }
            if (state.newPassword.length < 8) {
                _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
                return@launch
            }

            _uiState.update { it.copy(isChangingPassword = true) }
            val response = settingsService.changePassword(
                state.currentPassword,
                state.newPassword,
                state.confirmPassword
            )
            if (response?.success == true) {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Password changed successfully"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        errorMessage = response?.message ?: "Failed to change password"
                    )
                }
            }
        }
    }
}
