package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import to.kuudere.anisuge.data.models.AnimeItem
import to.kuudere.anisuge.data.models.ContinueWatchingItem
import to.kuudere.anisuge.data.models.HomeData
import to.kuudere.anisuge.data.models.HomeApiResponse
import to.kuudere.anisuge.data.models.SessionInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class HomeService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    private var cachedHomeData: HomeData? = null

    /** Fetches the home screen data. Returns null on network/parse failure. */
    suspend fun fetchHomeData(forceRefresh: Boolean = false): HomeData? {
        if (!forceRefresh && cachedHomeData != null) {
            println("[HomeService] Returning cached HomeData")
            return cachedHomeData
        }

        return try {
            val stored = sessionStore.get()
            val response = httpClient.get("$BASE_URL/") {
                parameter("type", "api")
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            val raw: kotlinx.serialization.json.JsonElement = response.body()
            val dataObj = raw.jsonObject["data"]?.jsonObject ?: raw.jsonObject

            fun parseList(key: String): List<AnimeItem> =
                dataObj[key]?.jsonArray?.let { arr ->
                    lenientJson.decodeFromJsonElement(arr)
                } ?: emptyList()

            val result = HomeData(
                topAired    = parseList("topAired"),
                latestEps   = parseList("latestEps"),
                lastUpdated  = parseList("lastUpdated"),
                topUpcoming  = parseList("topUpcoming"),
            )
            println("[HomeService] topAired=${result.topAired.size} latestEps=${result.latestEps.size} lastUpdated=${result.lastUpdated.size} topUpcoming=${result.topUpcoming.size}")
            cachedHomeData = result
            result
        } catch (e: Exception) {
            println("[HomeService] fetchHomeData error: ${e.message}")
            null
        }
    }

    /** Fetches continue watching list. Returns empty on error or if not logged in. */
    suspend fun fetchContinueWatching(): List<ContinueWatchingItem> {
        val stored = sessionStore.get() ?: return emptyList()
        return try {
            val response = httpClient.get("$BASE_URL/api/continue-watching/home") {
                header("Cookie", sessionToCookie(stored))
            }
            val raw: kotlinx.serialization.json.JsonElement = response.body()
            when (raw) {
                is JsonArray  -> lenientJson.decodeFromJsonElement(raw)
                is JsonObject -> {
                    val arr = raw.jsonObject["data"]?.jsonArray
                    if (arr != null) lenientJson.decodeFromJsonElement(arr) else emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            println("[HomeService] fetchContinueWatching error: ${e.message}")
            emptyList()
        }
    }
}
