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

internal expect fun internalOpenUrl(url: String)
