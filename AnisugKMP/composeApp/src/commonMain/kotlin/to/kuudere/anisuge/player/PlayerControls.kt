package to.kuudere.anisuge.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared cross-platform player controls overlay.
 * Renders on top of VideoPlayerSurface in the Compose layer.
 */
@Composable
fun PlayerControls(
    playerState: VideoPlayerState,
    title: String = "",
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {},
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    // Auto-hide controls after 3s of inactivity
    fun scheduleHide() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(3500)
            if (!isSeeking) controlsVisible = false
        }
    }

    // Show controls initially then auto-hide
    LaunchedEffect(Unit) { scheduleHide() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) scheduleHide()
                    },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 3) {
                            // Double tap left → rewind 10s
                            val newPos = (playerState.position - 10.0).coerceAtLeast(0.0)
                            playerState.seekTarget = newPos
                        } else if (offset.x > width * 2 / 3) {
                            // Double tap right → forward 10s
                            val newPos = (playerState.position + 10.0).coerceAtMost(playerState.duration)
                            playerState.seekTarget = newPos
                        } else {
                            // Double tap center → toggle play/pause
                            playerState.pauseRequested = !playerState.pauseRequested
                        }
                        controlsVisible = true
                        scheduleHide()
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top gradient + title bar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Center play/pause button
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        IconButton(
                            onClick = {
                                playerState.pauseRequested = !playerState.pauseRequested
                                scheduleHide()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                if (playerState.pauseRequested || !playerState.isPlaying)
                                    Icons.Default.PlayArrow
                                else
                                    Icons.Default.Pause,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom gradient + seek bar + controls
                Column(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(bottom = 8.dp)
                ) {
                    // Seekbar
                    val duration = playerState.duration
                    val position = if (isSeeking) seekValue.toDouble() else playerState.position
                    val progress = if (duration > 0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f

                    Slider(
                        value = progress,
                        onValueChange = { value ->
                            isSeeking = true
                            seekValue = (value * duration).toFloat()
                            hideJob?.cancel()
                        },
                        onValueChangeFinished = {
                            playerState.seekTarget = seekValue.toDouble()
                            isSeeking = false
                            scheduleHide()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(24.dp)
                    )

                    // Time + controls row
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time display
                        Text(
                            text = "${formatDuration(position)} / ${formatDuration(duration)}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.weight(1f))

                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val newPos = (playerState.position - 10.0).coerceAtLeast(0.0)
                                playerState.seekTarget = newPos
                                scheduleHide()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val newPos = (playerState.position + 10.0).coerceAtMost(playerState.duration)
                                playerState.seekTarget = newPos
                                scheduleHide()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Fullscreen toggle
                        IconButton(
                            onClick = {
                                onFullscreenToggle()
                                scheduleHide()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val totalSeconds = seconds.toLong().coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}
