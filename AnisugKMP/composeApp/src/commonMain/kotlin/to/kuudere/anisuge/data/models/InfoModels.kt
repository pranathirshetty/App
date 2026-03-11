package to.kuudere.anisuge.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnimeDetails(
    val id: String = "",
    val anilistId: Int? = null,
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null,
    val ageRating: String? = null,
    val malScore: Double? = null,
    val averageScore: Int? = 0,
    val duration: Int? = 0,
    val genres: List<String>? = emptyList(),
    val cover: String? = "",
    val banner: String? = "",
    val season: String? = null,
    val startDate: String? = null,
    val status: String? = null,
    val synonyms: List<String>? = emptyList(),
    val studios: List<String>? = emptyList(),
    val type: String? = null,
    val year: Int? = 0,
    val epCount: Int? = 0,
    val subbedCount: Int? = 0,
    val dubbedCount: Int? = 0,
    val description: String? = null,
    @SerialName("in_watchlist") val inWatchlist: Boolean = false,
    val views: String? = "0",
    val likes: String? = "0",
    val folder: String? = null,
    val continueWatching: ContinueWatching? = null
) {
    val title: String get() {
        val eng = english?.takeIf { it.isNotBlank() }
        val rom = romaji?.takeIf { it.isNotBlank() }
        return eng ?: rom ?: "Unknown"
    }
}

@Serializable
data class AnimeDetailsResponse(
    val data: AnimeDetails
)

@Serializable
data class ContinueWatching(
    val continueId: String? = null,
    val link: String? = null,
    val episode: Int? = null,
    val progress: String? = null,
    val duration: String? = null
)

@Serializable
data class EpisodeItem(
    val id: String = "",
    val number: Int = 0,
    val titles: List<String?>? = emptyList(),
    val ago: String? = null
)

@Serializable
data class AnimeInfoMeta(
    val id: String? = null,
    val anilist: Int? = null,
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null,
    val ageRating: String? = null,
    val malScore: Double? = null,
    val averageScore: Int? = 0,
    val duration: Int? = 0,
    val genres: List<String>? = emptyList(),
    val folder: String? = null,
    val cover: String? = null,
    val banner: String? = null,
    val season: String? = null,
    val startDate: String? = null,
    val status: String? = null,
    val synonyms: List<String>? = emptyList(),
    val studios: List<String>? = emptyList(),
    val type: String? = null,
    val year: Int? = 0,
    val epCount: Int? = 0,
    val subbedCount: Int? = 0,
    val dubbedCount: Int? = 0,
    val description: String? = null,
    val url: String? = null,
    @SerialName("inWatchlist") val inWatchlist: Boolean? = null,
    val userLiked: Boolean? = null,
    val userUnliked: Boolean? = null,
    val likes: Int? = 0,
    val dislikes: Int? = 0
)

@Serializable
data class EpisodeLink(
    val dataLink: String? = null,
    val dataType: String? = null,
    val serverName: String? = null
)

@Serializable
data class EpisodeDataResponse(
    @SerialName("episode_id") val episodeId: String? = null,
    val current: Int? = null,
    @SerialName("total_comments") val totalComments: Int? = 0,
    @SerialName("all_episodes") val allEpisodes: List<EpisodeItem>? = emptyList(),
    @SerialName("anime_info") val animeInfo: AnimeInfoMeta? = null,
    @SerialName("episode_links") val episodeLinks: List<EpisodeLink>? = emptyList(),
    val inWatchlist: Boolean = false,
    val folder: String? = null
)

@Serializable
data class ThumbnailsResponse(
    val success: Boolean = true,
    val thumbnails: Map<String, String>? = emptyMap()
)
