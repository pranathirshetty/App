package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json
import to.kuudere.anisuge.data.models.ScheduleAnime
import to.kuudere.anisuge.data.models.ScheduleApiResponse

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class ScheduleService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://kuudere.to"
    }

    private fun sessionToCookie(s: to.kuudere.anisuge.data.models.SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    /** Fetches schedule data grouped by date (YYYY-MM-DD → list of anime). */
    suspend fun fetchSchedule(timezone: String = "UTC"): Map<String, List<ScheduleAnime>> {
        return try {
            val stored = sessionStore.get()
            val response = httpClient.get("$BASE_URL/api/schedule") {
                parameter("timezone", timezone)
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            val parsed: ScheduleApiResponse = response.body()
            if (parsed.success) {
                println("[ScheduleService] Loaded ${parsed.total} anime across ${parsed.totalDates} days")
                parsed.data
            } else {
                println("[ScheduleService] API returned success=false")
                emptyMap()
            }
        } catch (e: Exception) {
            println("[ScheduleService] fetchSchedule error: ${e.message}")
            emptyMap()
        }
    }
}
