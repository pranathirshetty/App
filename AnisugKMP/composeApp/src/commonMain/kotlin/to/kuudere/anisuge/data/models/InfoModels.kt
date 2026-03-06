package to.kuudere.anisuge.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnimeDetails(
    val id: String = "",
    val english: String = "Unknown",
    val romaji: String = "Unknown",
    val native: String = "Unknown",
    val ageRating: String = "Unknown",
    val malScore: Double? = null,
    val averageScore: Int = 0,
    val duration: Int = 0,
    val genres: List<String>? = emptyList(),
    val cover: String = "",
    val banner: String = "",
    val season: String = "Unknown",
    val startDate: String = "Unknown",
    val status: String = "Unknown",
    val synonyms: List<String>? = emptyList(),
    val studios: List<String>? = emptyList(),
    val type: String = "Unknown",
    val year: Int = 0,
    val epCount: Int = 0,
    val subbedCount: Int = 0,
    val dubbedCount: Int = 0,
    val description: String = "No description available.",
    @SerialName("in_watchlist") val inWatchlist: Boolean = false,
    val views: String = "0",
    val likes: String = "0",
    val folder: String? = null,
) {
    val title: String get() = if (english != "Unknown" && english.isNotBlank()) english else romaji
}

@Serializable
data class AnimeDetailsResponse(
    val data: AnimeDetails
)

@Serializable
data class WatchlistUpdateResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class EpisodeItem(
    val number: Int = 0,
    val titles: List<String?>? = emptyList(),
)

@Serializable
data class AnimeInfoMeta(
    val anilist: Int? = null
)

@Serializable
data class EpisodeDataResponse(
    @SerialName("all_episodes") val allEpisodes: List<EpisodeItem>? = emptyList(),
    @SerialName("anime_info") val animeInfo: AnimeInfoMeta? = null
)

@Serializable
data class ThumbnailsResponse(
    val success: Boolean = true,
    val thumbnails: Map<String, String>? = emptyMap()
)
