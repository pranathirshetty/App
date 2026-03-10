package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userId: String? = null,
    val defaultComments: Boolean = false,
    val defaultLang: Boolean = true,
    val autoNext: Boolean = true,
    val autoPlay: Boolean = false,
    val skipIntro: Boolean = false,
    val skipOutro: Boolean = false,
    val syncPercentage: Int = 80,
    val ads_in_home: Boolean = false,
    val ads_in_player: Boolean = false,
    val ads_in_search: Boolean = false,
    val ads_in_info_page: Boolean = false,
    val ads_in_watch_page: Boolean = false,
    val ad_types: String? = null,
)

@Serializable
data class PreferencesResponse(
    val success: Boolean,
    val message: String? = null,
    val preferences: UserPreferences? = null,
)

@Serializable
data class SessionInfoResponse(
    val id: String,
    val userId: String,
    val clientName: String? = null,
    val clientVersion: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val countryCode: String? = null,
    val countryName: String? = null,
    val ip: String? = null,
    val current: Boolean = false,
    val createdAt: String,
    val expiresAt: String? = null,
)

@Serializable
data class SessionsResponse(
    val success: Boolean,
    val message: String? = null,
    val sessions: List<SessionInfoResponse> = emptyList(),
    val currentSession: SessionInfoResponse? = null,
)

@Serializable
data class SessionDeleteResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class MfaStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MfaStatusData? = null,
)

@Serializable
data class MfaStatusData(
    val mfaEnabled: Boolean,
    val totpEnabled: Boolean,
)

@Serializable
data class MfaToggleRequest(
    val enabled: Boolean,
)

@Serializable
data class MfaToggleResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MfaStatusData? = null,
)

@Serializable
data class TotpSetupResponse(
    val success: Boolean,
    val message: String? = null,
    val data: TotpAuthenticatorWrapper? = null,
)

@Serializable
data class TotpAuthenticatorWrapper(
    val authenticator: TotpSetupData? = null,
)

@Serializable
data class TotpSetupData(
    val secret: String,
    val uri: String,
    val qrCode: String,
)

@Serializable
data class TotpVerifyRequest(
    val code: String,
)

@Serializable
data class TotpVerifyResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class RecoveryCodesResponse(
    val success: Boolean,
    val message: String? = null,
    val codes: List<String> = emptyList(),
)

@Serializable
data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)

@Serializable
data class PasswordChangeResponse(
    val success: Boolean,
    val message: String? = null,
)

// AniList Models
@Serializable
data class AniListStatusResponse(
    val success: Boolean,
    val connected: Boolean = false,
    val expired: Boolean = false,
    val message: String? = null,
    val connectedAt: String? = null,
    val lastUpdated: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class AniListProfileResponse(
    val success: Boolean,
    val connected: Boolean = false,
    val expired: Boolean = false,
    val message: String? = null,
    val profile: AniListProfile? = null,
)

@Serializable
data class AniListProfile(
    val id: Int,
    val name: String,
    val avatar: AniListAvatar? = null,
    val bannerImage: String? = null,
    val about: String? = null,
    val statistics: AniListStatistics? = null,
    val scoreFormat: String? = null,
    val connectedAt: String? = null,
    val lastUpdated: String? = null,
)

@Serializable
data class AniListAvatar(
    val medium: String? = null,
    val large: String? = null,
)

@Serializable
data class AniListStatistics(
    val anime: AniListAnimeStats? = null,
)

@Serializable
data class AniListAnimeStats(
    val count: Int = 0,
    val episodesWatched: Int = 0,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0,
)

@Serializable
data class AniListDisconnectResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class AniListTokenResponse(
    val success: Boolean,
    val access_token: String? = null,
    val message: String? = null,
    val connected: Boolean = false,
    val expired: Boolean = false,
)

@Serializable
data class ImportProgressData(
    val imported: Int = 0,
    val skipped: Int = 0,
    val errors: Int = 0,
    val total: Int = 0,
)

@Serializable
data class ImportResult(
    val success: Boolean = false,
    val stats: ImportProgressData? = null,
    val message: String? = null,
)

@Serializable
data class ImportApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val stats: ImportProgressData? = null,
)

/** SSE progress event from the import stream */
@Serializable
data class ImportStreamEvent(
    val type: String = "",               // "progress", "complete", "error", "item_error"
    val progress: Int = 0,
    val status: String? = null,
    val currentItem: String? = null,
    val error: String? = null,
    val stats: ImportProgressData? = null,
)

// AniList GraphQL Models
@Serializable
data class AniListMediaListCollection(
    val lists: List<AniListMediaListGroup>? = null
)

@Serializable
data class AniListMediaListGroup(
    val name: String? = null,
    val isCustomList: Boolean? = null,
    val entries: List<AniListMediaList>? = null
)

@Serializable
data class AniListMediaList(
    val id: Int? = null,
    val status: String? = null,
    val score: Double? = null,
    val progress: Int? = null,
    val repeat: Int? = null,
    val notes: String? = null,
    val startedAt: AniListFuzzyDate? = null,
    val completedAt: AniListFuzzyDate? = null,
    val media: AniListMedia? = null
)

@Serializable
data class AniListFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class AniListMedia(
    val id: Int? = null,
    val idMal: Int? = null,
    val title: AniListMediaTitle? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val type: String? = null
)

@Serializable
data class AniListMediaTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListGraphQLResponse(
    val data: AniListGraphQLData? = null,
    val errors: List<AniListGraphQLError>? = null
)

@Serializable
data class AniListGraphQLData(
    val MediaListCollection: AniListMediaListCollection? = null,
    val SaveMediaListEntry: AniListSavedEntry? = null
)

@Serializable
data class AniListGraphQLError(
    val message: String? = null
)

@Serializable
data class AniListSavedEntry(
    val id: Int? = null
)

// Watchlist export model
@Serializable
data class WatchlistExportEntry(
    val itemId: String? = null,
    val animeId: String? = null,
    val romaji: String? = null,
    val english: String? = null,
    val folder: String? = null,
    val score: Double? = null,
    val episodesWatched: Int? = null,
    val anilistId: String? = null,
    val malId: Int? = null
)

@Serializable
data class WatchlistExportResponse(
    val success: Boolean = false,
    val data: List<WatchlistExportEntry> = emptyList(),
    val message: String? = null
)
