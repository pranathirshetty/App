package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import to.kuudere.anisuge.data.models.SessionInfo
import to.kuudere.anisuge.data.models.WatchlistResponse
import to.kuudere.anisuge.data.models.WatchlistUpdateResponse
import kotlinx.serialization.Serializable

class WatchlistService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    suspend fun getWatchlist(page: Int = 1, folder: String? = null, keyword: String? = null, sort: String? = null, genres: String? = null, type: String? = null, status: String? = null): WatchlistResponse? {
        val stored = sessionStore.get()
        val response = httpClient.get("$BASE_URL/api/anime/watchlist") {
            parameter("page", page.toString())
            parameter("perPage", "18")
            folder?.let { parameter("folder", it) }
            keyword?.takeIf { it.isNotBlank() }?.let { parameter("keyword", it) }
            sort?.let { parameter("sort", it) }
            genres?.let { parameter("genres", it) }
            type?.let { parameter("type", it) }
            status?.let { parameter("status", it) }
            if (stored != null) header("Cookie", sessionToCookie(stored))
        }
        return response.body<WatchlistResponse>()
    }

    @Serializable
    private data class UpdateStatusRequest(val animeId: String, val folder: String)

    suspend fun updateStatus(animeId: String, folder: String): WatchlistUpdateResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/anime/watchlist") {
                contentType(ContentType.Application.Json)
                header("Cookie", sessionToCookie(stored))
                setBody(UpdateStatusRequest(animeId, folder))
            }
            if (response.status.value in 200..299) {
                response.body<WatchlistUpdateResponse>()
            } else null
        } catch (e: Exception) {
            println("[WatchlistService] updateStatus error: ${e.message}")
            null
        }
    }
}
