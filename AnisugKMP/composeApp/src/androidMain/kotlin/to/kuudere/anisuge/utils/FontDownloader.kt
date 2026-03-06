package to.kuudere.anisuge.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import to.kuudere.anisuge.data.models.FontData
import java.io.File
import java.net.URL

actual suspend fun downloadFontsAndGetDir(fonts: List<FontData>): String? = withContext(Dispatchers.IO) {
    try {
        val cacheDir = System.getProperty("java.io.tmpdir") ?: return@withContext null
        val fontsDir = File("$cacheDir/sub-fonts")
        if (!fontsDir.exists()) fontsDir.mkdirs()
        
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
                try {
                    val conn = URL(urlStr).openConnection()
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val stream = conn.getInputStream()
                    val bytes = stream.readBytes()
                    fontFile.writeBytes(bytes)
                    stream.close()
                } catch (e: Exception) {}
            }
        }
        fontsDir.absolutePath
    } catch (e: Exception) {
        null
    }
}
