package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import to.kuudere.anisuge.data.models.SearchResponse
import to.kuudere.anisuge.data.models.SessionInfo

class SearchService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    suspend fun search(
        keyword: String? = null,
        page: Int = 1,
        genres: List<String>? = null,
        season: String? = null,
        year: String? = null,
        type: String? = null,
        status: String? = null,
        language: String? = null,
        rating: String? = null,
        sort: String? = null
    ): SearchResponse? {
        val stored = sessionStore.get()
        val response = httpClient.get("$BASE_URL/search") {
            parameter("format", "api")
            parameter("page", page)
            keyword?.let { parameter("keyword", it) }
            genres?.let { if (it.isNotEmpty()) parameter("genres", it.joinToString(",")) }
            season?.let { parameter("season", it) }
            year?.let { parameter("year", it) }
            type?.let { parameter("type", it) }
            status?.let { 
                val s = if (it == "Not Yet Released") "NOT_YET_RELEASED" else it
                parameter("status", s) 
            }
            language?.let { parameter("language", it) }
            rating?.let { parameter("rating", it) }
            sort?.let { parameter("sort", it) }

            if (stored != null) header("Cookie", sessionToCookie(stored))
        }
        return response.body<SearchResponse>()
    }
}
