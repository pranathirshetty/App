package to.kuudere.anisuge.utils

import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

actual fun getDownloadsDirectory(): String {
    val home = System.getProperty("user.home")
    val dir = File(home, "Downloads/Anisug")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}

actual fun getCacheDirectory(): String {
    val home = System.getProperty("user.home")
    val dir = File(home, ".anisug")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}

actual fun hasStoragePermission(): Boolean = true

@androidx.compose.runtime.Composable
actual fun RequestStoragePermission(onResult: (Boolean) -> Unit) {
    onResult(true)
}

actual fun hasNotificationPermission(): Boolean = true

@androidx.compose.runtime.Composable
actual fun RequestNotificationPermission(onResult: (Boolean) -> Unit) {
    onResult(true)
}

actual fun openDirectory(path: String) {
    try {
        val file = File(path)
        val dir = if (file.isDirectory) file else file.parentFile
        if (dir != null && dir.exists()) {
            java.awt.Desktop.getDesktop().open(dir)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual suspend fun muxToMkv(
    videoPath: String,
    audioPath: String?,
    subtitles: List<Pair<String, String>>,
    fonts: List<String>,
    metadataPath: String?,
    outputPath: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val ffmpegPath = try {
            ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator().executablePath
        } catch (e: Exception) {
            "ffmpeg" // Fallback to system path if locator fails
        }

        val args = mutableListOf(ffmpegPath, "-y")
        args.add("-i"); args.add(videoPath)
        if (audioPath != null) {
            args.add("-i"); args.add(audioPath)
        }
        subtitles.forEach { (path, _) ->
            args.add("-i"); args.add(path)
        }
        
        val metadataIndex = if (metadataPath != null) {
            val index = 1 + (if (audioPath != null) 1 else 0) + subtitles.size
            args.add("-i"); args.add(metadataPath)
            index
        } else -1

        args.add("-map"); args.add("0:v")
        if (audioPath != null) {
            args.add("-map"); args.add("1:a")
        } else {
            args.add("-map"); args.add("0:a?")
        }

        subtitles.forEachIndexed { i, _ ->
            val index = if (audioPath != null) i + 2 else i + 1
            args.add("-map"); args.add("$index:s")
        }

        if (metadataIndex != -1) {
            args.add("-map_metadata"); args.add("$metadataIndex")
        }

        fonts.forEach { fontPath ->
            args.add("-attach"); args.add(fontPath)
        }
        args.add("-metadata:s:t"); args.add("mimetype=application/x-truetype-font")

        subtitles.forEachIndexed { i, (_, label) ->
            args.add("-metadata:s:s:$i"); args.add("title=$label")
        }

        args.add("-c"); args.add("copy")
        args.add(outputPath)

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.inputStream.bufferedReader().readText()
            println("[FFmpeg] Error ($exitCode): $error")
        }
        exitCode == 0
    } catch (e: Exception) {
        println("[FFmpeg] Process failed: ${e.message}")
        false
    }
}