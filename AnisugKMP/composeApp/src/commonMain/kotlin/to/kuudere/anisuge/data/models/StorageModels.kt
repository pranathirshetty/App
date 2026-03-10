package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable

@Serializable
data class StorageInfo(
    val downloads: StorageCategory = StorageCategory(),
    val fontCache: StorageCategory = StorageCategory(),
    val settings: StorageCategory = StorageCategory(),
    val totalUsed: Long = 0,
    val freeSpace: Long = 0,
)

@Serializable
data class StorageCategory(
    val name: String = "",
    val size: Long = 0,
    val fileCount: Int = 0,
    val path: String = "",
)

@Serializable
data class DownloadStorageInfo(
    val animeFolders: List<AnimeFolderInfo> = emptyList(),
    val totalEpisodes: Int = 0,
    val totalSize: Long = 0,
)

@Serializable
data class AnimeFolderInfo(
    val animeId: String,
    val title: String,
    val episodeCount: Int,
    val size: Long,
    val coverImage: String? = null,
)
