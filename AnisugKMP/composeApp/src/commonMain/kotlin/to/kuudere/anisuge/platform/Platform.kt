package to.kuudere.anisuge.platform
import androidx.compose.runtime.Composable

expect val isDesktopPlatform: Boolean

/** Requests landscape on Android, ignored/different on desktop */
@Composable
expect fun LockScreenOrientation(landscape: Boolean)

/** Intercepts hardware back button on Android, no-op on desktop */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** Syncs app fullscreen state with window-level fullscreen on desktop */
@Composable
expect fun SyncFullscreen(isFullscreen: Boolean)
