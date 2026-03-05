package to.kuudere.anisuge.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import to.kuudere.anisuge.player.VideoPlayerSurface
import to.kuudere.anisuge.player.rememberVideoPlayerState

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val destination by viewModel.destination.collectAsState()
    var videoFinished by remember { mutableStateOf(false) }

    val playerState = rememberVideoPlayerState(
        url          = "composeResources/anisugkmp.composeapp.generated.resources/drawable/splash.mp4",
        loop         = false,
        muted        = true,
        showControls = false,
        enableSubs   = false,
    )

    // Timeout fallback
    LaunchedEffect(Unit) {
        delay(6000)
        videoFinished = true
    }

    // Navigate when both video finished AND destination resolved
    LaunchedEffect(destination, videoFinished) {
        if (videoFinished && destination != SplashDestination.Waiting) {
            when (destination) {
                is SplashDestination.GoHome -> onNavigateToHome()
                else -> onNavigateToAuth()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayerSurface(
            state      = playerState,
            modifier   = Modifier.fillMaxSize(),
            onFinished = { videoFinished = true },
        )
    }
}

@Composable
expect fun SplashVideoBackground(onVideoFinished: () -> Unit)
