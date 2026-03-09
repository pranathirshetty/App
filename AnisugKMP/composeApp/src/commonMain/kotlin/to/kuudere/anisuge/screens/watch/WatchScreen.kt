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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Comment
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
import to.kuudere.anisuge.ui.WatchlistBottomSheet
import kotlinx.coroutines.launch
import to.kuudere.anisuge.AppComponent
import to.kuudere.anisuge.data.models.SessionCheckResult

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
    to.kuudere.anisuge.platform.SyncFullscreen(uiState.isFullscreen)

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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    uiState.loadingMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = msg, color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val isPanelActive = uiState.activeSidePanel != null
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
                        }
                        
                        AnimatedVisibility(
                            visible = isPanelActive,
                            enter = slideInHorizontally(animationSpec = tween(300)) { it } + expandHorizontally(animationSpec = tween(300), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutHorizontally(animationSpec = tween(300)) { it } + shrinkHorizontally(animationSpec = tween(300), shrinkTowards = Alignment.Start) + fadeOut(animationSpec = tween(300))
                        ) {
                            Box(Modifier.width(sidePanelWidth).fillMaxHeight()) {
                                SidePanelContent(uiState, viewModel, animeId)
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
fun SidePanelContent(uiState: WatchUiState, viewModel: WatchViewModel, animeId: String = "") {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(1.dp, Color(0xFF222222))
    ) {
        // Top header
        if (uiState.activeSidePanel != "comments" && uiState.activeSidePanel != null) {
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
                    else -> ""
                }
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleSidePanel(null) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.LightGray)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        }
        
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
                        var fastUserId by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(Unit) {
                            fastUserId = AppComponent.sessionStore.get()?.userId
                        }

                        val userProfile by produceState<to.kuudere.anisuge.data.models.UserProfile?>(null) {
                            val result = AppComponent.authService.checkSession()
                            value = if (result is SessionCheckResult.Valid) result.user else null
                        }
                        
                        // Use the Kuudere string slug passed to WatchScreen, not the anilist int
                        CommentsSection(
                            animeId = animeId,
                            episodeNumber = uiState.currentEpisodeNumber,
                            userId = userProfile?.effectiveId ?: fastUserId,
                            username = userProfile?.username,
                            userPfp = userProfile?.avatar,
                            onClose = { viewModel.toggleSidePanel(null) }
                        )
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
fun WatchVideoPlayer(
    uiState: WatchUiState,
    viewModel: WatchViewModel,
    modifier: Modifier = Modifier,
    onFullscreenToggle: () -> Unit,
    onBack: () -> Unit
) {
    var showWatchlistSheet by remember { mutableStateOf(false) }

    if (uiState.isLoadingVideo) {
        Box(modifier = modifier.background(Color.Black)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                uiState.loadingMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = msg, color = Color.LightGray, fontSize = 14.sp)
                }
            }
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
                showControls = useOsc,
                autoPlay = uiState.autoPlay
            )
            
            LaunchedEffect(uiState.availableSubtitles) {
                if (uiState.availableSubtitles.isNotEmpty()) {
                    playerState.allSubUrls = uiState.availableSubtitles.mapNotNull { sub ->
                        sub.url?.let { Triple(it, sub.title ?: sub.resolvedLang ?: "Subtitle", it == uiState.currentSubtitleUrl) }
                    }
                }
            }

            LaunchedEffect(uiState.currentSubtitleUrl) {
                playerState.subFileUrl = uiState.currentSubtitleUrl ?: "NONE"
                playerState.subFileName = uiState.availableSubtitles.firstOrNull { it.url == uiState.currentSubtitleUrl }?.let { it.title ?: it.resolvedLang } ?: "Subtitle"
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

            LaunchedEffect(uiState.episodeData, uiState.currentEpisodeNumber) {
                val allEps = uiState.episodeData?.allEpisodes ?: emptyList()
                val current = uiState.currentEpisodeNumber
                playerState.hasPrevEpisode = allEps.any { it.number < current }
                playerState.hasNextEpisode = allEps.any { it.number > current }
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

            LaunchedEffect(playerState.position) {
                if (playerState.duration <= 0) return@LaunchedEffect
                val pos = playerState.position

                // Auto skip intro
                if (uiState.autoSkipIntro) {
                    val intro = uiState.streamingData?.intro
                    if (intro != null && intro.start != null && intro.end != null) {
                        if (pos >= intro.start && pos < intro.end - 1.0) {
                            playerState.seekTarget = intro.end.toDouble()
                        }
                    }
                }

                // Auto skip outro
                if (uiState.autoSkipOutro) {
                    val outro = uiState.streamingData?.outro
                    if (outro != null && outro.start != null && outro.end != null) {
                        if (pos >= outro.start && pos < outro.end - 1.0) {
                            playerState.seekTarget = outro.end.toDouble()
                        }
                    }
                }
            }

            Box(modifier = modifier.background(Color.Black)) {
                VideoPlayerSurface(
                    state = playerState,
                    modifier = Modifier.fillMaxSize(),
                    onFinished = {
                        if (uiState.autoNext && playerState.hasNextEpisode) {
                            val nextEp = uiState.episodeData?.allEpisodes?.filter { it.number > uiState.currentEpisodeNumber }?.minByOrNull { it.number }
                            if (nextEp != null) viewModel.onEpisodeSelected(nextEp.number)
                        }
                    }
                )

                // Render out our cross-platform compose player controls overlay
                PlayerControls(
                    playerState = playerState,
                    streamingData = uiState.streamingData,
                    title = title,
                    isFullscreen = uiState.isFullscreen,
                    onFullscreenToggle = onFullscreenToggle,
                    onBack = onBack,
                    onNextEpisode = {
                        val nextEp = uiState.episodeData?.allEpisodes?.filter { it.number > uiState.currentEpisodeNumber }?.minByOrNull { it.number }
                        if (nextEp != null) viewModel.onEpisodeSelected(nextEp.number)
                    },
                    onPrevEpisode = {
                        val prevEp = uiState.episodeData?.allEpisodes?.filter { it.number < uiState.currentEpisodeNumber }?.maxByOrNull { it.number }
                        if (prevEp != null) viewModel.onEpisodeSelected(prevEp.number)
                    },
                    onCaptionsClick = { viewModel.toggleSettingsOverlay(SettingsMenuPage.SUBTITLES) },
                    onSettingsClick = { viewModel.toggleSettingsOverlay() },
                    onInfoClick = { viewModel.toggleSidePanel("info") },
                    onEpisodesClick = { viewModel.toggleSidePanel("episodes") },
                    onCommentsClick = { viewModel.toggleSidePanel("comments") },
                    onWatchlistClick = { viewModel.toggleSettingsOverlay(SettingsMenuPage.WATCHLIST) },
                    isInWatchlist = uiState.episodeData?.inWatchlist ?: false,
                    currentFolder = uiState.episodeData?.folder,
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
                    onAudioTrackSelected = { playerState.selectedAudioTrack = it },
                    onWatchlistStatusSelected = { folder -> viewModel.updateWatchlistStatus(folder) },
                    onAutoPlayToggle = { viewModel.setAutoPlay(it) },
                    onAutoNextToggle = { viewModel.setAutoNext(it) },
                    onAutoSkipIntroToggle = { viewModel.setAutoSkipIntro(it) },
                    onAutoSkipOutroToggle = { viewModel.setAutoSkipOutro(it) }
                )
            }
        } else {
            Box(modifier = modifier.background(Color.Black)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No streaming links available for server: ${uiState.currentServer}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.toggleSettingsOverlay() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Change Server", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
                        
                        var targetAudioLang: String? = null
                        if (isFromZen && isToZen) targetAudioLang = "sub"
                        else if (isFromHiya && isToZen) targetAudioLang = if (currentServer == "hiya-dub") "dub" else "sub"
                        
                        viewModel.changeServerWithState(
                            newServer = serverLabel, 
                            position = uiState.savedWatchPosition, 
                            targetAudioLang = targetAudioLang,
                            targetSubtitleLang = null,
                            targetSubtitleLangCode = null
                        )
                    },
                    onSpeedSelected = { viewModel.setSpeed(it) },
                    onCycleAudio = { },
                    audioTracks = emptyList(),
                    selectedAudioTrack = -1,
                    onAudioTrackSelected = { },
                    onWatchlistStatusSelected = { folder -> viewModel.updateWatchlistStatus(folder) },
                    onAutoPlayToggle = { viewModel.setAutoPlay(it) },
                    onAutoNextToggle = { viewModel.setAutoNext(it) },
                    onAutoSkipIntroToggle = { viewModel.setAutoSkipIntro(it) },
                    onAutoSkipOutroToggle = { viewModel.setAutoSkipOutro(it) }
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
