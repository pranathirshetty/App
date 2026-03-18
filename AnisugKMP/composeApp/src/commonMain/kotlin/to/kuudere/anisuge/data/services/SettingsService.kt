package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import to.kuudere.anisuge.data.models.AniListDisconnectResponse
import to.kuudere.anisuge.data.models.AniListGraphQLResponse
import to.kuudere.anisuge.data.models.AniListMediaListCollection
import to.kuudere.anisuge.data.models.AniListProfileResponse
import to.kuudere.anisuge.data.models.AniListStatusResponse
import to.kuudere.anisuge.data.models.AniListTokenResponse
import to.kuudere.anisuge.data.models.ImportApiResponse
import to.kuudere.anisuge.data.models.ImportResult
import to.kuudere.anisuge.data.models.ImportStreamEvent
import to.kuudere.anisuge.data.models.WatchlistExportEntry
import to.kuudere.anisuge.data.models.WatchlistExportResponse
import to.kuudere.anisuge.data.models.MfaStatusResponse
import to.kuudere.anisuge.data.models.MfaToggleRequest
import to.kuudere.anisuge.data.models.MfaToggleResponse
import to.kuudere.anisuge.data.models.PasswordChangeRequest
import to.kuudere.anisuge.data.models.PasswordChangeResponse
import to.kuudere.anisuge.data.models.PreferencesResponse
import to.kuudere.anisuge.data.models.RecoveryCodesResponse
import to.kuudere.anisuge.data.models.SessionDeleteResponse
import to.kuudere.anisuge.data.models.SessionInfo
import to.kuudere.anisuge.data.models.SessionsResponse
import to.kuudere.anisuge.data.models.TotpSetupResponse
import to.kuudere.anisuge.data.models.TotpVerifyRequest
import to.kuudere.anisuge.data.models.TotpVerifyResponse
import to.kuudere.anisuge.data.models.UserPreferences

