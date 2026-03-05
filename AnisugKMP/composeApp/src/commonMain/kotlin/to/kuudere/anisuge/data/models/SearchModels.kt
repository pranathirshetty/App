package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val success: Boolean,
    val animeData: List<AnimeItem>? = emptyList(),
    val total: Int? = 0,
    val totalPages: Int? = 0,
    val currentPage: Int? = 1,
    val hasMore: Boolean? = false,
)
