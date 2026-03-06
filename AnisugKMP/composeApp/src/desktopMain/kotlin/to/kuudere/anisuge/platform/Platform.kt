package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable

actual val isDesktopPlatform: Boolean = true

@Composable
actual fun LockScreenOrientation(landscape: Boolean) {
    // Desktop manages its own window fullscreen elsewhere
}
