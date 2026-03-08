package to.kuudere.anisuge.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import to.kuudere.anisuge.data.models.StreamingData
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
    streamingData: StreamingData? = null,
    title: String = "",
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {},
    onBack: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onPrevEpisode: () -> Unit = {},
    onCaptionsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onEpisodesClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onWatchlistClick: () -> Unit = {},
    isInWatchlist: Boolean = false,
    currentFolder: String? = null,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    var expectedPosition by remember { mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    val isLoading = playerState.isBuffering || (!playerState.isPlaying && playerState.duration <= 0.0)
    val isPlayingActively = playerState.isPlaying && !playerState.isPaused

    // Clear expected position when actual catches up
    LaunchedEffect(playerState.position) {
        val expected = expectedPosition
        if (expected != null && kotlin.math.abs(playerState.position - expected) < 2.0) {
            expectedPosition = null
        }
    }

    // Timeout expected position after 3s to prevent infinite freeze
    LaunchedEffect(expectedPosition) {
        if (expectedPosition != null) {
            delay(3000)
            expectedPosition = null
        }
    }

    // Auto-hide controls after 3.5s of inactivity
    fun scheduleHide() {
        if (playerState.isLocked) return // Don't auto-hide when locked? Or maybe DO hide.
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(3500)
            val currentIsLoading = playerState.isBuffering || (!playerState.isPlaying && playerState.duration <= 0.0)
            val currentIsPlayingActively = playerState.isPlaying && !playerState.isPaused
            if (!isSeeking && !currentIsLoading && currentIsPlayingActively) {
                controlsVisible = false
            }
        }
    }

    // Show controls initially
    LaunchedEffect(Unit) { scheduleHide() }

    // If it's loading or not playing actively, keep controls visible
    LaunchedEffect(isLoading, isPlayingActively) {
        if (isLoading || !isPlayingActively) {
            controlsVisible = true
            hideJob?.cancel()
        } else {
            scheduleHide()
        }
    }

    // Hook up desktop AWT Canvas clicks to toggle visibility
    LaunchedEffect(playerState.canvasClicked) {
        if (playerState.canvasClicked > 0) {
            if (playerState.isLocked) {
                controlsVisible = true
                scheduleHide()
            } else {
                controlsVisible = !controlsVisible
                if (controlsVisible) scheduleHide()
            }
        }
    }

    // Hook up desktop AWT Canvas pointer moves to wake up controls
    LaunchedEffect(playerState.canvasPointerMoved) {
        if (playerState.canvasPointerMoved > 0) {
            controlsVisible = true
            scheduleHide()
        }
    }

    val deviceControls = to.kuudere.anisuge.platform.rememberDeviceControls()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput("hover") {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                            val currentIsLoading = playerState.isBuffering || (!playerState.isPlaying && playerState.duration <= 0.0)
                            if (!controlsVisible && !currentIsLoading) {
                                controlsVisible = true
                            }
                            scheduleHide()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (playerState.isLocked) {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) scheduleHide()
                            return@detectTapGestures
                        }
                        val currentIsLoading = playerState.isBuffering || (!playerState.isPlaying && playerState.duration <= 0.0)
                        val currentIsPlayingActively = playerState.isPlaying && !playerState.isPaused
                        if (currentIsLoading || !currentIsPlayingActively) {
                            controlsVisible = true
                        } else {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) scheduleHide()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (playerState.isLocked) return@detectTapGestures
                        val width = size.width
                        if (offset.x < width / 3) {
                            // Double tap left → rewind 10s
                            val newPos = (playerState.position - 10.0).coerceAtLeast(0.0)
                            playerState.seekTarget = newPos
                            expectedPosition = newPos
                        } else if (offset.x > width * 2 / 3) {
                            // Double tap right → forward 10s
                            val newPos = (playerState.position + 10.0).coerceAtMost(playerState.duration)
                            playerState.seekTarget = newPos
                            expectedPosition = newPos
                        } else {
                            // Double tap center → toggle play/pause
                            playerState.pauseRequested = !playerState.isPaused
                        }
                        controlsVisible = true
                        scheduleHide()
                    }
                )
            }
            .pointerInput(playerState.isLocked) {
                if (playerState.isLocked) return@pointerInput
                var startVolume = 100.0
                var startBrightness = 0.0
                var isVolumeDrag = false
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (to.kuudere.anisuge.platform.isDesktopPlatform) {
                            startVolume = playerState.volume
                            startBrightness = playerState.brightness
                        } else {
                            startVolume = deviceControls.currentVolume.toDouble()
                            startBrightness = deviceControls.currentBrightness.toDouble()
                        }
                        isVolumeDrag = offset.x > size.width / 2f
                    },
                    onDragEnd = {
                        playerState.indicatorText = null
                    },
                    onDragCancel = {
                        playerState.indicatorText = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -(dragAmount / size.height)
                        if (isVolumeDrag) {
                            if (to.kuudere.anisuge.platform.isDesktopPlatform) {
                                val deltaVol = delta * 150.0
                                val newVol = (startVolume + deltaVol).coerceIn(0.0, 130.0)
                                startVolume = newVol
                                playerState.volume = newVol
                                playerState.indicatorText = "Volume: ${newVol.toInt()}%"
                            } else {
                                val deltaVol = delta * 1.5
                                val newVol = (startVolume + deltaVol).coerceIn(0.0, 1.0)
                                startVolume = newVol
                                deviceControls.setVolume(newVol.toFloat())
                                playerState.indicatorText = "Volume: ${(newVol * 100).toInt()}%"
                            }
                        } else {
                            if (to.kuudere.anisuge.platform.isDesktopPlatform) {
                                val deltaBri = delta * 150.0
                                val newBri = (startBrightness + deltaBri).coerceIn(-100.0, 100.0)
                                startBrightness = newBri
                                playerState.brightness = newBri
                                playerState.indicatorText = "Brightness: ${((newBri + 100) / 2).toInt()}%"
                            } else {
                                val deltaBri = delta * 1.5
                                val newBri = (startBrightness + deltaBri).coerceIn(0.0, 1.0)
                                startBrightness = newBri
                                deviceControls.setBrightness(newBri.toFloat())
                                playerState.indicatorText = "Brightness: ${(newBri * 100).toInt()}%"
                            }
                        }
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = playerState.indicatorText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(bottom = 64.dp)
        ) {
            Box(
                Modifier.background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = playerState.indicatorText ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
        
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top gradient + title bar (Only show if not locked)
                if (!playerState.isLocked) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .align(Alignment.TopCenter)
                            .padding(bottom = 24.dp) 
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            if (title.isNotEmpty()) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            IconButton(onClick = onCaptionsClick) {
                                Icon(
                                    getCCIcon(),
                                    contentDescription = "Captions",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Center Loading Indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 4.dp,
                        modifier = Modifier.align(Alignment.Center).size(56.dp)
                    )
                }

                // Bottom Controls Area
                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .then(if (isFullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier)
                ) {
                    if (playerState.isLocked) {
                        // Only show Lock button if locked
                        Row(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(
                                onClick = { playerState.isLocked = false; scheduleHide() },
                                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth().padding(bottom = 0.dp)) {
                            // 1. Progress Bar Row
                            val duration = playerState.duration
                            val activePosition = if (isSeeking) seekValue.toDouble() else expectedPosition ?: playerState.position
                            val progress = if (duration > 0) (activePosition / duration).toFloat().coerceIn(0f, 1f) else 0f

                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(activePosition),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .pointerInput(duration) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    if (duration <= 0) return@detectTapGestures
                                                    val tapValue = ((offset.x / size.width) * duration).toDouble().coerceIn(0.0, duration)
                                                    playerState.seekTarget = tapValue
                                                    expectedPosition = tapValue
                                                    scheduleHide()
                                                }
                                            )
                                        }
                                        .pointerInput(duration) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    if (duration <= 0) return@detectDragGestures
                                                    isSeeking = true
                                                    hideJob?.cancel()
                                                    seekValue = ((offset.x / size.width) * duration).toFloat().coerceIn(0f, duration.toFloat())
                                                },
                                                onDrag = { change, _ ->
                                                    if (duration <= 0) return@detectDragGestures
                                                    seekValue = ((change.position.x / size.width) * duration).toFloat().coerceIn(0f, duration.toFloat())
                                                },
                                                onDragEnd = {
                                                    if (duration > 0) {
                                                        playerState.seekTarget = seekValue.toDouble()
                                                        expectedPosition = seekValue.toDouble()
                                                        isSeeking = false
                                                        scheduleHide()
                                                    }
                                                },
                                                onDragCancel = { isSeeking = false; scheduleHide() }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                                        val w = size.width
                                        val h = size.height

                                        // Background track
                                        drawRoundRect(Color.White.copy(alpha = 0.25f), size = size, cornerRadius = CornerRadius(4.dp.toPx()))
                                        // White progress fill
                                        drawRoundRect(Color.White, size = Size(w * progress, h), cornerRadius = CornerRadius(4.dp.toPx()))

                                        // Intro highlight — yellow ON TOP of fill so always visible
                                        val intro = streamingData?.intro
                                        if (intro?.start != null && intro.end != null && duration > 0) {
                                            val x0 = ((intro.start / duration).toFloat() * w).coerceIn(0f, w)
                                            val x1 = ((intro.end / duration).toFloat() * w).coerceIn(0f, w)
                                            if (x1 > x0) drawRect(Color.Yellow, Offset(x0, 0f), Size(x1 - x0, h))
                                        }

                                        // Outro highlight — also yellow
                                        val outro = streamingData?.outro
                                        if (outro?.start != null && outro.end != null && duration > 0) {
                                            val x0 = ((outro.start / duration).toFloat() * w).coerceIn(0f, w)
                                            val x1 = ((outro.end / duration).toFloat() * w).coerceIn(0f, w)
                                            if (x1 > x0) drawRect(Color.Yellow, Offset(x0, 0f), Size(x1 - x0, h))
                                        }

                                        // Chapter dividers
                                        streamingData?.chapters?.forEach { ch ->
                                            if (ch.start_time != null && ch.start_time > 0 && duration > 0) {
                                                val x = (ch.start_time / duration).toFloat() * w
                                                if (x > 1f && x < w - 1f)
                                                    drawRect(Color.Black.copy(alpha = 0.8f), Offset(x - 1.dp.toPx(), 0f), Size(2.dp.toPx(), h))
                                            }
                                        }
                                    }
                                    // Thumb
                                    Box(Modifier.fillMaxWidth(progress).wrapContentWidth(Alignment.End)) {
                                        Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = formatDuration(duration),
                                    color = Color.White.copy(alpha=0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 2. Control Row
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left actions: Lock, Volume, Watchlist
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { playerState.isLocked = true; scheduleHide() }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { playerState.isMuted = !playerState.isMuted; scheduleHide() }, modifier = Modifier.size(38.dp)) {
                                        Icon(if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(onClick = { onWatchlistClick(); scheduleHide() }, modifier = Modifier.size(38.dp)) {
                                        Icon(
                                            if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                                            null, 
                                            tint = Color.White, 
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                // Main Playback controls
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { 
                                        val nPos = (playerState.position - 10).coerceAtLeast(0.0)
                                        playerState.seekTarget = nPos; expectedPosition = nPos; scheduleHide() 
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(
                                        onClick = { onPrevEpisode(); scheduleHide() }, 
                                        enabled = playerState.hasPrevEpisode,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.SkipPrevious, null, tint = if (playerState.hasPrevEpisode) Color.White else Color.Gray, modifier = Modifier.size(26.dp))
                                    }
                                    
                                    // BIG PLAY BUTTON
                                    Box(
                                        Modifier.size(54.dp).padding(4.dp).clip(CircleShape).background(Color.White).clickable { 
                                            playerState.pauseRequested = !playerState.isPaused
                                            scheduleHide()
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (playerState.isPaused || !playerState.isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onNextEpisode(); scheduleHide() }, 
                                        enabled = playerState.hasNextEpisode,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.SkipNext, null, tint = if (playerState.hasNextEpisode) Color.White else Color.Gray, modifier = Modifier.size(26.dp))
                                    }
                                    IconButton(onClick = { 
                                        val nPos = (playerState.position + 10).coerceAtMost(duration)
                                        playerState.seekTarget = nPos; expectedPosition = nPos; scheduleHide() 
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                // Right actions: Info, Episodes, Comments, [Fullscreen on Desktop]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onInfoClick(); scheduleHide() }, modifier = Modifier.size(38.dp)) {
                                        Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = { onEpisodesClick(); scheduleHide() }, modifier = Modifier.size(38.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = { onCommentsClick(); scheduleHide() }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    if (to.kuudere.anisuge.platform.isDesktopPlatform) {
                                        IconButton(onClick = { onFullscreenToggle(); scheduleHide() }, modifier = Modifier.size(40.dp)) {
                                            Icon(
                                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                                                null, 
                                                tint = Color.White, 
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
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

private fun getCCIcon(): ImageVector {
    return ImageVector.Builder(
        name = "ClosedCaption",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10f, 9.17f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, 5.66f)
            moveTo(17f, 9.17f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, 5.66f)
            
            moveTo(4f, 5f)
            horizontalLineToRelative(16f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
            verticalLineToRelative(10f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
            horizontalLineToRelative(-16f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
            verticalLineToRelative(-10f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
            close()
        }
    }.build()
}
