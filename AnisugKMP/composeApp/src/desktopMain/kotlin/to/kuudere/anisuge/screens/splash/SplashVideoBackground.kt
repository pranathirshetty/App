package to.kuudere.anisuge.screens.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

/**
 * Desktop actual — uses vlcj to render splash.mp4 as a fullscreen background.
 * If libVLC is not installed the block is silently skipped (just shows the black bg).
 */
@Composable
actual fun SplashVideoBackground() {
    val vlcAvailable = remember { NativeDiscovery().discover() }
    if (!vlcAvailable) return

    val mediaPlayer = remember {
        EmbeddedMediaPlayerComponent()
    }

    DisposableEffect(Unit) {
        mediaPlayer.mediaPlayer().controls().setRepeat(true)
        // Extract resource to temp file so vlcj can load it
        val resourceStream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("drawable/splash.mp4")
        if (resourceStream != null) {
            val tmp = java.io.File.createTempFile("splash_", ".mp4").also { it.deleteOnExit() }
            tmp.outputStream().use { out -> resourceStream.copyTo(out) }
            mediaPlayer.mediaPlayer().media().play(tmp.absolutePath)
        }
        onDispose {
            mediaPlayer.mediaPlayer().controls().stop()
            mediaPlayer.release()
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory  = { mediaPlayer },
    )
}
