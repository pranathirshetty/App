package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable

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
    val window = LocalWindowScope.current.window
    androidx.compose.runtime.LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            window.extendedState = javax.swing.JFrame.MAXIMIZED_BOTH
        } else {
            window.extendedState = javax.swing.JFrame.NORMAL
        }
    }
}

