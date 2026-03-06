package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import to.kuudere.anisuge.data.models.AnimeDetailsResponse
import to.kuudere.anisuge.data.models.EpisodeDataResponse
import to.kuudere.anisuge.data.models.SessionInfo
import to.kuudere.anisuge.data.models.ThumbnailsResponse
import io.ktor.client.statement.HttpResponse

class InfoService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://kuudere.to"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    suspend fun getAnimeDetails(id: String): AnimeDetailsResponse? {
        return try {
            val stored = sessionStore.get()
            val response = httpClient.get("$BASE_URL/anime/$id") {
                parameter("type", "api")
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            response.body<AnimeDetailsResponse>()
        } catch (e: Exception) {
            println("[InfoService] fetchAnimeDetails error: ${e.message}")
            null
        }
    }

    suspend fun getEpisodes(animeId: String, episodeNumber: Int = 1): EpisodeDataResponse? {
        return try {
            val stored = sessionStore.get()
            val response = httpClient.get("$BASE_URL/api/watch/$animeId/$episodeNumber") {
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            response.body<EpisodeDataResponse>()
        } catch (e: Exception) {
            println("[InfoService] getEpisodes error: ${e.message}")
            null
        }
    }

    suspend fun getThumbnails(anilistId: Int): ThumbnailsResponse? {
        return try {
            val response = httpClient.get("$BASE_URL/api/thumbnails/$anilistId")
            response.body<ThumbnailsResponse>()
        } catch (e: Exception) {
            println("[InfoService] getThumbnails error: ${e.message}")
            null
        }
    }

    suspend fun updateWatchlistStatus(animeId: String, folder: String): Boolean {
        return try {
            val stored = sessionStore.get() ?: return false
            val payload = buildJsonObject {
                put("animeId", animeId)
                put("folder", folder)
            }
            val response = httpClient.post("$BASE_URL/api/anime/watchlist") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            println("[InfoService] updateWatchlistStatus error: ${e.message}")
            false
        }
    }

    suspend fun getVideoStream(anilistId: Int, episodeNumber: Int, server: String): to.kuudere.anisuge.data.models.WatchServerResponse? {
        return try {
            val stored = sessionStore.get()
            val response = httpClient.get("$BASE_URL/$anilistId/$episodeNumber/$server") {
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            response.body<to.kuudere.anisuge.data.models.WatchServerResponse>()
        } catch (e: Exception) {
            println("[InfoService] getVideoStream error: ${e.message}")
            null
        }
    }
}
