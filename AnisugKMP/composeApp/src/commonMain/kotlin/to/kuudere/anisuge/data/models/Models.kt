package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val userId: String,
    val session: String,
    val expire: String,
    val sessionId: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class VerifyResetCodeRequest(
    val email: String,
    val code: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
    val confirmPassword: String,
)

@Serializable
data class BasicApiResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val session: SessionData? = null,
)

@Serializable
data class SessionData(
    val userId: String,
    val sessionSecret: String? = null,
    val session: String? = null,
    val expire: String,
    val sessionId: String,
) {
    fun toSessionInfo() = SessionInfo(
        userId    = userId,
        session   = sessionSecret ?: session ?: "",
        expire    = expire,
        sessionId = sessionId,
    )
}

@Serializable
data class UserProfile(
    val id: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val email: String? = null,
    val avatar: String? = null,
) {
    val effectiveId: String? get() = id ?: userId
}

@Serializable
data class CurrentUserResponse(
    val success: Boolean? = null,
    val user: UserProfile? = null,
)

sealed interface SessionCheckResult {
    data object NoSession     : SessionCheckResult
    data object Expired       : SessionCheckResult
    data object NetworkError  : SessionCheckResult   // transient connectivity failure
    data class Valid(val session: SessionInfo, val user: UserProfile? = null) : SessionCheckResult
}
