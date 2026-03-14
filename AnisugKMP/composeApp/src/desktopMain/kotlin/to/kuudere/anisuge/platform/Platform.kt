package to.kuudere.anisuge.platform
import to.kuudere.anisuge.BuildConfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/** Desktop-only: provides the WindowState so composables can drive window placement. */
val LocalWindowState = staticCompositionLocalOf<WindowState> {
    error("LocalWindowState not provided — did you forget to provide it in main.kt?")
}

actual val isDesktopPlatform: Boolean = true
actual val PlatformName: String = System.getProperty("os.name").let { os ->
    val lower = os.lowercase()
    when {
        "windows" in lower -> "Windows"
        "linux" in lower   -> "Linux"
        "mac" in lower     -> "macOS"
        else               -> "Desktop"
    }
}

actual val AppVersion: String by lazy {
    // Priority 1: System property set by jpackage launcher (Windows/Linux/macOS)
    System.getProperty("jpackage.app-version") 
        // Priority 2: Linux AppImage metadata
        ?: System.getenv("APPIMAGE_VERSION")
        // Fallback: Internal build metadata (IDE/Dev)
        ?: BuildConfig.APP_VERSION
}

actual val AppBuildNumber: Int by lazy {
    // If we have a jpackage version, it might be in format 1.0.0+1
    val fullVersion = System.getProperty("jpackage.app-version") ?: ""
    if ("+" in fullVersion) {
        fullVersion.substringAfterLast("+").toIntOrNull() ?: BuildConfig.APP_BUILD_NUMBER
    } else {
        BuildConfig.APP_BUILD_NUMBER
    }
}

@Composable
actual fun LockScreenOrientation(landscape: Boolean) {
    // Desktop manages its own window fullscreen elsewhere
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop has no native back gesture that requires interception like Android
}

@Composable
actual fun SyncFullscreen(isFullscreen: Boolean) {
    val windowState = LocalWindowState.current
    androidx.compose.runtime.LaunchedEffect(isFullscreen) {
        windowState.placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }
}
