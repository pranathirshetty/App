package to.kuudere.anisuge.utils

import java.io.File

actual fun getDownloadsDirectory(): String {
    val home = System.getProperty("user.home")
    val dir = File(home, "Downloads/Anisug")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
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
