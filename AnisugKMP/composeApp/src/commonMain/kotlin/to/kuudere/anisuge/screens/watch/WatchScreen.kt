package to.kuudere.anisuge.screens.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import to.kuudere.anisuge.platform.LockScreenOrientation
import to.kuudere.anisuge.platform.isDesktopPlatform
import to.kuudere.anisuge.player.PlayerControls
import to.kuudere.anisuge.player.VideoPlayerState
import to.kuudere.anisuge.player.VideoPlayerSurface
import to.kuudere.anisuge.player.rememberVideoPlayerState

@Composable
fun WatchScreen(
    animeId: String,
    episodeNumber: Int,
    viewModel: WatchViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LockScreenOrientation(uiState.isFullscreen)

    LaunchedEffect(animeId, episodeNumber) {
        viewModel.initialize(animeId, episodeNumber)
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!uiState.isFullscreen) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            // Spacer for sticky player height
                            Spacer(Modifier.fillMaxWidth().aspectRatio(16f/9f))

                            // Anime details
                            val animeInfo = uiState.episodeData?.animeInfo
                            val currentEpData = uiState.episodeData?.allEpisodes?.find { it.number == uiState.currentEpisodeNumber }
                            if (animeInfo != null) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        text = animeInfo.english ?: "Unknown",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (currentEpData != null) {
                                        val title = currentEpData.titles?.firstOrNull() ?: ""
                                        Text(
                                            text = "Episode ${uiState.currentEpisodeNumber} - $title",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Action buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Likes/Dislikes
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF333333).copy(alpha = 0.5f))
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ThumbUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(animeInfo.likes?.toString() ?: "0", color = Color.White, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.width(16.dp))
                                            Icon(Icons.Default.ThumbDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(animeInfo.dislikes?.toString() ?: "0", color = Color.White, fontWeight = FontWeight.Medium)
                                        }

                                        ActionButton(icon = Icons.Default.Share, label = "Share") { /* Share */ }
                                        ActionButton(icon = Icons.Outlined.BookmarkBorder, label = "Save") { /* Save */ }
                                        ActionButton(icon = Icons.Default.Flag, label = "Report") { /* Report */ }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Comments Button
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1A1A1A))
                                            .clickable { viewModel.toggleSidePanel("comments") }
                                            .padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Comment, null, tint = Color.White)
                                            Spacer(Modifier.width(12.dp))
                                            Text("Comments", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                Modifier
                                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(uiState.episodeData?.totalComments?.toString() ?: "0", color = Color.White, fontSize = 12.sp)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                                        }
                                    }

                                    Spacer(Modifier.height(24.dp))

                                    // Episode List Selection
                                    Text("Episodes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(16.dp))

                                    // For simplicity, just list all without pagination for initial implementation
                                    val episodes = uiState.episodeData?.allEpisodes ?: emptyList()
                                    episodes.sortedBy { it.number }.forEach { episode ->
                                        val isSelected = episode.number == uiState.currentEpisodeNumber
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color.Red.copy(alpha = 0.1f) else Color(0xFF1A1A1A))
                                                .border(1.dp, if (isSelected) Color.Red else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { viewModel.onEpisodeSelected(episode.number) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Episode ${episode.number}",
                                                color = if (isSelected) Color.Red else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.PlayArrow, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Video Player dynamically snaps without unmounting
                    key("video_player") {
                        WatchVideoPlayer(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = if (uiState.isFullscreen) Modifier.fillMaxSize() 
                                       else Modifier.fillMaxWidth().aspectRatio(16f/9f).align(Alignment.TopCenter),
                            onFullscreenToggle = { viewModel.setFullscreen(!uiState.isFullscreen) },
                            onBack = onBack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.background(Color(0xFF333333).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WatchVideoPlayer(
    uiState: WatchUiState,
    viewModel: WatchViewModel,
    modifier: Modifier = Modifier,
    onFullscreenToggle: () -> Unit,
    onBack: () -> Unit
) {
    if (uiState.isLoadingVideo) {
        Box(modifier = modifier.background(Color.Black)) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
        }
    } else {
        val currentUrl = uiState.availableQualities.find { it.first == uiState.currentQuality }?.second
            ?: uiState.availableQualities.firstOrNull()?.second

        if (currentUrl != null) {
            // Desktop: We now use our custom Compose PlayerControls instead of mpv's OSC
            val useOsc = false
            val playerState = rememberVideoPlayerState(
                url = currentUrl,
                startPosition = uiState.savedWatchPosition,
                fontsDir = uiState.currentFontsDir,
                showControls = useOsc
            )
            
            LaunchedEffect(uiState.availableSubtitles) {
                if (uiState.availableSubtitles.isNotEmpty()) {
                    playerState.allSubUrls = uiState.availableSubtitles.map { (_, url) ->
                        url to (url == uiState.currentSubtitleUrl)
                    }
                }
            }

            LaunchedEffect(uiState.currentSubtitleUrl) {
                playerState.subFileUrl = uiState.currentSubtitleUrl ?: "NONE"
            }

            LaunchedEffect(playerState.isPlaying) {
                while (playerState.isPlaying) {
                    kotlinx.coroutines.delay(5000)
                    if (playerState.duration > 0) {
                        viewModel.saveProgress(playerState.position, playerState.duration)
                    }
                }
            }

            val animeInfo = uiState.episodeData?.animeInfo
            val currentEp = uiState.episodeData?.allEpisodes?.find { it.number == uiState.currentEpisodeNumber }
            val title = buildString {
                if (animeInfo?.english != null) append(animeInfo.english)
                if (currentEp != null) {
                    if (isNotEmpty()) append(" • ")
                    append("Episode ${uiState.currentEpisodeNumber}")
                    currentEp.titles?.firstOrNull()?.let { epTitle ->
                        if (epTitle.isNotEmpty()) append(" - $epTitle")
                    }
                }
            }

            Box(modifier = modifier.background(Color.Black)) {
                VideoPlayerSurface(
                    state = playerState,
                    modifier = Modifier.fillMaxSize()
                )

                // Render out our cross-platform compose player controls overlay
                PlayerControls(
                    playerState = playerState,
                    streamingData = uiState.streamingData,
                    title = title,
                    isFullscreen = uiState.isFullscreen,
                    onFullscreenToggle = onFullscreenToggle,
                    onBack = onBack,
                    onCaptionsClick = { viewModel.toggleSettingsOverlay(SettingsMenuPage.SUBTITLES) },
                    onSettingsClick = { viewModel.toggleSettingsOverlay() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (uiState.showSettingsOverlay) {
                val servers = listOf("hiya", "hiya-dub", "zen", "zen2", "pahe", "allmanga", "allmanga-dub")
                SettingsOverlay(
                    uiState = uiState,
                    servers = servers,
                    onDismiss = { viewModel.toggleSettingsOverlay() },
                    onQualitySelected = { viewModel.setQuality(it) },
                    onSubtitleSelected = { viewModel.setSubtitle(it) },
                    onServerSelected = { viewModel.setServer(it) },
                    onSpeedSelected = { viewModel.setSpeed(it) },
                    onCycleAudio = { playerState.cycleAudio = true },
                    audioTracks = playerState.audioTracks,
                    selectedAudioTrack = playerState.selectedAudioTrack,
                    onAudioTrackSelected = { playerState.selectedAudioTrack = it }
                )
            }
        } else {
            Box(modifier = modifier.background(Color.Black)) {
                Text(
                    text = "No streaming links available",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val hStr = h.toString().padStart(2, '0')
    val mStr = m.toString().padStart(2, '0')
    val sStr = s.toString().padStart(2, '0')
    return if (h > 0) "$hStr:$mStr:$sStr" else "$mStr:$sStr"
}
