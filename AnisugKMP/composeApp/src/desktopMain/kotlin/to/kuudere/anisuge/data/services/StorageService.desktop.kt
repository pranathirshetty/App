package to.kuudere.anisuge.data.services

import java.io.File

actual fun getFontCacheDirectory(): String {
    val home = System.getProperty("user.home")
    return "$home/.local/share/fonts/kuudere-subs"
}

actual fun getSettingsDirectory(): String {
    val home = System.getProperty("user.home")
    val dir = File(home, ".config/anisuge")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}

actual fun getTotalDiskSpace(): Long {
    return try {
        val home = File(System.getProperty("user.home"))
        home.totalSpace
    } catch (e: Exception) {
        0L
    }
}

actual fun getFreeDiskSpace(): Long {
    return try {
        val home = File(System.getProperty("user.home"))
        home.freeSpace
    } catch (e: Exception) {
        0L
    }
}
