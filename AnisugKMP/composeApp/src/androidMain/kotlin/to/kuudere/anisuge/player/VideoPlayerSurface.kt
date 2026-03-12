package to.kuudere.anisuge.player

import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

@Composable
actual fun VideoPlayerSurface(
    state: VideoPlayerState,
    modifier: Modifier,
    onFinished: (() -> Unit)?
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val resolvedUrl = remember(state.config.url) {
        val url = state.config.url
        when {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("/") -> url
            else -> {
                // Copy composeResources to temp file
                try {
                    val ext = url.substringAfterLast('.', "mp4")
                    val tmp = File(context.cacheDir, "cmp_res_${url.hashCode()}.$ext")
                    if (!tmp.exists()) {
                        context.assets.open(url).use { input ->
                            tmp.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    tmp.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    if (resolvedUrl == null) {
        LaunchedEffect(Unit) { currentOnFinished?.invoke() }
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val surfaceView = remember(resolvedUrl) { 
        SurfaceView(context).apply {
            // SurfaceView is more stable than TextureView for hardware-accelerated video
            // specifically avoiding the "destroyed mutex" crashes in libhwui.
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    MPVLib.attachSurface(holder.surface)
                    MPVLib.setOptionString("force-window", "yes")
                    
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        if (state.config.startPosition > 0.0) {
                            MPVLib.setOptionString("start", state.config.startPosition.toString())
                        }
                        MPVLib.command(arrayOf<String>("loadfile", resolvedUrl))
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
                    MPVLib.command(arrayOf("video-redraw"))
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    MPVLib.setPropertyString("vo", "null")
                    MPVLib.setPropertyString("force-window", "no")
                    MPVLib.detachSurface()
                }
            })

            setOnTouchListener { _, event ->
                val x = event.x.toInt().toString()
                val y = event.y.toInt().toString()
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> MPVLib.command(arrayOf("mouse", x, y, "0", "down"))
                    android.view.MotionEvent.ACTION_MOVE -> MPVLib.command(arrayOf("mouse", x, y))
                    android.view.MotionEvent.ACTION_UP -> MPVLib.command(arrayOf("mouse", x, y, "0", "up"))
                }
                true
            }
        }
    }

    val isSeeking = remember { mutableStateOf(false) }

    DisposableEffect(resolvedUrl) {
        val configDir = context.filesDir.absolutePath
        val subfontFile = File(configDir, "subfont.ttf")
        if (!subfontFile.exists()) {
            try {
                val systemFont = File("/system/fonts/Roboto-Regular.ttf")
                if (systemFont.exists()) {
                    systemFont.copyTo(subfontFile)
                } else {
                    val fallbackFont = File("/system/fonts/DroidSans.ttf")
                    if (fallbackFont.exists()) fallbackFont.copyTo(subfontFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        synchronized(MPVLibLock) {
            if (!isMPVInitialized) {
                MPVLib.create(context)
                MPVLib.setOptionString("config", "yes")
                MPVLib.setOptionString("config-dir", configDir)
                isMPVInitialized = true
            }
        }

        // Shared native engine, so we set non-global options per-instance here
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("hwdec", state.config.hwdec)
        val showOsc = if (state.config.showControls) "yes" else "no"
        MPVLib.setOptionString("osc", showOsc)
        MPVLib.setOptionString("osd-bar", showOsc)
        MPVLib.setOptionString("osd-level", if (state.config.showControls) "1" else "0")
        MPVLib.setOptionString("keep-open", "yes") 
        MPVLib.setOptionString("demuxer-seekable-cache", "no") 
        MPVLib.setOptionString("hr-seek", "no") 
        MPVLib.setOptionString("input-default-bindings", showOsc)
        MPVLib.setOptionString("input-vo-keyboard", showOsc)
        
        if (state.config.muted) {
            MPVLib.setOptionString("mute", "yes")
        }
        if (state.config.loop) {
            MPVLib.setOptionString("loop-file", "yes")
        }

        // Subtitle options
        // Use "no" for sub-auto when external subs are provided by the API (matching desktop behaviour).
        // "fuzzy" would cause mpv to also pick up embedded container subs which we don't want
        // when we are explicitly loading API-supplied subtitles via sub-add.
        MPVLib.setOptionString("sub-auto", "no")
        MPVLib.setOptionString("embeddedfonts", if (state.config.embeddedFonts) "yes" else "no")
        state.config.fontsDir?.let {
            MPVLib.setOptionString("sub-fonts-dir", it)
        }
        MPVLib.setOptionString("sub-ass", "yes")
        MPVLib.setOptionString("sub-ass-override", "scale")
        
        // Only init once
        synchronized(MPVLibLock) {
            if (!isMPVInited) {
                MPVLib.init()
                isMPVInited = true
            }
        }

        var isPausedForCache = false
        val observer = object : MPVLib.EventObserver {
            override fun eventProperty(property: String) {}
            override fun eventProperty(property: String, value: Long) {}
            override fun eventProperty(property: String, value: String) {}
            
            override fun eventProperty(property: String, value: Boolean) {
                when (property) {
                    "paused-for-cache" -> {
                        isPausedForCache = value
                        state.isBuffering = isPausedForCache || isSeeking.value
                    }
                    "seeking" -> {
                        isSeeking.value = value
                        state.isBuffering = isPausedForCache || isSeeking.value
                    }
                    "pause" -> {
                        state.isPaused = value
                    }
                }
            }
            
            override fun eventProperty(property: String, value: Double) {
                if (property == "time-pos") {
                    if (!isSeeking.value) {
                        state.position = value
                    }
                } else if (property == "duration") {
                    state.duration = value
                }
            }

            override fun event(eventId: Int) {
                when (eventId) {
                    MPVLib.MPV_EVENT_FILE_LOADED -> {
                        state.isPlaying = true
                        
                        try {
                            val count = MPVLib.getPropertyInt("track-list/count") ?: 0
                            val aTracks = mutableListOf<Pair<Int, String>>()
                            val sTracks = mutableListOf<Pair<Int, String>>()
                            for (i in 0 until count) {
                                val type = MPVLib.getPropertyString("track-list/$i/type")
                                val id = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
                                val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: (if (type == "audio") "Audio $id" else "Subtitle $id")
                                val title = MPVLib.getPropertyString("track-list/$i/title")
                                val label = if (title != null) "$lang - $title" else lang
                                
                                if (type == "audio") aTracks.add(id to label)
                                else if (type == "sub") sTracks.add(id to label)
                            }
                            state.audioTracks = aTracks
                            // Decide where to pull subtitle labels from.
                            // For offline files we still use mpv's track list, but
                            // for online streams we want the names provided by the API
                            // (stored earlier in state.allSubUrls) because the container
                            // track names vanish after load.
                            val isOffline = state.config.url.startsWith("file://") ||
                                    state.config.url.startsWith("/")
                            if (isOffline) {
                                state.subtitleTracks = sTracks
                                if (state.selectedSubtitleTrack == null && sTracks.isNotEmpty()) {
                                    state.selectedSubtitleTrack = sTracks.first().first
                                }
                            } else {
                                val apiSubs = state.allSubUrls ?: emptyList()
                                state.subtitleTracks = apiSubs.mapIndexed { idx, sub ->
                                    idx to sub.second
                                }
                                // select default if available
                                val defaultIndex = apiSubs.indexOfFirst { it.third }
                                if (defaultIndex >= 0) {
                                    state.selectedSubtitleTrack = defaultIndex
                                }
                            }
                        } catch (e: Exception) {
                            println("[VideoPlayerSurface] Error extracting tracks: ${e.message}")
                        }
                        
                        state.allSubUrls?.let { subs ->
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                subs.forEach { (url, name, isDefault) ->
                                    val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
                                    if (localPath != null) {
                                        val flag = if (isDefault) "select" else "auto"
                                        MPVLib.command(arrayOf<String>("sub-add", localPath, flag))
                                    }
                                }
                            }
                            state.allSubUrls = null
                        }
                    }
                    MPVLib.MPV_EVENT_END_FILE -> {
                        state.isPlaying = false
                        if (!state.config.loop) {
                            currentOnFinished?.invoke()
                        }
                    }
                }
            }
        }

        MPVLib.addObserver(observer)
        MPVLib.observeProperty("time-pos", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("duration", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("paused-for-cache", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("seeking", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("pause", MPVLib.MPV_FORMAT_FLAG)

        onDispose {
            MPVLib.removeObserver(observer)
            // Synchronously stop and detach to avoid races in native rendering threads during re-init
            MPVLib.command(arrayOf<String>("stop"))
            MPVLib.setPropertyString("vo", "null")
            MPVLib.detachSurface()
            // We do NOT call MPVLib.destroy() here because we reuse it across videos
            // to avoid the "pthread_mutex_lock called on a destroyed mutex" crash in libhwui.
        }
    }

    // ── MediaSession for earphone/headphone media button support ──
    val mediaSessionManager = remember(resolvedUrl) { MediaSessionManager(context) }
    DisposableEffect(resolvedUrl) {
        mediaSessionManager.start(
            state = state,
            onPlayPauseToggle = { shouldPause ->
                state.pauseRequested = shouldPause
            }
        )
        onDispose {
            mediaSessionManager.release()
        }
    }

    LaunchedEffect(state.pauseRequested) {
        state.isPaused = state.pauseRequested 
        withContext(Dispatchers.IO) {
            MPVLib.setOptionString("pause", if (state.pauseRequested) "yes" else "no")
        }
    }

    LaunchedEffect(state.isMuted) {
        withContext(Dispatchers.IO) {
            MPVLib.setOptionString("mute", if (state.isMuted) "yes" else "no")
        }
    }

    LaunchedEffect(state.aspectRatio) {
        withContext(Dispatchers.IO) {
            when (state.aspectRatio) {
                "Fit" -> {
                    MPVLib.setOptionString("video-aspect-override", "-1")
                    MPVLib.setOptionString("panscan", "0")
                }
                "Stretch" -> {
                    MPVLib.setOptionString("video-aspect-override", "16:9") 
                    MPVLib.setOptionString("panscan", "0")
                }
                "Zoom" -> {
                    MPVLib.setOptionString("video-aspect-override", "-1")
                    MPVLib.setOptionString("panscan", "1.0")
                }
            }
        }
    }

    LaunchedEffect(state.seekTarget) {
        val target = state.seekTarget ?: return@LaunchedEffect
        if (isSeeking.value) return@LaunchedEffect
        state.seekTarget = null
        isSeeking.value = true
        val safeTarget = target.coerceAtLeast(0.1)
        state.position = safeTarget

        withContext(Dispatchers.IO) {
            MPVLib.command(arrayOf("seek", safeTarget.toString(), "absolute"))
        }

        var lastPos = -1.0
        var waited = 0
        while (waited < 3000) {
            delay(100)
            waited += 100
            val pos = withContext(Dispatchers.IO) {
                try { MPVLib.getPropertyDouble("time-pos") } catch (_: Exception) { null }
            } ?: continue
            if (lastPos >= 0 && kotlin.math.abs(pos - lastPos) < 1.0) {
                state.position = pos
                break
            }
            lastPos = pos
        }
        isSeeking.value = false
    }
    
    // Reactively update sub-fonts-dir when the API fonts dir becomes available (may arrive
    // after the player is already initialised since font download happens in the ViewModel).
    LaunchedEffect(state.config.fontsDir) {
        state.config.fontsDir?.let { dir ->
            withContext(Dispatchers.IO) {
                MPVLib.setOptionString("sub-fonts-dir", dir)
                // Also disable embedded fonts now that we have API fonts
                MPVLib.setOptionString("embeddedfonts", "no")
            }
            println("[VideoPlayerSurface] Updated sub-fonts-dir: $dir")
        }
    }

    // Runtime sub change
    LaunchedEffect(state.subFileUrl) {
        state.subFileUrl?.let { sub ->
            if (sub == "NONE") {
                MPVLib.setPropertyInt("sid", 0) 
            } else {
                val localPath = withContext(Dispatchers.IO) {
                    to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(sub)
                }
                if (localPath != null) {
                    MPVLib.command(arrayOf<String>("sub-add", localPath, "select"))
                }
            }
            state.subFileUrl = null
        }
    }
    // Runtime all-subs load — load API-provided subtitles regardless of isPlaying state.
    // These subs come from the API (not the MKV container), so we must add them explicitly.
    // We no longer guard on isPlaying because the ViewModel may push subs before mpv
    // fires FILE_LOADED (especially on fast connections). Sub-add is safe to call anytime
    // after MPVLib.init().
    LaunchedEffect(state.allSubUrls) {
        state.allSubUrls?.let { subs ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                subs.forEach { (url, name, isDefault) ->
                    val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
                    if (localPath != null) {
                        val flag = if (isDefault) "select" else "auto"
                        MPVLib.command(arrayOf<String>("sub-add", localPath, flag))
                    }
                }
            }
            state.allSubUrls = null
        }
    }
    // Cycle audio tracks
    LaunchedEffect(state.cycleAudio) {
        if (state.cycleAudio) {
            withContext(Dispatchers.IO) {
                MPVLib.command(arrayOf<String>("cycle", "audio"))
            }
            state.cycleAudio = false
        }
    }

    LaunchedEffect(state.selectedAudioTrack) {
        state.selectedAudioTrack?.let { aid ->
            withContext(Dispatchers.IO) {
                MPVLib.setPropertyInt("aid", aid)
            }
        }
    }

    LaunchedEffect(state.selectedSubtitleTrack) {
        state.selectedSubtitleTrack?.let { sid ->
            withContext(Dispatchers.IO) {
                MPVLib.setPropertyInt("sid", sid)
            }
        }
    }

    AndroidView(
        factory = {
            surfaceView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.background(Color.Black)
    )
}

// Ensure MPV natively creates only once for app stability
private var isMPVInitialized = false
private var isMPVInited = false
private val MPVLibLock = Any()
