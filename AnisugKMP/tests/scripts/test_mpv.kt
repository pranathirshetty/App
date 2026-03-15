import javax.swing.*
import java.awt.Canvas
import com.sun.jna.Native
import java.io.File

fun main() {
    val frame = JFrame("Test mpv")
    frame.setSize(800, 600)
    val canvas = Canvas()
    frame.add(canvas)
    frame.isVisible = true
    
    var wid: Long = 0
    while(wid == 0L) {
        try { wid = Native.getComponentID(canvas) } catch(e: Exception) {}
        Thread.sleep(50)
    }
    println("Wid: \$wid")
    
    val p = ProcessBuilder("mpv", "--wid=\$wid", "--loop", "--vo=x11", "composeApp/src/commonMain/composeResources/drawable/splash.mp4").inheritIO().start()
    p.waitFor()
}
