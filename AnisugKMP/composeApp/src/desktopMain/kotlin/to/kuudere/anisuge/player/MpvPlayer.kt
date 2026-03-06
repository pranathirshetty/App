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
        
        // Reduce terminal noise — only show errors
        mpv.mpv_set_option_string(handle, "terminal", "yes")
        mpv.mpv_set_option_string(handle, "msg-level", "all=error,osc=warn")

        // Subtitle options
        mpv.mpv_set_option_string(handle, "sub-auto", "fuzzy")
        mpv.mpv_set_option_string(handle, "embeddedfonts", if (config.embeddedFonts) "yes" else "no")
        // Always try to use fonts-dir if provided to allow ASS styling
        if (config.fontsDir != null) {
            mpv.mpv_set_option_string(handle, "sub-fonts-dir", config.fontsDir)
        }
        mpv.mpv_set_option_string(handle, "sub-font-provider", "fontconfig")
        mpv.mpv_set_option_string(handle, "sub-ass", "yes")
        mpv.mpv_set_option_string(handle, "sub-ass-override", "scale")

        // UI: enable mpv's built-in OSC (on-screen controller) — renders inside canvas
        // This is the only practical way to show player controls over a SwingPanel
        mpv.mpv_set_option_string(handle, "osc", if (config.showControls) "yes" else "no")
        mpv.mpv_set_option_string(handle, "osd-level", if (config.showControls) "1" else "0")
        mpv.mpv_set_option_string(handle, "osd-bar", if (config.showControls) "yes" else "no")
        // Enable mouse/keyboard so OSC is interactive
        mpv.mpv_set_option_string(handle, "input-default-bindings", if (config.showControls) "yes" else "no")
        mpv.mpv_set_option_string(handle, "input-vo-keyboard", if (config.showControls) "yes" else "no")
        // OSC style — uosc-like title bar positioning
        mpv.mpv_set_option_string(handle, "osc-layout", "box")
        mpv.mpv_set_option_string(handle, "osc-seekbarstyle", "bar")
        // Always keep last frame visible to prevent white flash during navigation
        mpv.mpv_set_option_string(handle, "keep-open", "yes")

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

    fun setSubFile(url: String) {
        ctx ?: return
        val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
            ?: return println("[MpvPlayer] setSubFile: prepareSubtitle returned null for $url")
        println("[MpvPlayer] sub-add -> $localPath")
        val r = lib?.mpv_command(ctx!!, arrayOf("sub-add", localPath, "select", null))
        println("[MpvPlayer] sub-add result: $r")
    }

    // ── Internal event loop ─────────────────────────────────────────────────

    private fun startEventLoop() {
        val handle = ctx ?: return
        val mpv = lib ?: return
        scope.launch {
            var lastSentPause = false
            var pendingSub: String? = null
            var pendingAllSubs: List<Pair<String, Boolean>>? = null
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

                        // Extract Audio Tracks
                        try {
                            val countPtr = mpv.mpv_get_property_string(handle, "track-list/count")
                            if (countPtr != null) {
                                val count = countPtr.getString(0).toIntOrNull() ?: 0
                                mpv.mpv_free(countPtr)
                                val tracks = mutableListOf<Pair<Int, String>>()
                                for (i in 0 until count) {
                                    val typePtr = mpv.mpv_get_property_string(handle, "track-list/$i/type")
                                    if (typePtr != null) {
                                        val type = typePtr.getString(0)
                                        mpv.mpv_free(typePtr)
                                        if (type == "audio") {
                                            val idPtr = mpv.mpv_get_property_string(handle, "track-list/$i/id")
                                            if (idPtr != null) {
                                                val id = idPtr.getString(0).toIntOrNull() ?: -1
                                                mpv.mpv_free(idPtr)
                                                if (id != -1) {
                                                    val langPtr = mpv.mpv_get_property_string(handle, "track-list/$i/lang")
                                                    val lang = langPtr?.getString(0) ?: "Audio $id"
                                                    if (langPtr != null) mpv.mpv_free(langPtr)
                                                    
                                                    val titlePtr = mpv.mpv_get_property_string(handle, "track-list/$i/title")
                                                    val title = titlePtr?.getString(0)
                                                    if (titlePtr != null) mpv.mpv_free(titlePtr)
                                                    
                                                    val label = if (title != null) "$lang - $title" else lang
                                                    tracks.add(id to label)
                                                }
                                            }
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) { state.audioTracks = tracks }
                            }
                        } catch (e: Exception) {
                            println("[MpvPlayer] Error extracting tracks: ${e.message}")
                        }

                        // Load all subtitles — default selected, rest available via OSC
                        val subsToLoad = pendingAllSubs ?: state.allSubUrls
                        if (!subsToLoad.isNullOrEmpty()) {
                            println("[MpvPlayer] FILE_LOADED: loading ${subsToLoad.size} subtitle(s)")
                            withContext(Dispatchers.IO) {
                                subsToLoad.forEach { (url, isDefault) ->
                                    val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
                                    if (localPath != null) {
                                        val flag = if (isDefault) "select" else "auto"
                                        lib?.mpv_command(ctx!!, arrayOf("sub-add", localPath, flag, null))
                                        println("[MpvPlayer] sub-add [$flag] -> $localPath")
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) { state.allSubUrls = null }
                            pendingAllSubs = null
                        }
                        
                        val singleSub = pendingSub
                        if (singleSub != null) {
                            if (singleSub == "NONE") {
                                mpv.mpv_set_option_string(handle, "sid", "no")
                            } else {
                                withContext(Dispatchers.IO) { setSubFile(singleSub) }
                            }
                            pendingSub = null
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

                // React to UI-driven pause toggle
                val wantPause = state.pauseRequested
                if (wantPause != lastSentPause) {
                    if (wantPause) pause() else resume()
                    lastSentPause = wantPause
                }

                // Handle seek
                state.seekTarget?.let { target ->
                    seekTo(target)
                    withContext(Dispatchers.Main) { state.seekTarget = null }
                }

                // Handle runtime sub switch (single track, e.g. user picks from UI)
                state.subFileUrl?.let { sub ->
                    if (state.isPlaying) {
                        println("[MpvPlayer] Runtime sub change -> $sub")
                        if (sub == "NONE") {
                            mpv.mpv_set_option_string(handle, "sid", "no")
                        } else {
                            withContext(Dispatchers.IO) { setSubFile(sub) }
                        }
                        withContext(Dispatchers.Main) { state.subFileUrl = null }
                    } else {
                        pendingSub = sub
                        withContext(Dispatchers.Main) { state.subFileUrl = null }
                    }
                }

                if (state.cycleAudio) {
                    lib?.mpv_command(ctx!!, arrayOf("cycle", "audio", null))
                    withContext(Dispatchers.Main) { state.cycleAudio = false }
                }

                state.selectedAudioTrack?.let { aid ->
                    mpv.mpv_set_option_string(handle, "aid", aid.toString())
                    // Don't clear selectedAudioTrack so state remains consistent
                }

                // Handle all-subs load (on new episode/server change before file ready)
                state.allSubUrls?.let { subs ->
                    if (state.isPlaying) {
                        println("[MpvPlayer] Runtime: loading ${subs.size} subtitle(s)")
                        withContext(Dispatchers.IO) {
                            subs.forEach { (url, isDefault) ->
                                val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
                                if (localPath != null) {
                                    val flag = if (isDefault) "select" else "auto"
                                    lib?.mpv_command(ctx!!, arrayOf("sub-add", localPath, flag, null))
                                }
                            }
                        }
                        withContext(Dispatchers.Main) { state.allSubUrls = null }
                    } else {
                        pendingAllSubs = subs
                        withContext(Dispatchers.Main) { state.allSubUrls = null }
                    }
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
