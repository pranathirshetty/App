package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import to.kuudere.anisuge.data.models.SearchResponse
import to.kuudere.anisuge.data.models.SessionInfo

class LatestService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    suspend fun getLatestUpdates(page: Int = 1): SearchResponse? {
        val stored = sessionStore.get()
        val response = httpClient.get("$BASE_URL/recently-updated") {
            parameter("type", "api")
            parameter("page", page)
            if (stored != null) header("Cookie", sessionToCookie(stored))
        }
        return response.body<SearchResponse>()
    }
}
