package to.kuudere.anisuge.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import to.kuudere.anisuge.MainActivity

actual val isDesktopPlatform: Boolean = false
actual val PlatformName: String = "Android"

actual val AppVersion: String by lazy {
    val packageInfo = androidAppContext.packageManager.getPackageInfo(androidAppContext.packageName, 0)
    packageInfo.versionName!!
}

actual val AppBuildNumber: Int by lazy {
    val packageInfo = androidAppContext.packageManager.getPackageInfo(androidAppContext.packageName, 0)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
    }
}

@Composable
actual fun LockScreenOrientation(landscape: Boolean) {
    val context = LocalContext.current
    DisposableEffect(landscape) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val insetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)

        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = if (landscape) {
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        onDispose {
            // Force return to portrait when composable is destroyed (like pressing back)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
}

internal actual fun internalOpenUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    androidAppContext.startActivity(intent)
}

internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun SyncFullscreen(isFullscreen: Boolean) {
    // Android is mostly handled by LockScreenOrientation's insets controller
}
