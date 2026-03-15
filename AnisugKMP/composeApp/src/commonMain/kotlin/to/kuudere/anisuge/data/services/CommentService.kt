package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import to.kuudere.anisuge.data.models.Comment
import to.kuudere.anisuge.data.models.CommentsResponse
import to.kuudere.anisuge.data.models.PostCommentResponse

class CommentService(
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://anime.anisurge.qzz.io"
    }

    private fun sessionToCookie(s: to.kuudere.anisuge.data.models.SessionInfo): String =
        "session_id=${s.sessionId}; session_secret=${s.session}; user_id=${s.userId}"

    /** Fetch top-level comments for an anime episode. */
    suspend fun getComments(
        animeId: String,
        episodeNumber: Int,
        page: Int = 1,
        sort: String = "new",   // "new" | "oldest" | "best"
        nid: String? = null,    // notification highlight id
    ): CommentsResponse? {
        return try {
            val stored = sessionStore.get()
            val url = buildString {
                append("$BASE_URL/api/anime/comments/$animeId/$episodeNumber?page=$page&sort=$sort")
                if (nid != null) append("&nid=$nid")
            }
            val response = httpClient.get(url) {
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            response.body()
        } catch (e: Exception) {
            println("[CommentService] getComments error: ${e.message}")
            null
        }
    }

    /** Fetch replies for a specific comment. */
    suspend fun getReplies(
        animeId: String,
        episodeNumber: Int,
        parentId: String,
        page: Int = 1,
    ): CommentsResponse? {
        return try {
            val stored = sessionStore.get()
            val url = "$BASE_URL/api/anime/comments/$animeId/$episodeNumber?parent_id=$parentId&page=$page"
            val response = httpClient.get(url) {
                if (stored != null) header("Cookie", sessionToCookie(stored))
            }
            response.body()
        } catch (e: Exception) {
            println("[CommentService] getReplies error: ${e.message}")
            null
        }
    }

    /** Post a root-level comment. */
    suspend fun postComment(
        animeId: String,
        episodeNumber: Int,
        content: String,
        isSpoiler: Boolean = false,
        parentCommentId: String? = null,
    ): PostCommentResponse? {
        return try {
            val stored = sessionStore.get()
            if (stored == null) {
                println("[CommentService] postComment: no session stored, aborting")
                return null
            }
            val body = buildJsonObject {
                put("anime", animeId)
                put("ep", episodeNumber)
                put("content", content)
                put("spoiller", isSpoiler)
                if (parentCommentId != null) put("parentCommentId", parentCommentId)
            }
            println("[CommentService] postComment animeId=$animeId ep=$episodeNumber body=$body")
            val response = httpClient.post("$BASE_URL/api/anime/comment") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            println("[CommentService] postComment status=${response.status}")
            if (response.status.value !in 200..299) {
                val raw = response.body<String>()
                println("[CommentService] postComment error body: $raw")
                return PostCommentResponse(success = false, message = "HTTP ${response.status.value}")
            }
            try {
                val parsed = response.body<PostCommentResponse>()
                println("[CommentService] postComment parsed: $parsed")
                parsed
            } catch (e: Exception) {
                println("[CommentService] postComment body parse error: ${e.message} — treating as success")
                PostCommentResponse(success = true)
            }
        } catch (e: Exception) {
            println("[CommentService] postComment error: ${e.message}")
            null
        }
    }

    /** Like or dislike a comment. */
    suspend fun voteComment(commentId: String, type: String /* "like" | "dislike" */): Boolean {
        return try {
            val stored = sessionStore.get() ?: return false
            val body = buildJsonObject { put("type", type) }
            val response = httpClient.post("$BASE_URL/api/anime/comment/respond/$commentId") {
                header("Cookie", sessionToCookie(stored))
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            println("[CommentService] voteComment error: ${e.message}")
            false
        }
    }

    /** Delete a comment (own comment or admin). */
    suspend fun deleteComment(commentId: String): Boolean {
        return try {
            val stored = sessionStore.get() ?: return false
            val response = httpClient.delete("$BASE_URL/api/anime/comment/$commentId") {
                header("Cookie", sessionToCookie(stored))
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            println("[CommentService] deleteComment error: ${e.message}")
            false
        }
    }
}