class SettingsService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    /** Fetch user preferences from the server */
    suspend fun getPreferences(): PreferencesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/preferences") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<PreferencesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getPreferences error: ${e.message}")
            null
        }
    }

    /** Fetch user profile from the server */
    suspend fun getUserProfile(): to.kuudere.anisuge.data.models.CurrentUserResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/user") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<to.kuudere.anisuge.data.models.CurrentUserResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getUserProfile error: ${e.message}")
            null
        }
    }

    /** Update user preferences on the server */
    suspend fun updatePreferences(preferences: UserPreferences): PreferencesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.put("$BASE_URL/api/settings/preferences") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(preferences)
            }
            response.body<PreferencesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] updatePreferences error: ${e.message}")
            null
        }
    }

    /** Fetch active sessions */
    suspend fun getSessions(): SessionsResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/sessions") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionsResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getSessions error: ${e.message}")
            null
        }
    }

    /** Delete a specific session */
    suspend fun deleteSession(sessionId: String): SessionDeleteResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/settings/sessions?action=single&sessionId=$sessionId") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionDeleteResponse>()
        } catch (e: Exception) {
            println("[SettingsService] deleteSession error: ${e.message}")
            null
        }
    }

    /** Delete all sessions (logout everywhere) */
    suspend fun deleteAllSessions(): SessionDeleteResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/settings/sessions?action=all") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<SessionDeleteResponse>()
        } catch (e: Exception) {
            println("[SettingsService] deleteAllSessions error: ${e.message}")
            null
        }
    }

    /** Get MFA status */
    suspend fun getMfaStatus(): MfaStatusResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/mfa") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<MfaStatusResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getMfaStatus error: ${e.message}")
            null
        }
    }

    /** Enable/disable MFA */
    suspend fun toggleMfa(enabled: Boolean): MfaToggleResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(MfaToggleRequest(enabled))
            }
            response.body<MfaToggleResponse>()
        } catch (e: Exception) {
            println("[SettingsService] toggleMfa error: ${e.message}")
            null
        }
    }

    /** Setup TOTP (returns secret and QR code URL) */
    suspend fun setupTotp(): TotpSetupResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa/totp") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<TotpSetupResponse>()
        } catch (e: Exception) {
            println("[SettingsService] setupTotp error: ${e.message}")
            null
        }
    }

    /** Verify TOTP code during setup */
    suspend fun verifyTotp(code: String): TotpVerifyResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/mfa/totp/verify") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(TotpVerifyRequest(code))
            }
            response.body<TotpVerifyResponse>()
        } catch (e: Exception) {
            println("[SettingsService] verifyTotp error: ${e.message}")
            null
        }
    }

    /** Get recovery codes */
    suspend fun getRecoveryCodes(): RecoveryCodesResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/settings/mfa/recovery") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<RecoveryCodesResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getRecoveryCodes error: ${e.message}")
            null
        }
    }

    /** Change password */
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String): PasswordChangeResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.post("$BASE_URL/api/settings/password") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(PasswordChangeRequest(currentPassword, newPassword, confirmPassword))
            }
            response.body<PasswordChangeResponse>()
        } catch (e: Exception) {
            println("[SettingsService] changePassword error: ${e.message}")
            null
        }
    }

    /** Get AniList connection status */
    suspend fun getAniListStatus(): AniListStatusResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/anilist/status") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListStatusResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getAniListStatus error: ${e.message}")
            null
        }
    }

    /** Get AniList profile */
    suspend fun getAniListProfile(): AniListProfileResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/anilist/profile") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListProfileResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getAniListProfile error: ${e.message}")
            null
        }
    }

    /** Disconnect AniList */
    suspend fun disconnectAniList(): AniListDisconnectResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.delete("$BASE_URL/api/auth/anilist/disconnect") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListDisconnectResponse>()
        } catch (e: Exception) {
            println("[SettingsService] disconnectAniList error: ${e.message}")
            null
        }
    }

    /** Get AniList OAuth URL */
    fun getAniListAuthUrl(token: String? = null): String {
        return if (!token.isNullOrBlank()) {
            "$BASE_URL/api/auth/anilist/login?token=$token"
        } else {
            "$BASE_URL/api/auth/anilist/login"
        }
    }

    /** Get AniList access token for import/export */
    suspend fun getAniListToken(): AniListTokenResponse? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/auth/anilist/token") {
                header("Cookie", sessionToCookie(stored))
            }
            response.body<AniListTokenResponse>()
        } catch (e: Exception) {
            println("[SettingsService] getAniListToken error: ${e.message}")
            null
        }
    }

    /** Fetch AniList watchlist via GraphQL */
    suspend fun fetchAniListWatchlist(accessToken: String, userId: Int): AniListMediaListCollection? {
        return try {
            val query = """
                query (${'$'}userId: Int, ${'$'}chunk: Int, ${'$'}perChunk: Int) {
                    MediaListCollection(userId: ${'$'}userId, type: ANIME, perChunk: ${'$'}perChunk, chunk: ${'$'}chunk, forceSingleCompletedList: true) {
                        lists {
                            name
                            isCustomList
                            entries {
                                id
                                status
                                score
                                progress
                                repeat
                                notes
                                startedAt { year month day }
                                completedAt { year month day }
                                media {
                                    id
                                    idMal
                                    title { romaji english native }
                                    episodes
                                    status
                                    type
                                }
                            }
                        }
                    }
                }
            """.trimIndent()

            val body = buildJsonObject {
                put("query", query)
                put("variables", buildJsonObject {
                    put("userId", userId)
                    put("chunk", 1)
                    put("perChunk", 500)
                })
            }

            val response = httpClient.post("https://graphql.anilist.co") {
                header("Authorization", "Bearer $accessToken")
                header("Content-Type", "application/json")
                header("Accept", "application/json")
                setBody(body)
            }

            val result = response.body<AniListGraphQLResponse>()
            if (result.errors != null && result.errors.isNotEmpty()) {
                println("[SettingsService] AniList GraphQL errors: ${result.errors}")
                return null
            }
            result.data?.MediaListCollection
        } catch (e: Exception) {
            println("[SettingsService] fetchAniListWatchlist error: ${e.message}")
            null
        }
    }

    /** Import watchlist to Kuudere */
    suspend fun importWatchlistToKuudere(
        data: Map<String, List<Map<String, Any?>>>,
        onProgress: (suspend (ImportStreamEvent) -> Unit)? = null
    ): ImportResult? {
        return try {
            val stored = sessionStore.get() ?: return null

            // Build JSON string manually for the HiAnime format
            val jsonBuilder = StringBuilder()
            jsonBuilder.append("{")
            data.entries.forEachIndexed { index, (folder, items) ->
                if (index > 0) jsonBuilder.append(",")
                jsonBuilder.append("\"$folder\":[")
                items.forEachIndexed { itemIndex, item ->
                    if (itemIndex > 0) jsonBuilder.append(",")
                    jsonBuilder.append("{")
                    item.entries.forEachIndexed { fieldIndex, (key, value) ->
                        if (fieldIndex > 0) jsonBuilder.append(",")
                        jsonBuilder.append("\"$key\":")
                        when (value) {
                            is String -> jsonBuilder.append("\"$value\"")
                            is Int -> jsonBuilder.append(value)
                            else -> jsonBuilder.append("null")
                        }
                    }
                    jsonBuilder.append("}")
                }
                jsonBuilder.append("]")
            }
            jsonBuilder.append("}")
            val jsonData = jsonBuilder.toString()

            // Build raw multipart body exactly like browser does
            val boundary = "----KtorFormBoundary" + kotlin.random.Random.nextLong().toString(16)
            val crlf = "\r\n"
            val multipartBody = StringBuilder()
            multipartBody.append("--$boundary$crlf")
            multipartBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"anilist-import.json\"$crlf")
            multipartBody.append("Content-Type: application/json$crlf")
            multipartBody.append(crlf)
            multipartBody.append(jsonData)
            multipartBody.append(crlf)
            multipartBody.append("--$boundary--$crlf")

            // Use streaming endpoint for progress updates
            val streamUrl = "$BASE_URL/api/import/json?stream=true"

            httpClient.preparePost(streamUrl) {
                header("Cookie", sessionToCookie(stored))
                header(HttpHeaders.Origin, BASE_URL)
                header(HttpHeaders.Referrer, "$BASE_URL/user/settings/sync")
                header("X-Requested-With", "XMLHttpRequest")
                header(HttpHeaders.Accept, "text/event-stream")
                header(HttpHeaders.ContentType, "multipart/form-data; boundary=$boundary")
                setBody(multipartBody.toString().encodeToByteArray())
            }.execute { response ->
                println("[SettingsService] Import SSE response status: ${response.status}")
                val contentType = response.headers[HttpHeaders.ContentType] ?: ""

                if (response.status.value in 200..299 && contentType.contains("text/event-stream")) {
                    // Read SSE stream line by line
                    val channel = response.bodyAsChannel()
                    var lastEvent: ImportStreamEvent? = null

                    while (!channel.isClosedForRead) {
                        val line = try {
                            channel.readUTF8Line()
                        } catch (_: Exception) {
                            null
                        } ?: break

                        if (line.startsWith("data: ")) {
                            val eventJson = line.removePrefix("data: ").trim()
                            try {
                                val event = json.decodeFromString<ImportStreamEvent>(eventJson)
                                lastEvent = event
                                onProgress?.invoke(event)
                            } catch (e: Exception) {
                                println("[SettingsService] Failed to parse SSE event: $eventJson — ${e.message}")
                            }
                        }
                    }

                    // Return based on last event
                    when (lastEvent?.type) {
                        "complete" -> ImportResult(
                            success = true,
                            message = lastEvent.status,
                            stats = lastEvent.stats
                        )
                        "error" -> ImportResult(
                            success = false,
                            message = lastEvent.error ?: "Import error"
                        )
                        else -> ImportResult(
                            success = lastEvent != null,
                            message = lastEvent?.status ?: "Stream ended unexpectedly",
                            stats = lastEvent?.stats
                        )
                    }
                } else {
                    // Non-streaming fallback (regular JSON response)
                    val responseBody = response.bodyAsText()
                    println("[SettingsService] Import fallback response: $responseBody")
                    val result = json.decodeFromString<ImportApiResponse>(responseBody)
                    ImportResult(
                        success = result.success,
                        message = result.message,
                        stats = result.stats
                    )
                }
            }
        } catch (e: Exception) {
            println("[SettingsService] importWatchlistToKuudere error: ${e.message}")
            null
        }
    }

    /** Get Kuudere watchlist for export */
    suspend fun getKuudereWatchlistForExport(): List<WatchlistExportEntry>? {
        return try {
            val stored = sessionStore.get() ?: return null
            val response = httpClient.get("$BASE_URL/api/anime/watchlist?perPage=9999") {
                header("Cookie", sessionToCookie(stored))
            }
            val result = response.body<WatchlistExportResponse>()
            if (result.success) result.data else null
        } catch (e: Exception) {
            println("[SettingsService] getKuudereWatchlistForExport error: ${e.message}")
            null
        }
    }

    /** Update AniList entry via GraphQL mutation. Returns null on success, error message on failure. */
    suspend fun updateAniListEntry(
        accessToken: String,
        mediaId: String,
        status: String,
        score: Double?,
        progress: Int
    ): String? {
        return try {
            val mutation = if (score != null) {
                """
                    mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float, ${'$'}progress: Int) {
                        SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score, progress: ${'$'}progress) {
                            id
                        }
                    }
                """.trimIndent()
            } else {
                """
                    mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int) {
                        SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress) {
                            id
                        }
                    }
                """.trimIndent()
            }

            val body = buildJsonObject {
                put("query", mutation)
                put("variables", buildJsonObject {
                    put("mediaId", mediaId.toInt())
                    put("status", status)
                    if (score != null) put("score", score)
                    put("progress", progress)
                })
            }

            val response = httpClient.post("https://graphql.anilist.co") {
                header("Authorization", "Bearer $accessToken")
                header("Content-Type", "application/json")
                header("Accept", "application/json")
                setBody(body)
            }

            val responseText = response.bodyAsText()
            val result = runCatching { json.decodeFromString<AniListGraphQLResponse>(responseText) }.getOrNull()

            if (response.status.value !in 200..299) {
                return "HTTP ${response.status.value}: ${responseText.take(500)}"
            }

            if (result?.errors != null && result.errors.isNotEmpty()) {
                val graphQlErrors = result.errors.joinToString("; ") { it.message ?: "unknown error" }
                "$graphQlErrors | raw=${responseText.take(500)}"
            } else if (result == null) {
                "Could not parse AniList response | raw=${responseText.take(500)}"
            } else {
                null // success
            }
        } catch (e: Exception) {
            println("[SettingsService] updateAniListEntry error: ${e.message}")
            e.message ?: "exception"
        }
    }
}
