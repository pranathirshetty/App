package to.kuudere.anisuge.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistResponse(
    val data: List<AnimeItem> = emptyList(),
    @SerialName("watchlist") val watchlistList: List<AnimeItem>? = null,
    @SerialName("current_page") val currentPageAlt: Int? = null,
    val currentPage: Int = 1,
    @SerialName("total_pages") val totalPagesAlt: Int? = null,
    val totalPages: Int = 1
) {
    val items: List<AnimeItem> get() = data.ifEmpty { watchlistList ?: emptyList() }
    val page: Int get() = currentPageAlt ?: currentPage
    val total: Int get() = totalPagesAlt ?: totalPages
}
