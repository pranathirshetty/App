package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AniListService(
    private val httpClient: HttpClient
) {
    suspend fun updateStatus(
        accessToken: String,
        anilistId: Int,
        folder: String,
        progress: Int = 0,
        score: Double? = null
    ): Boolean {
        val anilistStatus = convertFolderToStatus(folder) ?: return false

        val query = """
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float, ${'$'}progress: Int) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score, progress: ${'$'}progress) {
                    id
                    status
                    progress
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", anilistId)
            put("status", anilistStatus)
            put("progress", progress)
            score?.let { put("score", it) }
        }

        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }

        return try {
            println("[AniListService] Updating AniList for ID $anilistId to $anilistStatus")
            val response = httpClient.post("https://graphql.anilist.co") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                header("Accept", "application/json")
                setBody(payload.toString())
            }
            
            val success = response.status.value in 200..299
            if (!success) {
                println("[AniListService] Error response: ${response.status} - ${response.bodyAsText()}")
            } else {
                val body = response.bodyAsText()
                if (body.contains("\"errors\"")) {
                    println("[AniListService] GraphQL errors: $body")
                    return false
                }
                println("[AniListService] Successfully updated AniList for $anilistId")
            }
            success
        } catch (e: Exception) {
            println("[AniListService] updateStatus exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun convertFolderToStatus(folder: String): String? = when (folder) {
        "Watching" -> "CURRENT"
        "Completed" -> "COMPLETED"
        "On Hold" -> "PAUSED"
        "Dropped" -> "DROPPED"
        "Plan To Watch" -> "PLANNING"
        else -> null
    }
}
