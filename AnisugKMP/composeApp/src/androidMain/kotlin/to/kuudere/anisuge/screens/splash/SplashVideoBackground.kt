package to.kuudere.anisuge.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// On Android, skip the video splash and go straight to app
@Composable
actual fun SplashVideoBackground(onVideoFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        onVideoFinished()
    }
}
