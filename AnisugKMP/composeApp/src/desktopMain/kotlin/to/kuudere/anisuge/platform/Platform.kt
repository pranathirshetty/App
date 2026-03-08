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

