package to.kuudere.anisuge.screens.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.Canvas
import java.awt.Color
import java.io.File
import com.sun.jna.Native
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color as ComposeColor
@Composable
actual fun SplashVideoBackground(onVideoFinished: () -> Unit) {
    val mpvAvailable = remember { 
        try {
            Runtime.getRuntime().exec(arrayOf("mpv", "--version")).waitFor() == 0
        } catch(e: Exception) { false }
    }

    if (!mpvAvailable) {
        onVideoFinished()
        return
    }

    val canvas = remember { 
        object : Canvas() {
            override fun paint(g: java.awt.Graphics?) {}
            override fun update(g: java.awt.Graphics?) {}
        }.apply {
            background = Color.BLACK
        }
    }

    DisposableEffect(Unit) {
        var process: Process? = null
        
        val resourceStream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("composeResources/anisurge.composeapp.generated.resources/drawable/splash.mp4")
            
        if (resourceStream != null) {
            val tmp = File.createTempFile("splash_", ".mp4").also { it.deleteOnExit() }
            tmp.outputStream().use { out -> resourceStream.copyTo(out) }
            
            Thread {
                var wid: Long = 0
                // Wait until canvas has a native peer
                while(wid == 0L) {
                    try {
                        wid = Native.getComponentID(canvas)
                    } catch(e: Exception) { }
                    Thread.sleep(50)
                }
                
                process = ProcessBuilder(
                    "mpv",
                    "--wid=$wid",
                    "--vo=x11",
                    "--hwdec=auto",
                    "--mute=yes",
                    "--keep-open=no",
                    "--no-osc",
                    "--no-osd-bar",
                    "--osd-level=0",
                    "--no-input-default-bindings",
                    "--no-terminal",
                    tmp.absolutePath
                ).inheritIO().start()
                
                process?.waitFor()
                onVideoFinished()
            }.start()
        } else {
            onVideoFinished()
        }

        onDispose {
            process?.destroy()
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize().background(ComposeColor.Black),
        factory  = { canvas },
    )
}
