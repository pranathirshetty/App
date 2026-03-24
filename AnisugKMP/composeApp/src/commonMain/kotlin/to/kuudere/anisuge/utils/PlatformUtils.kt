package to.kuudere.anisuge.utils

expect fun getDownloadsDirectory(): String
expect fun getCacheDirectory(): String

expect fun openDirectory(path: String)

expect fun hasStoragePermission(): Boolean

@androidx.compose.runtime.Composable
expect fun RequestStoragePermission(onResult: (Boolean) -> Unit)

expect fun hasNotificationPermission(): Boolean

@androidx.compose.runtime.Composable
expect fun RequestNotificationPermission(onResult: (Boolean) -> Unit)

expect suspend fun muxToMkv(
    videoPath: String,
    audioPath: String?,
    subtitles: List<Pair<String, String>>, // Path to Label
    fonts: List<String>,
    metadataPath: String?,
    outputPath: String,
    inputHeaders: Map<String, String>? = null
): Boolean
