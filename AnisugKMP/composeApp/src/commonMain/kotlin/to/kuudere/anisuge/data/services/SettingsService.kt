package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import to.kuudere.anisuge.data.models.AniListDisconnectResponse
import to.kuudere.anisuge.data.models.AniListProfileResponse
import to.kuudere.anisuge.data.models.AniListStatusResponse
import to.kuudere.anisuge.data.models.MfaStatusResponse
import to.kuudere.anisuge.data.models.MfaToggleRequest
import to.kuudere.anisuge.data.models.MfaToggleResponse
import to.kuudere.anisuge.data.models.PasswordChangeRequest
import to.kuudere.anisuge.data.models.PasswordChangeResponse
import to.kuudere.anisuge.data.models.PreferencesResponse
import to.kuudere.anisuge.data.models.RecoveryCodesResponse
import to.kuudere.anisuge.data.models.SessionDeleteResponse
import to.kuudere.anisuge.data.models.SessionInfo
import to.kuudere.anisuge.data.models.SessionsResponse
import to.kuudere.anisuge.data.models.TotpSetupResponse
import to.kuudere.anisuge.data.models.TotpVerifyRequest
import to.kuudere.anisuge.data.models.TotpVerifyResponse
import to.kuudere.anisuge.data.models.UserPreferences

class SettingsService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://kuudere.to"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    /** Fetch user preferences from the server */
    suspend fun getPreferences(): PreferencesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/preferences") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<PreferencesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getPreferences error: ${e.message}")
            null
        }
    }

    /** Update user preferences on the server */
    suspend fun updatePreferences(preferences: UserPreferences): PreferencesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.put("$BASE_URL/api/settings/preferences") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(preferences)
            }
            response.body<PreferencesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] updatePreferences error: ${e.message}")
            null
        }
    }

    /** Fetch active sessions */
    suspend fun getSessions(): SessionsResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/sessions") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionsResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getSessions error: ${e.message}")
            null
        }
    }

    /** Delete a specific session */
    suspend fun deleteSession(sessionId: String): SessionDeleteResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/settings/sessions?action=single&sessionId=$sessionId") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionDeleteResponse>()
        } catch (e: Exception) {
            println("[SettingsService] deleteSession error: ${e.message}")
            null
        }
    }

    /** Delete all sessions (logout everywhere) */
    suspend fun deleteAllSessions(): SessionDeleteResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/settings/sessions?action=all") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionDeleteResponse>()
        } catch (e: Exception) {
            println("[SettingsService] deleteAllSessions error: ${e.message}")
            null
        }
    }

    /** Get MFA status */
    suspend fun getMfaStatus(): MfaStatusResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/mfa") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<MfaStatusResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getMfaStatus error: ${e.message}")
            null
        }
    }

    /** Enable/disable MFA */
    suspend fun toggleMfa(enabled: Boolean): MfaToggleResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(MfaToggleRequest(enabled))
            }
            response.body<MfaToggleResponse>()
        } catch (e: Exception) {
            println("[SettingsService] toggleMfa error: ${e.message}")
            null
        }
    }

    /** Setup TOTP (returns secret and QR code URL) */
    suspend fun setupTotp(): TotpSetupResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa/totp") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<TotpSetupResponse>()
        } catch (e: Exception) {
            println("[SettingsService] setupTotp error: ${e.message}")
            null
        }
    }

    /** Verify TOTP code during setup */
    suspend fun verifyTotp(code: String): TotpVerifyResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa/totp/verify") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(TotpVerifyRequest(code))
            }
            response.body<TotpVerifyResponse>()
        } catch (e: Exception) {
            println("[SettingsService] verifyTotp error: ${e.message}")
            null
        }
    }

    /** Get recovery codes */
    suspend fun getRecoveryCodes(): RecoveryCodesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/mfa/recovery") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<RecoveryCodesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getRecoveryCodes error: ${e.message}")
            null
        }
    }

    /** Change password */
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String): PasswordChangeResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/password") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(PasswordChangeRequest(currentPassword, newPassword, confirmPassword))
            }
            response.body<PasswordChangeResponse>()
        } catch (e: Exception) {
            println("[SettingsService] changePassword error: ${e.message}")
            null
        }
    }

    /** Get AniList connection status */
    suspend fun getAniListStatus(): AniListStatusResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/anilist/status") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListStatusResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getAniListStatus error: ${e.message}")
            null
        }
    }

    /** Get AniList profile */
    suspend fun getAniListProfile(): AniListProfileResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/anilist/profile") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListProfileResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getAniListProfile error: ${e.message}")
            null
        }
    }

    /** Disconnect AniList */
    suspend fun disconnectAniList(): AniListDisconnectResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/auth/anilist/disconnect") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListDisconnectResponse>()
        } catch (e: Exception) {
            println("[SettingsService] disconnectAniList error: ${e.message}")
            null
        }
    }

    /** Get AniList OAuth URL */
    fun getAniListAuthUrl(): String {
        return "$BASE_URL/api/auth/anilist/login"
    }
}
