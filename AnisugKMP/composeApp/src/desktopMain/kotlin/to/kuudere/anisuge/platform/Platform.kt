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
        val frame = window as? javax.swing.JFrame
        if (frame != null) {
            if (isFullscreen) {
                frame.extendedState = javax.swing.JFrame.MAXIMIZED_BOTH
            } else {
                frame.extendedState = javax.swing.JFrame.NORMAL
            }
        }
    }
}

