package to.kuudere.anisuge.data.services

import android.content.Context
import android.os.StatFs
import android.os.Environment
import java.io.File

actual fun getFontCacheDirectory(): String {
    // Android implementation would use app's cache dir
    return ""
}

actual fun getSettingsDirectory(): String {
    // Android uses DataStore, not files
    return ""
}

actual fun getTotalDiskSpace(): Long {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.totalBytes
    } catch (e: Exception) {
        0L
    }
}

actual fun getFreeDiskSpace(): Long {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBytes
    } catch (e: Exception) {
        0L
    }
}
