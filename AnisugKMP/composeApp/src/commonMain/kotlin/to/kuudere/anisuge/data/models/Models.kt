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
data class CurrentUserResponse(
    val success: Boolean? = null,
    val user: Map<String, String>? = null,
)
