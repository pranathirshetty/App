package to.kuudere.anisuge.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Anime ────────────────────────────────────────────────────────────────────

@Serializable
data class AnimeItem(
    val id:           String = "",
    val english:      String? = null,
    val romaji:       String? = null,
    val cover:        String? = null,
    @SerialName("carouselBanner")
    val carouselBanner: String? = null,
    val banner:       String? = null,
    val description:  String? = null,
    val malScore:     Double? = null,
    val genres:       List<String>? = emptyList(),
    val type:         String? = null,
    val epCount:      Int? = 0,
    @SerialName("subbed") val subbed: Int? = null,
    @SerialName("dubbed") val dubbed: Int? = null,
    val subbedCount:  Int? = null,
    val dubbedCount:  Int? = null,
    val duration:     Int? = 24,
    val folder:       String? = null,
    val status:       String? = null,
    val mainId:       String? = null,
    val anilistId:    Int? = null,
) {
    val title: String get() = english?.ifBlank { romaji } ?: romaji ?: ""
    val imageUrl: String get() = cover ?: ""
    val bannerUrl: String? get() = carouselBanner ?: banner
    val activeId: String get() = mainId?.ifBlank { id } ?: id
}

// ── Continue Watching ────────────────────────────────────────────────────────

@Serializable
data class ContinueWatchingItem(
    val duration:  String = "",
    val episode:   Int    = 0,
    val link:      String = "",
    val progress:  String = "",
    val thumbnail: String = "",
    val title:     String = "",
)

// ── Home API response ────────────────────────────────────────────────────────

@Serializable
data class HomeApiResponse(
    val success: Boolean = true,
    val data:    HomeData? = null,
)

@Serializable
data class HomeData(
    @SerialName("topAired")    val topAired:    List<AnimeItem> = emptyList(),
    @SerialName("latestEps")   val latestEps:   List<AnimeItem> = emptyList(),
    @SerialName("lastUpdated") val lastUpdated:  List<AnimeItem> = emptyList(),
    @SerialName("topUpcoming") val topUpcoming:  List<AnimeItem> = emptyList(),
)
