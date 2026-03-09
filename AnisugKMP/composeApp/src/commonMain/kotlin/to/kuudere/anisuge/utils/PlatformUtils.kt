package to.kuudere.anisuge.utils

expect fun getDownloadsDirectory(): String

expect fun openDirectory(path: String)

expect suspend fun muxToMkv(
    videoPath: String,
    audioPath: String?,
    subtitles: List<Pair<String, String>>, // Path to Label
    fonts: List<String>,
    outputPath: String
): Boolean
