package to.kuudere.anisuge.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import to.kuudere.anisuge.data.models.FontData
import java.io.File
import java.net.URL

actual suspend fun downloadFontsAndGetDir(fonts: List<FontData>, onProgress: ((String) -> Unit)?): String? = withContext(Dispatchers.IO) {
    try {
        val userHome = System.getProperty("user.home") ?: "/tmp"
        val fontsDir = File("$userHome/.local/share/fonts/kuudere-subs")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }
        
        var count = 1
        val total = fonts.size
        
        for (font in fonts) {
            val urlStr = font.url ?: continue
            val name = font.name ?: continue
            
            var extension = ".ttf"
            try {
                val path = URL(urlStr).path
                if (path.contains(".")) {
                    extension = path.substring(path.lastIndexOf("."))
                }
            } catch (e: Exception) {}
            
            val finalName = if (name.endsWith(extension, ignoreCase = true)) name else name + extension
            val fontFile = File(fontsDir, finalName)
            
            if (!fontFile.exists()) {
                withContext(Dispatchers.Main) { onProgress?.invoke("Downloading font ($count/$total): $finalName") }
                try {
                    val conn = URL(urlStr).openConnection()
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val stream = conn.getInputStream()
                    val bytes = stream.readBytes()
                    fontFile.writeBytes(bytes)
                    stream.close()
                    println("Downloaded font to ${fontFile.absolutePath}")
                } catch (e: Exception) {
                    println("Failed to download font: $finalName -> ${e.message}")
                }
            }
            count++
        }
        
        // Update font cache (optional, sometimes mpv/fontconfig needs this)
        try {
            Runtime.getRuntime().exec(arrayOf("fc-cache", "-f", fontsDir.absolutePath)).waitFor()
        } catch (e: Exception) {
            // Ignore if fc-cache formatting fails
        }
        
        fontsDir.absolutePath
    } catch (e: Exception) {
        println("Font downloader error: ${e.message}")
        null
    }
}
