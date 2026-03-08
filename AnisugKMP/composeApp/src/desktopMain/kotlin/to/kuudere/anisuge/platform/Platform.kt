package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/** Desktop-only: provides the WindowState so composables can drive window placement. */
val LocalWindowState = staticCompositionLocalOf<WindowState> {
    error("LocalWindowState not provided — did you forget to provide it in main.kt?")
}

actual val isDesktopPlatform: Boolean = true

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
