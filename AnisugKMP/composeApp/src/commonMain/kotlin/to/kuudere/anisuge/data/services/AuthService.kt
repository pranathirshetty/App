package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import to.kuudere.anisuge.data.models.AuthResponse
import to.kuudere.anisuge.data.models.CurrentUserResponse
import to.kuudere.anisuge.data.models.LoginRequest
import to.kuudere.anisuge.data.models.RegisterRequest
import to.kuudere.anisuge.data.models.SessionCheckResult
import to.kuudere.anisuge.data.models.SessionInfo

class AuthService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://kuudere.to"
    }

    /** Build Cookie header from stored session */
    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    suspend fun login(email: String, password: String): SessionInfo {
        val response = httpClient.post("$BASE_URL/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        if (response.status != HttpStatusCode.OK)
            throw Exception("Login failed (${response.status})")
        val body: AuthResponse = response.body()
        if (!body.success || body.session == null)
            throw Exception(body.message ?: "Login failed")
        return body.session.toSessionInfo().also { sessionStore.save(it) }
    }

    suspend fun register(email: String, password: String, username: String): SessionInfo {
        val response = httpClient.post("$BASE_URL/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, username))
        }
        if (response.status != HttpStatusCode.OK)
            throw Exception("Registration failed (${response.status})")
        val body: AuthResponse = response.body()
        if (!body.success || body.session == null)
            throw Exception(body.message ?: "Registration failed")
        return body.session.toSessionInfo().also { sessionStore.save(it) }
    }

    /** Returns true if there is a valid, non-expired stored session. */
    suspend fun checkSession(): SessionCheckResult {
        val stored = sessionStore.get() ?: return SessionCheckResult.NoSession
        if (sessionStore.isExpired(stored)) return SessionCheckResult.Expired

        return try {
            val response = httpClient.get("$BASE_URL/api/user/current") {
                header("Cookie", sessionToCookie(stored))
            }
            val body: CurrentUserResponse = response.body()
            if (body.success != false) SessionCheckResult.Valid(stored, body.user)
            else SessionCheckResult.Expired
        } catch (e: Exception) {
            // Network error — treat as expired/unauthenticated
            SessionCheckResult.Expired
        }
    }

    suspend fun logout() {
        try {
            val stored = sessionStore.get()
            if (stored != null) {
                httpClient.post("$BASE_URL/api/auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "sessionId"     to stored.sessionId,
                        "sessionSecret" to stored.session,
                    ))
                }
            }
        } catch (e: Exception) {
            println("[AuthService] logout API call failed (proceeding with local clear): ${e.message}")
        } finally {
            sessionStore.clear()
        }
    }
}


