package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userId: String? = null,
    val defaultComments: Boolean = false,
    val defaultLang: Boolean = true,
    val autoNext: Boolean = true,
    val autoPlay: Boolean = false,
    val skipIntro: Boolean = false,
    val skipOutro: Boolean = false,
    val syncPercentage: Int = 80,
    val ads_in_home: Boolean = false,
    val ads_in_player: Boolean = false,
    val ads_in_search: Boolean = false,
    val ads_in_info_page: Boolean = false,
    val ads_in_watch_page: Boolean = false,
    val ad_types: String? = null,
)

@Serializable
data class PreferencesResponse(
    val success: Boolean,
    val message: String? = null,
    val preferences: UserPreferences? = null,
)

@Serializable
data class SessionInfoResponse(
    val id: String,
    val userId: String,
    val clientName: String? = null,
    val clientVersion: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val countryCode: String? = null,
    val countryName: String? = null,
    val ip: String? = null,
    val current: Boolean = false,
    val createdAt: String,
    val expiresAt: String? = null,
)

@Serializable
data class SessionsResponse(
    val success: Boolean,
    val message: String? = null,
    val sessions: List<SessionInfoResponse> = emptyList(),
    val currentSession: SessionInfoResponse? = null,
)

@Serializable
data class SessionDeleteResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class MfaStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MfaStatusData? = null,
)

@Serializable
data class MfaStatusData(
    val mfaEnabled: Boolean,
    val totpEnabled: Boolean,
)

@Serializable
data class MfaToggleRequest(
    val enabled: Boolean,
)

@Serializable
data class MfaToggleResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MfaStatusData? = null,
)

@Serializable
data class TotpSetupResponse(
    val success: Boolean,
    val message: String? = null,
    val data: TotpAuthenticatorWrapper? = null,
)

@Serializable
data class TotpAuthenticatorWrapper(
    val authenticator: TotpSetupData? = null,
)

@Serializable
data class TotpSetupData(
    val secret: String,
    val uri: String,
    val qrCode: String,
)

@Serializable
data class TotpVerifyRequest(
    val code: String,
)

@Serializable
data class TotpVerifyResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class RecoveryCodesResponse(
    val success: Boolean,
    val message: String? = null,
    val codes: List<String> = emptyList(),
)

@Serializable
data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)

@Serializable
data class PasswordChangeResponse(
    val success: Boolean,
    val message: String? = null,
)
