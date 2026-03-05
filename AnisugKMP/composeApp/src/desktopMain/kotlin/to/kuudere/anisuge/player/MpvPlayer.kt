package to.kuudere.anisuge.player

import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kotlin wrapper around LibMpv.
 *
 * Lifecycle:
 *   1. MpvPlayer(config) — loads libmpv only (no create/init yet).
 *   2. initAndPlay(wid, url) — creates handle, sets ALL options (incl. wid), initialises, loads file.
 *   3. destroy() — must be called in DisposableEffect.onDispose.
 */
internal class MpvPlayer(
    private val config: VideoPlayerConfig,
    private val state:  VideoPlayerState,
    private val onFinished: (() -> Unit)? = null,
) {
    private val lib: LibMpv? = LibMpv.load()
    private var ctx: Pointer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isAvailable: Boolean get() = lib != null

    /**
     * Create the mpv handle, set ALL options (including wid) BEFORE initialising,
     * then start playback. This is the correct libmpv lifecycle.
     */
    fun initAndPlay(wid: Long, url: String) {
        println("[MpvPlayer] initAndPlay: wid=$wid url=$url")
        val mpv = lib ?: run {
            println("[MpvPlayer] ERROR: lib is null (libmpv not loaded)")
            return
        }
        val handle = mpv.mpv_create() ?: run {
            println("[MpvPlayer] ERROR: mpv_create() returned null")
            return
        }
        ctx = handle
        println("[MpvPlayer] mpv_create() OK")

        // ── Pre-init options ─────────────────────────────────────────────────
        var r: Int
        r = mpv.mpv_set_option_string(handle, "wid", wid.toString())
        println("[MpvPlayer] set wid=$wid → $r")
        r = mpv.mpv_set_option_string(handle, "vo", "x11")
        println("[MpvPlayer] set vo=x11 → $r")
        r = mpv.mpv_set_option_string(handle, "hwdec", config.hwdec)
        println("[MpvPlayer] set hwdec=${config.hwdec} → $r")
        r = mpv.mpv_set_option_string(handle, "mute", if (config.muted) "yes" else "no")
        println("[MpvPlayer] set mute → $r")
        r = mpv.mpv_set_option_string(handle, "loop-file", if (config.loop) "inf" else "no")
        println("[MpvPlayer] set loop-file → $r")
        
        // Enable terminal output so we can see mpv errors
        mpv.mpv_set_option_string(handle, "terminal", "yes")
        mpv.mpv_set_option_string(handle, "msg-level", "all=v")

        // Subtitle options
        mpv.mpv_set_option_string(handle, "sub-auto", "fuzzy")
        mpv.mpv_set_option_string(handle, "embeddedfonts", if (config.embeddedFonts) "yes" else "no")
        mpv.mpv_set_option_string(handle, "sub-font-provider", "fontconfig")

        // UI options
        mpv.mpv_set_option_string(handle, "osc", "no")
        mpv.mpv_set_option_string(handle, "osd-level", "0")
        mpv.mpv_set_option_string(handle, "input-default-bindings", "no")
        mpv.mpv_set_option_string(handle, "input-vo-keyboard", "no")
        mpv.mpv_set_option_string(handle, "keep-open", if (config.loop) "yes" else "no")

        val initResult = mpv.mpv_initialize(handle)
        println("[MpvPlayer] mpv_initialize() → $initResult")
        if (initResult != 0) {
            println("[MpvPlayer] ERROR: mpv_initialize() failed")
            mpv.mpv_terminate_destroy(handle)
            ctx = null
            return
        }

        // Load file
        r = mpv.mpv_command(handle, arrayOf("loadfile", url, null))
        println("[MpvPlayer] loadfile '$url' → $r")

        if (config.startPosition > 0.0) {
            mpv.mpv_set_property_string(handle, "time-pos", config.startPosition.toString())
        }

        startEventLoop()
    }

    fun pause()  { ctx?.let { lib?.mpv_set_property_string(it, "pause", "yes") } }
    fun resume() { ctx?.let { lib?.mpv_set_property_string(it, "pause", "no")  } }

    fun seekTo(seconds: Double) {
        ctx ?: return
        lib?.mpv_command(ctx!!, arrayOf("seek", seconds.toString(), "absolute", null))
    }

    fun setSubFile(path: String) {
        ctx ?: return
        lib?.mpv_command(ctx!!, arrayOf("sub-add", path, "select", null))
    }

    // ── Internal event loop ─────────────────────────────────────────────────

    private fun startEventLoop() {
        val handle = ctx ?: return
        val mpv = lib ?: return
        scope.launch {
            while (isActive && ctx != null) {
                val event = mpv.mpv_wait_event(handle, 0.5) ?: continue
                when (event.mpvEventId()) {
                    LibMpv.MPV_EVENT_END_FILE  -> {
                        withContext(Dispatchers.Main) { state.isPlaying = false }
                        if (!config.loop) onFinished?.invoke()
                    }
                    LibMpv.MPV_EVENT_FILE_LOADED -> {
                        withContext(Dispatchers.Main) {
                            state.isPlaying = true
                            state.isBuffering = false
                            val durPtr = mpv.mpv_get_property_string(handle, "duration")
                            if (durPtr != null) {
                                state.duration = durPtr.getString(0).toDoubleOrNull() ?: 0.0
                                mpv.mpv_free(durPtr)
                            }
                        }
                    }
                    LibMpv.MPV_EVENT_TICK -> {
                        val posPtr = mpv.mpv_get_property_string(handle, "time-pos")
                        if (posPtr != null) {
                            val pos = posPtr.getString(0).toDoubleOrNull() ?: 0.0
                            mpv.mpv_free(posPtr)
                            withContext(Dispatchers.Main) { state.position = pos }
                        }
                    }
                    LibMpv.MPV_EVENT_SHUTDOWN -> break
                }

                // React to UI-driven commands
                if (state.pauseRequested != !state.isPlaying) {
                    if (state.pauseRequested) pause() else resume()
                }
                state.seekTarget?.let { target ->
                    seekTo(target)
                    withContext(Dispatchers.Main) { state.seekTarget = null }
                }
                state.subFileUrl?.let { sub ->
                    setSubFile(sub)
                    withContext(Dispatchers.Main) { state.subFileUrl = null }
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
        ctx?.let { handle ->
            try {
                // Stop playback first to prevent X11 BadWindow errors
                lib?.mpv_command(handle, arrayOf("stop", null))
                Thread.sleep(50) // Give mpv a moment to release the window
                lib?.mpv_terminate_destroy(handle)
            } catch (_: Exception) {}
        }
        ctx = null
    }
}
