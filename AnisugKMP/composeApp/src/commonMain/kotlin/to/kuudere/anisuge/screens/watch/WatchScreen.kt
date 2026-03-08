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
import coil3.compose.AsyncImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

@Composable
fun WatchScreen(
    animeId: String,
    episodeNumber: Int,
    server: String? = null,
    lang: String? = null,
    viewModel: WatchViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var isLandscape by remember { mutableStateOf(true) }

    LockScreenOrientation(isLandscape)

    val handleBack = {
        isLandscape = false
        onBack()
    }

    to.kuudere.anisuge.platform.PlatformBackHandler {
        handleBack()
    }

    LaunchedEffect(animeId, episodeNumber) {
        viewModel.initialize(animeId, episodeNumber, server, lang)
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
                    val isPanelActive = uiState.activeSidePanel != null && !uiState.isFullscreen
                    val sidePanelWidth = 350.dp // Twitch standard width
                    
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            key("video_player") {
                                WatchVideoPlayer(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onFullscreenToggle = { viewModel.setFullscreen(!uiState.isFullscreen) },
                                    onBack = handleBack
                                )
                            }
                            if (!uiState.isFullscreen) {
                                PlayerOverlayButtons(viewModel, uiState)
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = isPanelActive,
                            enter = slideInHorizontally(animationSpec = tween(300)) { it } + expandHorizontally(animationSpec = tween(300), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutHorizontally(animationSpec = tween(300)) { it } + shrinkHorizontally(animationSpec = tween(300), shrinkTowards = Alignment.Start) + fadeOut(animationSpec = tween(300))
                        ) {
                            Box(Modifier.width(sidePanelWidth).fillMaxHeight()) {
                                SidePanelContent(uiState, viewModel)
                            }
                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SidePanelContent(uiState: WatchUiState, viewModel: WatchViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(1.dp, Color(0xFF222222))
    ) {
        // Top header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = when (uiState.activeSidePanel) {
                "info" -> "Anime Info"
                "episodes" -> "Episodes"
                "comments" -> "Comments"
                else -> ""
            }
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.toggleSidePanel(null) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.LightGray)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        
        // Content
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = uiState.activeSidePanel,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "SidePanelAnimation"
            ) { activePanel ->
                when (activePanel) {
                    "info" -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            val animeInfo = uiState.episodeData?.animeInfo
                            Text(
                                text = animeInfo?.english ?: "Unknown",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Likes / Dislikes
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ThumbUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(animeInfo?.likes?.toString() ?: "0", color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ThumbDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(animeInfo?.dislikes?.toString() ?: "0", color = Color.White)
                                }
                            }
                        }
                    }
                    "episodes" -> {
                        val episodes = uiState.episodeData?.allEpisodes ?: emptyList()
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            items(episodes.sortedBy { it.number }) { episode ->
                                val isSelected = episode.number == uiState.currentEpisodeNumber
                                val thumbnail = uiState.thumbnails[episode.number.toString()]
                                
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White else Color(0xFF111111))
                                        .clickable { viewModel.onEpisodeSelected(episode.number) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (thumbnail != null) {
                                        AsyncImage(
                                            model = thumbnail,
                                            contentDescription = "Episode ${episode.number} Thumbnail",
                                            modifier = Modifier
                                                .width(100.dp)
                                                .aspectRatio(16f/9f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.DarkGray),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .width(100.dp)
                                                .aspectRatio(16f/9f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, tint = Color.LightGray)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Episode ${episode.number}",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        val title = episode.titles?.firstOrNull()
                                        if (!title.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                title,
                                                color = if (isSelected) Color.DarkGray else Color.LightGray,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    if (isSelected) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                    "comments" -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Comments integration here", color = Color.LightGray)
                        }
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select an option", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerOverlayButtons(viewModel: WatchViewModel, uiState: WatchUiState) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OverlayIconButton(Icons.Default.Info, "Info", uiState.activeSidePanel == "info") { 
                viewModel.toggleSidePanel("info") 
            }
            OverlayIconButton(Icons.Default.List, "Episodes", uiState.activeSidePanel == "episodes") { 
                viewModel.toggleSidePanel("episodes") 
            }
            OverlayIconButton(Icons.Default.Comment, "Comments", uiState.activeSidePanel == "comments") { 
                viewModel.toggleSidePanel("comments") 
            }
        }
    }
}

@Composable
fun OverlayIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, isActive: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent)
    ) {
        Icon(icon, contentDescription, tint = if (isActive) Color.White else Color.LightGray)
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
                    playerState.allSubUrls = uiState.availableSubtitles.mapNotNull { sub ->
                        sub.url?.let { it to (it == uiState.currentSubtitleUrl) }
                    }
                }
            }

            LaunchedEffect(uiState.currentSubtitleUrl) {
                playerState.subFileUrl = uiState.currentSubtitleUrl ?: "NONE"
            }

            LaunchedEffect(playerState.isPlaying, playerState.isPaused) {
                while (playerState.isPlaying && !playerState.isPaused) {
                    kotlinx.coroutines.delay(5000)
                    if (playerState.duration > 0) {
                        val currentAudioLabel = playerState.audioTracks.firstOrNull { it.first == playerState.selectedAudioTrack }?.second?.lowercase() ?: ""
                        val trackLang = if (currentAudioLabel.contains("eng")) "dub" else "sub"
                        viewModel.saveProgress(playerState.position, playerState.duration, language = trackLang)
                    }
                }
            }

            LaunchedEffect(playerState.audioTracks, uiState.targetLang) {
                if (playerState.audioTracks.isNotEmpty() && uiState.targetLang != null) {
                    val target = uiState.targetLang
                    val track = playerState.audioTracks.find { 
                        val label = it.second.lowercase()
                        if (target == "dub") label.contains("eng") else (label.contains("jpn") || label.contains("ja")) 
                    }
                    if (track != null && playerState.selectedAudioTrack != track.first) {
                        playerState.selectedAudioTrack = track.first
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
                val servers = listOf("hiya", "hiya-dub", "zen", "zen2")
                SettingsOverlay(
                    uiState = uiState,
                    servers = servers,
                    onDismiss = { viewModel.toggleSettingsOverlay() },
                    onQualitySelected = { viewModel.setQuality(it) },
                    onSubtitleSelected = { url -> 
                        val selectedSub = uiState.availableSubtitles.firstOrNull { it.url == url }
                        val lang = selectedSub?.title ?: selectedSub?.resolvedLang
                        viewModel.setSubtitle(url, lang) 
                    },
                    onServerSelected = { serverLabel -> 
                        val currentServer = uiState.currentServer.lowercase()
                        val newServer = serverLabel.lowercase()
                        
                        val isFromZen = currentServer.startsWith("zen")
                        val isToZen = newServer.startsWith("zen")
                        val isFromHiya = currentServer.startsWith("hiya")
                        val isToHiya = newServer.startsWith("hiya")

                        val currentAudioLabel = playerState.audioTracks.firstOrNull { it.first == playerState.selectedAudioTrack }?.second?.lowercase() ?: ""
                        val currentTrackLang = if (currentAudioLabel.contains("eng")) "dub" else "sub"
                        
                        var targetAudioLang: String? = null
                        val currentSubData = uiState.availableSubtitles.firstOrNull { it.url == uiState.currentSubtitleUrl }
                        var targetSubtitleLang = currentSubData?.title ?: currentSubData?.resolvedLang
                        var targetSubtitleLangCode = currentSubData?.language ?: currentSubData?.lang
                        
                        // Zen > zen > subtitle title, time, audio track
                        if (isFromZen && isToZen) {
                            targetAudioLang = currentTrackLang
                        } 
                        // hiya > zen(1-2) > time, subtitle lang > default audio sub
                        // hiya-dub > zen(1-2) > time, subtitle lang > default audio dub
                        else if (isFromHiya && isToZen) {
                            targetAudioLang = if (currentServer == "hiya-dub") "dub" else "sub"
                            // Note: Zen servers ignore exactly named 'English' string since they use 'S&S@DiabloTripleA' 
                            //   so we rely fully on `targetSubtitleLangCode == 'eng'`
                        } 
                        // Zen > hiya > time only
                        else if (isFromZen && isToHiya) {
                            targetAudioLang = null
                            targetSubtitleLang = null
                            targetSubtitleLangCode = null
                        } 
                        // Hiya > hia > time & subtitle title
                        else if (isFromHiya && isToHiya) {
                            targetAudioLang = null
                        }
                        
                        viewModel.changeServerWithState(
                            newServer = serverLabel, 
                            position = playerState.position, 
                            targetAudioLang = targetAudioLang,
                            targetSubtitleLang = targetSubtitleLang,
                            targetSubtitleLangCode = targetSubtitleLangCode
                        )
                    },
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
