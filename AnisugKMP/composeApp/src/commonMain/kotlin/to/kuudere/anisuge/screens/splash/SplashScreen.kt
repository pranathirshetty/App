package to.kuudere.anisuge.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import anisugkmp.composeapp.generated.resources.Res
import anisugkmp.composeapp.generated.resources.logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import to.kuudere.anisuge.screens.splash.SplashDestination

/**
 * Splash screen — fades in the logo, plays the splash video in background (desktop),
 * then navigates once the session check resolves AND the minimum display time has passed.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val destination by viewModel.destination.collectAsState()

    // Minimum splash display — won't disappear before this even if session resolves instantly
    var minTimeElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2_800)
        minTimeElapsed = true
    }

    // Navigate once both conditions are satisfied
    LaunchedEffect(destination, minTimeElapsed) {
        if (minTimeElapsed && destination != SplashDestination.Waiting) {
            when (destination) {
                is SplashDestination.GoHome -> onNavigateToHome()
                else                        -> onNavigateToAuth()
            }
        }
    }

    // Logo fade-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "splash_logo_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
        contentAlignment = Alignment.Center,
    ) {
        // Desktop video background rendered by platform-specific VideoPlayer expect on commonMain
        SplashVideoBackground()

        // Centered logo with fade-in
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Anisuge Logo",
            modifier = Modifier
                .size(120.dp)
                .alpha(alpha),
            contentScale = ContentScale.Fit,
        )
    }
}

// ── Default stub (common) — overridden in desktopMain ────────────────────────
@Composable
expect fun SplashVideoBackground()
