package to.kuudere.anisuge.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import com.sun.jna.Native

/**
 * Desktop actual of [VideoPlayerSurface].
 * Renders video via JNA libmpv into an AWT Canvas embedded in Compose via SwingPanel.
 */
@Composable
actual fun VideoPlayerSurface(
    state:      VideoPlayerState,
    modifier:   Modifier,
    onFinished: (() -> Unit)?,
) {
    val canvas = remember {
        object : java.awt.Canvas() {
            override fun paint (g: java.awt.Graphics?) {}
            override fun update(g: java.awt.Graphics?) {}
        }.apply { background = java.awt.Color.BLACK }
    }

    val player = remember(state.config) {
        MpvPlayer(config = state.config, state = state, onFinished = onFinished)
    }

    if (!player.isAvailable) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    // Resolve classpath resources to temp files; HTTP/file paths pass through
    val resolvedUrl = remember(state.config.url) {
        val url = state.config.url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) {
            url
        } else {
            val stream = Thread.currentThread().contextClassLoader
                ?.getResourceAsStream(url)
            if (stream != null) {
                val ext = url.substringAfterLast('.', "mp4")
                val tmp = java.io.File.createTempFile("mpv_res_", ".$ext").also { it.deleteOnExit() }
                tmp.outputStream().use { out -> stream.copyTo(out) }
                tmp.absolutePath
            } else {
                println("[VideoPlayerSurface] WARNING: Resource not found: $url")
                ""
            }
        }
    }

    DisposableEffect(resolvedUrl) {
        if (resolvedUrl.isEmpty()) {
            onDispose {}
        } else {
            val thread = Thread {
                // Wait for the AWT canvas to get a valid native window ID
                var wid = 0L
                while (wid == 0L) {
                    try { wid = Native.getComponentID(canvas) } catch (_: Exception) {}
                    Thread.sleep(16)
                }
                // Now create mpv with the wid, init, and play — all in correct order
                player.initAndPlay(wid, resolvedUrl)
            }.also { it.isDaemon = true; it.start() }

            onDispose {
                thread.interrupt()
                player.destroy()
            }
        }
    }

    LaunchedEffect(state.subFileUrl) {
        state.subFileUrl?.let { player.setSubFile(it) }
    }

    SwingPanel(
        modifier = modifier.fillMaxSize(),
        factory  = { canvas },
    )
}
