package to.kuudere.anisuge.platform
import androidx.compose.runtime.Composable

expect val isDesktopPlatform: Boolean

/** Requests landscape on Android, ignored/different on desktop */
@Composable
expect fun LockScreenOrientation(landscape: Boolean)
