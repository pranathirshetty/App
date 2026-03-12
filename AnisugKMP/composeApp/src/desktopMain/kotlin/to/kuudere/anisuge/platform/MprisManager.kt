package to.kuudere.anisuge.platform

import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.annotations.DBusInterfaceName

/**
 * Basic D-Bus interface for MPRIS Player core.
 */
@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    fun Raise()
    fun Quit()
}

/**
 * Basic D-Bus interface for MPRIS Player controls.
 * This is the interface that earphone/headphone buttons (via DE/playerctl) will call on Linux.
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface Player : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
}

/**
 * Implementation of the MPRIS Player interface to hook D-Bus into our VideoPlayer.
 * This ensures media keys work gracefully on Linux Wayland/X11 even if JNativeHook is blocked.
 */
class LinuxMprisControl(
    private val onPlayPause: () -> Unit,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onStop: () -> Unit
) : Player, MediaPlayer2 {

    override fun Next() { onNext() }
    override fun Previous() { onPrevious() }
    override fun Pause() { onPause() }
    override fun PlayPause() { onPlayPause() }
    override fun Stop() { onStop() }
    override fun Play() { onPlay() }

    override fun Raise() {}
    override fun Quit() { onStop() }
    override fun getObjectPath(): String = "/org/mpris/MediaPlayer2"
}

object MprisManager {
    private var connection: DBusConnection? = null

    fun register(
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onStop: () -> Unit
    ) {
        if (!to.kuudere.anisuge.platform.isDesktopPlatform) return
        if (!System.getProperty("os.name").lowercase().contains("linux")) return

        try {
            if (connection == null) {
                connection = DBusConnectionBuilder.forSessionBus().build()
            }
            
            val mpris = LinuxMprisControl(
                onPlayPause = onPlayPause,
                onPlay = onPlayPause, // using toggle for play/pause
                onPause = onPlayPause, // using toggle for pause
                onNext = onNext,
                onPrevious = onPrevious,
                onStop = onStop
            )

            connection?.exportObject("/org/mpris/MediaPlayer2", mpris)
            // Claiming a bus name to advertise our presence
            connection?.requestBusName("org.mpris.MediaPlayer2.anisuge")
            println("[MprisManager] Registered MPRIS DBus object for Linux global media control.")
        } catch (e: Exception) {
            println("[MprisManager] Failed to register MPRIS (D-Bus): ${e.message}")
            e.printStackTrace()
        }
    }

    fun unregister() {
        if (!to.kuudere.anisuge.platform.isDesktopPlatform) return
        if (!System.getProperty("os.name").lowercase().contains("linux")) return

        try {
            connection?.releaseBusName("org.mpris.MediaPlayer2.anisuge")
            connection?.unExportObject("/org/mpris/MediaPlayer2")
            connection?.disconnect()
            connection = null
            println("[MprisManager] Unregistered MPRIS DBus object.")
        } catch (e: Exception) {
            println("[MprisManager] Error during MPRIS unregister: ${e.message}")
        }
    }
}
