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
    private var currentUrl: String? = null
    @Volatile private var isSeeking = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isAvailable: Boolean get() = lib != null

    private var renderCtx: com.sun.jna.ptr.PointerByReference? = null

    /**
     * Create the mpv handle, set ALL options BEFORE initialising,
     * then start playback. This is the correct libmpv lifecycle.
     */
    fun initAndPlay(url: String) {
        println("[MpvPlayer] initAndPlay: url=$url")
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
        r = mpv.mpv_set_option_string(handle, "vo", "libmpv")
        println("[MpvPlayer] set vo=libmpv → $r")
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
        if (config.fontsDir != null) {
            mpv.mpv_set_option_string(handle, "sub-fonts-dir", config.fontsDir)
        }
        mpv.mpv_set_option_string(handle, "sub-font-provider", "fontconfig")
        mpv.mpv_set_option_string(handle, "sub-ass", "yes")
        mpv.mpv_set_option_string(handle, "sub-ass-override", "scale")

        // UI: We disabled mpv's built-in OSC because we now render the 
        // Compose PlayerControls UI overlay directly
        mpv.mpv_set_option_string(handle, "osc", "no")
        mpv.mpv_set_option_string(handle, "osd-level", "0")
        mpv.mpv_set_option_string(handle, "osd-bar", "no")
        mpv.mpv_set_option_string(handle, "input-cursor", "no")
        mpv.mpv_set_option_string(handle, "input-default-bindings", if (config.showControls) "yes" else "no")
        mpv.mpv_set_option_string(handle, "input-vo-keyboard", if (config.showControls) "yes" else "no")

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

        // Setup SW Render Context
        val params = mpv_render_param().toArray(2) as Array<mpv_render_param>
        params[0].type = 1 // MPV_RENDER_PARAM_API_TYPE
        val strMem = com.sun.jna.Memory(3)
        strMem.setString(0, "sw")
        params[0].data = strMem
        params[1].type = 0 // terminate array
        params[1].data = null

        val ctxRef = com.sun.jna.ptr.PointerByReference()
        val rCtx = mpv.mpv_render_context_create(ctxRef, handle, params[0])
        println("[MpvPlayer] render_context_create -> $rCtx")
        renderCtx = ctxRef 

        if (config.startPosition > 0.0) {
            mpv.mpv_set_property_string(handle, "start", config.startPosition.toString())
        }

        startEventLoop()

        // Load file
        currentUrl = url
        r = mpv.mpv_command(handle, arrayOf("loadfile", url, null))
        println("[MpvPlayer] loadfile '$url' → $r")
    }

    fun pause()  { ctx?.let { lib?.mpv_set_property_string(it, "pause", "yes") } }
    fun resume() { ctx?.let { lib?.mpv_set_property_string(it, "pause", "no")  } }

    fun seekTo(seconds: Double) {
        val handle = ctx ?: return
        val mpv = lib ?: return
        isSeeking = true
        // HLS streams can't seek to absolute 0 — clamp to 0.1s
        val safeTarget = seconds.coerceAtLeast(0.1)
        mpv.mpv_command(handle, arrayOf("seek", safeTarget.toString(), "absolute", null))
        scope.launch {
            kotlinx.coroutines.delay(500)
            isSeeking = false
        }
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

        // Dedicated position polling loop since MPV_EVENT_TICK can be unreliable
        scope.launch {
            while (isActive && ctx != null) {
                if (state.isPlaying) {
                    val posPtr = mpv.mpv_get_property_string(handle, "time-pos")
                    if (posPtr != null) {
                        val pos = posPtr.getString(0).toDoubleOrNull() ?: 0.0
                        mpv.mpv_free(posPtr)
                        if (!isSeeking) {
                            withContext(Dispatchers.Main) { state.position = pos }
                        }
                    }

                    val bufPtr = mpv.mpv_get_property_string(handle, "paused-for-cache")
                    // "seeking" is true when mpv is actively jumping on the timeline
                    val seekPtr = mpv.mpv_get_property_string(handle, "seeking")

                    var isBuf = false
                    if (bufPtr != null) { isBuf = isBuf || bufPtr.getString(0) == "yes"; mpv.mpv_free(bufPtr) }
                    if (seekPtr != null) { isBuf = isBuf || seekPtr.getString(0) == "yes"; mpv.mpv_free(seekPtr) }

                    // Only show buffering if we are NOT intentionally paused, OR if we are seeking
                    // Actually, if we are paused, paused-for-cache might still be true if it was buffering when paused
                    withContext(Dispatchers.Main) { state.isBuffering = isBuf }

                    val pausePtr = mpv.mpv_get_property_string(handle, "pause")
                    if (pausePtr != null) {
                        val isPaused = pausePtr.getString(0) == "yes"
                        mpv.mpv_free(pausePtr)
                        withContext(Dispatchers.Main) { state.isPaused = isPaused }
                    }
                }
                kotlinx.coroutines.delay(50)
            }
        }

        scope.launch {
            var lastSentPause = false
            var pendingSub: String? = null
            var pendingAllSubs: List<Pair<String, Boolean>>? = null
            var lastSentAudioTrack: Int? = null
            while (isActive && ctx != null) {
                val event = mpv.mpv_wait_event(handle, 0.05)
                if (event != null) {
                    when (event.mpvEventId()) {
                        LibMpv.MPV_EVENT_END_FILE  -> {
                            withContext(Dispatchers.Main) { state.isPlaying = false }
                            if (!config.loop) onFinished?.invoke()
                        }
                        LibMpv.MPV_EVENT_FILE_LOADED -> {
                            isSeeking = false
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
                                launch(Dispatchers.IO) {
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
                                    launch(Dispatchers.IO) { setSubFile(singleSub) }
                                }
                                pendingSub = null
                            }
                        }
                        LibMpv.MPV_EVENT_TICK -> {
                            if (!isSeeking) {
                                val posPtr = mpv.mpv_get_property_string(handle, "time-pos")
                                if (posPtr != null) {
                                    val pos = posPtr.getString(0).toDoubleOrNull() ?: 0.0
                                    mpv.mpv_free(posPtr)
                                    withContext(Dispatchers.Main) { state.position = pos }
                                }
                            }
                        }
                        LibMpv.MPV_EVENT_SHUTDOWN -> break
                    }
                }

                // React to UI-driven pause toggle
                val wantPause = state.pauseRequested
                if (wantPause != lastSentPause) {
                    if (wantPause) pause() else resume()
                    withContext(Dispatchers.Main) { state.isPaused = wantPause } // Instant UI snap
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
                            launch(Dispatchers.IO) { setSubFile(sub) }
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
                    lastSentAudioTrack = null // reset so next explicit selection forces update
                }

                state.selectedAudioTrack?.let { aid ->
                    if (aid != lastSentAudioTrack) {
                        mpv.mpv_set_option_string(handle, "aid", aid.toString())
                        lastSentAudioTrack = aid
                    }
                }

                // Handle all-subs load (on new episode/server change before file ready)
                state.allSubUrls?.let { subs ->
                    if (state.isPlaying) {
                        println("[MpvPlayer] Runtime: loading ${subs.size} subtitle(s)")
                        launch(Dispatchers.IO) {
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
        val c = ctx
        val rc = renderCtx
        lib?.let { mpv ->
            if (rc != null) mpv.mpv_render_context_free(rc.value)
            if (c != null) mpv.mpv_terminate_destroy(c)
        }
        ctx = null
        renderCtx = null
    }

    fun renderFrame(width: Int, height: Int): ByteArray? {
        val rc = renderCtx?.value ?: return null
        val mpv = lib ?: return null

        val stride = width * 4L  // 4 bytes per pixel for "bgra"
        val buffer = com.sun.jna.Memory(stride * height)

        val sizeMem = com.sun.jna.Memory(8)
        sizeMem.setInt(0, width)
        sizeMem.setInt(4, height)

        val strideMem = com.sun.jna.Memory(8)
        strideMem.setLong(0, stride)

        val fmtMem = com.sun.jna.Memory(5)
        fmtMem.setString(0, "bgra")

        val params = mpv_render_param().toArray(6) as Array<mpv_render_param>
        params[0].type = 17; params[0].data = sizeMem
        params[1].type = 18; params[1].data = fmtMem
        params[2].type = 19; params[2].data = strideMem
        params[3].type = 20; params[3].data = buffer
        params[4].type = 0; params[4].data = null  // terminator

        mpv.mpv_render_context_update(rc)
        val r = mpv.mpv_render_context_render(rc, params[0])
        if (r < 0) {
            println("[MpvPlayer] render failed: $r")
            return null
        }

        return buffer.getByteArray(0, (stride * height).toInt())
    }
}
