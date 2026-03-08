package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val author: String? = null,
    val authorId: String? = null,
    val authorPfp: String? = null,
    val authorVerified: Boolean = false,
    val authorLabels: List<String> = emptyList(),
    val content: String,
    val isSpoiller: Boolean = false,
    val created_at: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val isLiked: Boolean = false,
    val isUnliked: Boolean = false,
    val reply_count: Int = 0,
    val replies: List<Comment> = emptyList(),
    val highlight: Boolean = false,
    // UI-only state — not serialized from server but populated locally
    val showReplies: Boolean = false,
    val isReplying: Boolean = false,
    val replyText: String = "",
    val isSubmitting: Boolean = false,
    val isLoadingReplies: Boolean = false,
    val repliesPage: Int = 0,
    val hasMoreReplies: Boolean = false,
)

@Serializable
data class CommentsResponse(
    val comments: List<Comment> = emptyList(),
    val total_comments: Int = 0,
    val total_root_comments: Int? = null,
    val has_more: Boolean = false,
    val targetCommentId: String? = null,
)

@Serializable
data class PostCommentResponse(
    val success: Boolean = false,
    val data: PostCommentData? = null,
    val message: String? = null,
)

@Serializable
data class PostCommentData(
    val commentId: String? = null,
    val id: String? = null,
)
