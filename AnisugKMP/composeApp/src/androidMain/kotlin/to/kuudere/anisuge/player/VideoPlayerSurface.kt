package to.kuudere.anisuge.player

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * Android actual of [VideoPlayerSurface].
 * Uses ExoPlayer (Media3).
 * Compose resource paths (composeResources/...) are resolved from Android assets.
 */
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayerSurface(
    state: VideoPlayerState,
    modifier: Modifier,
    onFinished: (() -> Unit)?,
) {
    val context = LocalContext.current

    // Resolve the playback URI — handles both http(s) and composeResources asset paths
    val resolvedUri = remember(state.config.url) {
        val url = state.config.url
        when {
            url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("file://") || url.startsWith("/") -> {
                Uri.parse(url)
            }
            url.startsWith("composeResources/") || !url.contains("://") -> {
                // It's a Compose Multiplatform asset — copy from assets to a temp file
                try {
                    val ext = url.substringAfterLast('.', "mp4")
                    val tmp = File(context.cacheDir, "cmp_res_${url.hashCode()}.$ext")
                    if (!tmp.exists()) {
                        context.assets.open(url).use { input ->
                            tmp.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    Uri.fromFile(tmp)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    // If we couldn't resolve at all, call finished and show black
    if (resolvedUri == null) {
        LaunchedEffect(Unit) { onFinished?.invoke() }
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val exoPlayer = remember(resolvedUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(resolvedUri))
            playWhenReady = !state.pauseRequested
            volume = if (state.config.muted) 0f else 1f
            repeatMode = if (state.config.loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            prepare()

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    state.isPlaying = isPlaying
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    state.isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_ENDED) onFinished?.invoke()
                }
                override fun onPlayerError(error: PlaybackException) {
                    state.error = error.message
                    onFinished?.invoke()
                }
            })

            if (state.config.startPosition > 0.0) {
                seekTo((state.config.startPosition * 1000).toLong())
            }
        }
    }

    // Sync position
    LaunchedEffect(exoPlayer) {
        while (true) {
            state.position = exoPlayer.currentPosition / 1000.0
            state.duration = exoPlayer.duration.let { if (it == C.TIME_UNSET) 0.0 else it / 1000.0 }
            kotlinx.coroutines.delay(500)
        }
    }

    // Pause/resume
    LaunchedEffect(state.pauseRequested) {
        if (state.pauseRequested) exoPlayer.pause() else exoPlayer.play()
    }

    // Seek
    LaunchedEffect(state.seekTarget) {
        state.seekTarget?.let {
            exoPlayer.seekTo((it * 1000).toLong())
            state.seekTarget = null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = state.config.showControls
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.useController = state.config.showControls
        },
        modifier = modifier
    )
}
