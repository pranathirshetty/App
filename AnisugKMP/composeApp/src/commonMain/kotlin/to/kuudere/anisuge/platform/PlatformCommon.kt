package to.kuudere.anisuge.platform
import androidx.compose.runtime.Composable

expect val isDesktopPlatform: Boolean
expect val PlatformName: String
expect val AppVersion: String
expect val AppBuildNumber: Int

/** Requests landscape on Android, ignored/different on desktop */
@Composable
expect fun LockScreenOrientation(landscape: Boolean)

/** Intercepts hardware back button on Android, no-op on desktop */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** Syncs app fullscreen state with window-level fullscreen on desktop */
@Composable
expect fun SyncFullscreen(isFullscreen: Boolean)

/** Opens a URL in the platform's default browser */
fun openUrl(url: String) {
    internalOpenUrl(url)
}

/** Opens a platform-specific folder picker */
expect fun pickFolder(onPathSelected: (String) -> Unit)

/** Returns true if the app has permission to write and create files in the given directory. */
expect fun isFolderWritable(path: String): Boolean

/** Updates a system notification with the current download progress (Android only, no-op elsewhere) */
expect fun updateDownloadNotification(
    activeTasksCount: Int,
    totalProgress: Float,
    isInitial: Boolean = false
)

/** Clears the download notification when all tasks are finished or cancelled */
expect fun clearDownloadNotification()

/** Converts a raw technical path into a human-friendly display string. */
fun formatDisplayPath(path: String): String {
    if (path.isBlank()) return "Default Downloads"
    
    var display = path
    
    // Android common paths
    if (display.startsWith("/storage/emulated/0")) {
        display = display.replace("/storage/emulated/0/Download", "Downloads")
        display = display.replace("/storage/emulated/0", "Internal Storage")
    }
    
    // Linux/macOS common paths
    val home = System.getProperty("user.home") ?: ""
    if (home.isNotBlank() && display.startsWith(home)) {
        display = display.replace(home + "/Downloads", "Downloads")
        display = display.replace(home, "Home")
    }

    return display.trim('/').replace("/", " > ")
}

internal expect fun internalOpenUrl(url: String)
