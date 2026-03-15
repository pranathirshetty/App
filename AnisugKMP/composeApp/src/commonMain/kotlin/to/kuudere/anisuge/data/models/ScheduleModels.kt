package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Schedule API Response ────────────────────────────────────────────────────

@Serializable
data class ScheduleApiResponse(
    val success: Boolean = false,
    val data: Map<String, List<ScheduleAnime>> = emptyMap(),
    val timezone: String? = null,
    val total: Int? = null,
    val totalDates: Int? = null,
    val loadedDates: Int? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class ScheduleAnime(
    val id: String = "",
    val title: String = "",
    val episode: Int = 1,
    val time: String = "",
    val description: String = "",
    val cover: String = "",
    val banner: String? = null,
    val epCount: Int = 0,
    val status: String = "",
    val genres: List<String> = emptyList(),
    val year: Int = 0,
    val type: String = "TV",
) {
    val imageUrl: String get() = when {
        cover.startsWith("http") -> cover
        cover.isNotBlank() -> "https://anime.anisurge.qzz.io/img/poster/$cover"
        else -> ""
    }
    val bannerUrl: String? get() = when {
        banner == null -> null
        banner.startsWith("http") -> banner
        banner.isNotBlank() -> "https://anime.anisurge.qzz.io/img/banner/$banner"
        else -> null
    }
}
