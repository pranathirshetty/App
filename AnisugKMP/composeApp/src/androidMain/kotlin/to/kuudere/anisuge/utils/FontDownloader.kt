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
        val cacheDir = System.getProperty("java.io.tmpdir") ?: return@withContext null
        val fontsDir = File("$cacheDir/sub-fonts")
        if (!fontsDir.exists()) fontsDir.mkdirs()
        
        // Download fonts in parallel for better performance
        fonts.map { font ->
            async {
                val urlStr = font.url ?: return@async
                val name = font.name ?: return@async
                
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
                    try {
                        val conn = URL(urlStr).openConnection()
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.getInputStream().use { input ->
                            fontFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.awaitAll()
        
        fontsDir.absolutePath
    } catch (e: Exception) {
        null
    }
}
