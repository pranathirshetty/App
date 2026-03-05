package to.kuudere.anisuge.screens.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

@Composable
actual fun AuthVideoBackground() {
    val vlcAvailable = remember { NativeDiscovery().discover() }
    if (!vlcAvailable) return

    val mediaPlayer = remember { EmbeddedMediaPlayerComponent() }

    DisposableEffect(Unit) {
        mediaPlayer.mediaPlayer().controls().setRepeat(true)
        mediaPlayer.mediaPlayer().audio().setMute(true)
        val stream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("drawable/auth_bg.mp4")
        if (stream != null) {
            val tmp = java.io.File.createTempFile("auth_bg_", ".mp4").also { it.deleteOnExit() }
            tmp.outputStream().use { out -> stream.copyTo(out) }
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
