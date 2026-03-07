package to.kuudere.anisuge.player

import android.net.Uri
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
        LaunchedEffect(Unit) { onFinished?.invoke() }
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val surfaceView = remember { 
        SurfaceView(context).apply {
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

        MPVLib.create(context)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("hwdec", state.config.hwdec)
        val showOsc = if (state.config.showControls) "yes" else "no"
        MPVLib.setOptionString("osc", showOsc)
        MPVLib.setOptionString("osd-bar", showOsc)
        MPVLib.setOptionString("osd-level", if (state.config.showControls) "1" else "0")
        MPVLib.setOptionString("keep-open", "yes") // Prevent mpv from exiting or showing the drag-and-drop logo
        MPVLib.setOptionString("demuxer-seekable-cache", "no") // Force network re-fetch on seek; in-cache seek silently fails on HLS
        MPVLib.setOptionString("hr-seek", "no") // Disable hr-seek: its two-pass seek (keyframe then precise) causes double-seek on HLS, landing at wrong position
        MPVLib.setOptionString("input-default-bindings", showOsc)
        MPVLib.setOptionString("input-vo-keyboard", showOsc)
        
        if (state.config.muted) {
            MPVLib.setOptionString("mute", "yes")
        }
        if (state.config.loop) {
            MPVLib.setOptionString("loop-file", "yes")
        }

        // Subtitle options
        MPVLib.setOptionString("sub-auto", "fuzzy")
        MPVLib.setOptionString("embeddedfonts", if (state.config.embeddedFonts) "yes" else "no")
        state.config.fontsDir?.let {
            MPVLib.setOptionString("sub-fonts-dir", it)
        }
        MPVLib.setOptionString("sub-ass", "yes")
        MPVLib.setOptionString("sub-ass-override", "scale")
        
        MPVLib.init()

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
                    // Suppress stale time-pos updates while mpv is actively seeking;
                    // the slider would snap back to the pre-seek position otherwise.
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
                        
                        // Extract Audio Tracks
                        try {
                            val count = MPVLib.getPropertyInt("track-list/count")
                            val tracks = mutableListOf<Pair<Int, String>>()
                            for (i in 0 until (count ?: 0)) {
                                val type = MPVLib.getPropertyString("track-list/$i/type")
                                if (type == "audio") {
                                    val id = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
                                    val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "Audio $id"
                                    val title = MPVLib.getPropertyString("track-list/$i/title")
                                    val label = if (title != null) "$lang - $title" else lang
                                    tracks.add(id to label)
                                }
                            }
                            state.audioTracks = tracks
                        } catch (e: Exception) {
                            println("[VideoPlayerSurface] Error extracting tracks: ${e.message}")
                        }
                        
                        // Load pending subtitles asynchronously to prevent JNI blocking
                        state.allSubUrls?.let { subs ->
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                subs.forEach { (url, isDefault) ->
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
                            onFinished?.invoke()
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

        var urlLoaded = false
        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                MPVLib.attachSurface(holder.surface)
                MPVLib.setOptionString("force-window", "yes")
                if (!urlLoaded) {
                    urlLoaded = true
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        if (state.config.startPosition > 0.0) {
                            MPVLib.setOptionString("start", state.config.startPosition.toString())
                        }
                        MPVLib.command(arrayOf<String>("loadfile", resolvedUrl))
                    }
                } else {
                    MPVLib.setPropertyString("vo", "gpu")
                }
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                MPVLib.setPropertyString("android-surface-size", "${w}x${h}")
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setPropertyString("force-window", "no")
                MPVLib.detachSurface()
            }
        }
        
        surfaceView.holder.addCallback(callback)

        onDispose {
            MPVLib.removeObserver(observer)
            surfaceView.holder.removeCallback(callback)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                MPVLib.command(arrayOf<String>("stop"))
                MPVLib.destroy()
            }
        }
    }

    LaunchedEffect(state.pauseRequested) {
        state.isPaused = state.pauseRequested // Instant UI snap
        withContext(Dispatchers.IO) {
            MPVLib.setOptionString("pause", if (state.pauseRequested) "yes" else "no")
        }
    }

    LaunchedEffect(state.seekTarget) {
        val target = state.seekTarget ?: return@LaunchedEffect
        if (isSeeking.value) return@LaunchedEffect
        state.seekTarget = null
        isSeeking.value = true

        withContext(Dispatchers.IO) {
            if (target <= 0.0) {
                MPVLib.setPropertyString("force-seekable", "yes")
                MPVLib.command(arrayOf("seek", "1.0", "absolute", "keyframes"))
            } else {
                MPVLib.command(arrayOf("seek", target.toString(), "absolute", "keyframes"))
            }
        }

        delay(500)
        isSeeking.value = false
    }
    
    // Runtime sub change
    LaunchedEffect(state.subFileUrl) {
        state.subFileUrl?.let { sub ->
            if (sub == "NONE") {
                MPVLib.setPropertyInt("sid", 0) // Disable subtitles
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
    // Runtime all-subs load (on new episode/server change after file read)
    LaunchedEffect(state.allSubUrls) {
        state.allSubUrls?.let { subs ->
            if (state.isPlaying) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    subs.forEach { (url, isDefault) ->
                        val localPath = to.kuudere.anisuge.utils.SubtitleUtils.prepareSubtitle(url)
                        if (localPath != null) {
                            val flag = if (isDefault) "select" else "auto"
                            MPVLib.command(arrayOf<String>("sub-add", localPath, flag))
                        }
                    }
                }
            }
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
