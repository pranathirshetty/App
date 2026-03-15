package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import to.kuudere.anisuge.data.models.ScheduleApiResponse

class ScheduleService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: to.kuudere.anisuge.data.models.SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    /** Fetches a page of schedule data. limit = how many date-groups, offset = how many to skip. */
    suspend fun fetchSchedule(
        limit: Int = 3,
        offset: Int = 0,
        timezone: String = "UTC",
    ): ScheduleApiResponse {
        val stored = sessionStore.get()
        val response = httpClient.get("$BASE_URL/api/schedule") {
            parameter("timezone", timezone)
            parameter("limit", limit)
            if (offset > 0) parameter("offset", offset)
            if (stored != null) header("Cookie", sessionToCookie(stored))
        }
        val parsed: ScheduleApiResponse = response.body()
        println("[ScheduleService] fetched limit=$limit offset=$offset → ${parsed.data.size} date-groups, hasMore=${parsed.hasMore}")
        return parsed
    }
}
