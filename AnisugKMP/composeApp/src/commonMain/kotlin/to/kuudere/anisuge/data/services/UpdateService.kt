package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import to.kuudere.anisuge.data.models.UpdateResponse
import to.kuudere.anisuge.platform.PlatformName

class UpdateService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://anime.anisurge.qzz.io/api/v1/update"
    }

    suspend fun checkUpdate(): UpdateResponse? {
        val platformType = when (PlatformName.lowercase()) {
            "android" -> "android"
            "linux"   -> "linux"
            "windows" -> "windows"
            "macos"   -> "macos"
            else      -> "desktop"
        }

        return try {
            val response = httpClient.get(BASE_URL) {
                parameter("type", platformType)
            }
            response.body<UpdateResponse>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
