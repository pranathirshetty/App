package to.kuudere.anisuge.platform

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Uses JNativeHook to listen for OS-level media keys globally (even when unfocused).
 * This works on Linux (X11), Windows, and macOS, allowing earphone/headphone buttons 
 * (Play/Pause, Next, Previous) to control the application.
 */
object GlobalMediaKeys {

    private val logger = Logger.getLogger(GlobalScreen::class.java.`package`.name)

    init {
        // Disable aggressive JNativeHook logging
        logger.level = Level.WARNING
        logger.useParentHandlers = false

        // Force JNativeHook to extract its native library to a writable directory.
        // This prevents "Permission denied" errors when the app is installed in /opt 
        // and tries to extract or load libraries from restricted locations.
        val homeDir = System.getProperty("user.home")
        val libDir = java.io.File(homeDir, ".anisurge/native").apply { mkdirs() }
        System.setProperty("jnativehook.lib.path", libDir.absolutePath)
    }

    private var currentListener: NativeKeyListener? = null
    private var isRegistered = false

    /**
     * Start listening for global media keys.
     * @param onPlayPause Called when play/pause is toggled.
     * @param onNext Called when next media button is pressed.
     * @param onPrevious Called when previous media button is pressed.
     */
    fun register(
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onStop: () -> Unit
    ) {
        if (isRegistered) unregister()

        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook()
            }

            currentListener = object : NativeKeyListener {
                override fun nativeKeyPressed(e: NativeKeyEvent) {
                    when (e.keyCode) {
                        NativeKeyEvent.VC_MEDIA_PLAY -> onPlayPause()
                        
                        NativeKeyEvent.VC_MEDIA_STOP -> onStop()
                        NativeKeyEvent.VC_MEDIA_NEXT -> onNext()
                        NativeKeyEvent.VC_MEDIA_PREVIOUS -> onPrevious()
                    }
                }
                override fun nativeKeyReleased(e: NativeKeyEvent) {}
                override fun nativeKeyTyped(e: NativeKeyEvent) {}
            }

            GlobalScreen.addNativeKeyListener(currentListener)
            isRegistered = true
            println("[GlobalMediaKeys] Registered global OS media key listener.")
        } catch (e: Exception) {
            println("[GlobalMediaKeys] Failed to register global media key hooks: ${e.message}")
        }
    }

    /**
     * Stop listening for global media keys and unregister the native hook
     * if no other listeners require it.
     */
    fun unregister() {
        if (!isRegistered) return
        
        try {
            currentListener?.let { GlobalScreen.removeNativeKeyListener(it) }
            currentListener = null
            
            // Note: We don't necessarily unregister the whole hook globally if other things were using it,
            // but for this app it's safe to clear it when not watching video.
            if (GlobalScreen.isNativeHookRegistered()) {
                 GlobalScreen.unregisterNativeHook()
            }
            
            isRegistered = false
            println("[GlobalMediaKeys] Unregistered global OS media key listener.")
        } catch (e: Exception) {
            println("[GlobalMediaKeys] Error unregistering global media keys: ${e.message}")
        }
    }
}
