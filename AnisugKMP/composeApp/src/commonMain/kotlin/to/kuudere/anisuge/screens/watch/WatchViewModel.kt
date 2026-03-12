package to.kuudere.anisuge.screens.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import to.kuudere.anisuge.data.models.EpisodeDataResponse
import to.kuudere.anisuge.data.models.EpisodeLink
import to.kuudere.anisuge.data.models.StreamingData
import to.kuudere.anisuge.data.services.InfoService
import okio.Path.Companion.toPath
import to.kuudere.anisuge.player.VideoPlayerConfig
import to.kuudere.anisuge.player.VideoPlayerState

import to.kuudere.anisuge.screens.watch.SettingsMenuPage

data class WatchUiState(
    val isLoading: Boolean = true,
    val isLoadingVideo: Boolean = false,
    val loadingMessage: String? = null,
    val episodeData: EpisodeDataResponse? = null,
    val thumbnails: Map<String, String> = emptyMap(),
    val currentEpisodeNumber: Int = 1,
    val currentServer: String = "zen2",
    val streamingData: StreamingData? = null,
    val availableQualities: List<Pair<String, String>> = emptyList(), // Pair(Quality, URL)
    val currentQuality: String = "Auto",
    val availableSubtitles: List<to.kuudere.anisuge.data.models.SubtitleData> = emptyList(), // Store the full data object
    val currentSubtitleUrl: String? = null,
    val currentFontsDir: String? = null,
    val playbackSpeed: Double = 1.0,
    val savedWatchPosition: Double = 0.0,
    val showSettingsOverlay: Boolean = false,
    val initialSettingsPage: SettingsMenuPage? = SettingsMenuPage.MAIN,
    val activeSidePanel: String? = null, // "episodes" or "comments"
    val isFullscreen: Boolean = false,
    val targetLang: String? = null,
    val targetSubtitleLang: String? = null,
    val targetSubtitleLangCode: String? = null,
    val isUpdatingWatchlist: Boolean = false,
    val autoPlay: Boolean = true,
    val autoNext: Boolean = true,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    val defaultLang: Boolean = false,
    val syncPercentage: Int = 80,
    val didMarkWatched: Boolean = false,
    val offlinePath: String? = null,
    // Offline metadata
    val offlineTitle: String? = null
)

class WatchViewModel(
    private val infoService: InfoService,
    private val settingsStore: to.kuudere.anisuge.data.services.SettingsStore,
    private val settingsService: to.kuudere.anisuge.data.services.SettingsService
) : ViewModel() {
    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState = _uiState.asStateFlow()

    private var currentAnimeId: String = ""
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch { settingsStore.autoPlayFlow.collect { v -> _uiState.update { it.copy(autoPlay = v) } } }
        viewModelScope.launch { settingsStore.autoNextFlow.collect { v -> _uiState.update { it.copy(autoNext = v) } } }
        viewModelScope.launch { settingsStore.autoSkipIntroFlow.collect { v -> _uiState.update { it.copy(autoSkipIntro = v) } } }
        viewModelScope.launch { settingsStore.autoSkipOutroFlow.collect { v -> _uiState.update { it.copy(autoSkipOutro = v) } } }
        viewModelScope.launch { settingsStore.defaultLangFlow.collect { v -> _uiState.update { it.copy(defaultLang = v) } } }
        viewModelScope.launch { settingsStore.syncPercentageFlow.collect { v -> _uiState.update { it.copy(syncPercentage = v) } } }
    }

    fun initialize(animeId: String, episodeNumber: Int, server: String? = null, lang: String? = null, offlinePath: String? = null, offlineTitle: String? = null) {
        // Cancel any ongoing loading IMMEDIATELY before touching state
        // This prevents race conditions when switching between videos
        loadJob?.cancel()

        currentAnimeId = animeId

        // Update state IMMEDIATELY (synchronously) to prevent stale data from being used
        // before the coroutine starts. This fixes the bug where switching from online
        // to offline would briefly show the old online video.
        _uiState.update {
            it.copy(
                currentEpisodeNumber = episodeNumber,
                isLoading = true,
                loadingMessage = if (offlinePath != null) "Loading offline video..." else "Fetching episode $episodeNumber...",
                isLoadingVideo = false,
                episodeData = null,
                streamingData = null,
                availableQualities = emptyList(),
                availableSubtitles = emptyList(),
                currentSubtitleUrl = null,
                currentFontsDir = null,
                savedWatchPosition = 0.0,
                targetLang = null,
                targetSubtitleLang = null,
                targetSubtitleLangCode = null,
                didMarkWatched = false,
                offlinePath = offlinePath,
                offlineTitle = offlineTitle
            )
        }

        loadJob = viewModelScope.launch {
            if (offlinePath != null) {
                loadOfflineStream(offlinePath)
            } else {
                fetchEpisodeData(episodeNumber, server, lang)
            }
        }
    }

    private suspend fun loadOfflineStream(path: String) {
        // Guard: if state shows we're not in offline mode anymore, abort
        if (_uiState.value.offlinePath != path) {
            println("[WatchVM] loadOfflineStream aborted - path mismatch: expected $path, got ${_uiState.value.offlinePath}")
            return
        }

        // Check for local subs/fonts in same dir
        val dir = try { path.substringBeforeLast("/") } catch(e: Exception) { null }
        val subs = mutableListOf<to.kuudere.anisuge.data.models.SubtitleData>()

        if (dir != null) {
            try {
                val dirPath = dir.toPath()
                if (okio.FileSystem.SYSTEM.exists(dirPath)) {
                    val files = okio.FileSystem.SYSTEM.list(dirPath)
                    files.forEach { file ->
                        val name = file.name
                        if (name.startsWith("subtitle") && (name.endsWith(".ass") || name.endsWith(".vtt") || name.endsWith(".srt"))) {
                            val label = name.substringAfter("subtitle_", "").substringBeforeLast(".").ifEmpty { "Default" }
                            subs.add(to.kuudere.anisuge.data.models.SubtitleData(
                                languageName = label,
                                url = "file://${file.toString()}",
                                format = name.substringAfterLast(".")
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val defaultSub = subs.find { it.url?.contains("subtitle.ass") == true || it.url?.contains("subtitle.vtt") == true } ?: subs.firstOrNull()
        
        _uiState.update { it.copy(
            isLoading = false,
            isLoadingVideo = false,
            currentQuality = "Offline",
            availableQualities = listOf("Offline" to path),
            availableSubtitles = subs,
            currentSubtitleUrl = defaultSub?.url,
            currentFontsDir = dir,
            streamingData = null,
            currentServer = "offline"
        ) }
    }

    private suspend fun fetchEpisodeData(episodeNumber: Int, reqServer: String? = null, reqLang: String? = null) {
        // Guard: if we're in offline mode, don't fetch online data
        if (_uiState.value.offlinePath != null) {
            println("[WatchVM] fetchEpisodeData aborted - offlinePath is set")
            return
        }

        _uiState.update { it.copy(isLoading = true, loadingMessage = "Fetching episode data...") }
            val data = infoService.getEpisodes(currentAnimeId, episodeNumber)

            // Check if cancelled or switched to offline before updating state
            if (!coroutineContext.isActive || _uiState.value.offlinePath != null) {
                println("[WatchVM] fetchEpisodeData aborted after API call - cancelled or switched to offline")
                return
            }

            if (data != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        loadingMessage = "Switching servers...",
                        episodeData = data,
                        savedWatchPosition = data.current?.toDouble() ?: 0.0
                    )
                }

                data.animeInfo?.anilist?.let { anilistId ->
                    fetchThumbnails(anilistId)
                }

                // Check if cancelled before proceeding to load video
                if (!coroutineContext.isActive) return

                val fallbackPriority = listOf("zen2", "zen", "hiya", "hiya-dub")
                var targetServerName: String? = null
                var finalLang: String? = reqLang
                val links = data.episodeLinks ?: emptyList()
                val hasLinks = links.isNotEmpty()

                if (hasLinks && reqServer != null && reqLang != null) {
                    val rawServerName = if (reqServer.lowercase() == "zen-2") "Zen-2" else reqServer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    val matchedLink = links.find {
                        it.serverName.equals(rawServerName, ignoreCase = true) &&
                        it.dataType.equals(reqLang, ignoreCase = true)
                    }
                    if (matchedLink != null) {
                        targetServerName = reqServer.lowercase()
                        if (targetServerName == "zen-2") targetServerName = "zen2"
                        finalLang = reqLang
                    }
                }

                if (hasLinks && targetServerName == null) {
                    val fallbackLang = reqLang ?: if (_uiState.value.defaultLang) "dub" else "sub"
                    for (candidate in fallbackPriority) {
                        val apiServerName = when (candidate) {
                            "zen2" -> "Zen-2"
                            "zen" -> "Zen"
                            "hiya", "hiya-dub" -> "Hiya"
                            else -> candidate
                        }
                        val apiLang = if (candidate.endsWith("-dub")) "dub" else fallbackLang
                        
                        val matched = links.find { 
                            it.serverName.equals(apiServerName, ignoreCase = true) && 
                            it.dataType.equals(apiLang, ignoreCase = true) 
                        }
                        
                        if (matched != null) {
                            targetServerName = if (candidate == "hiya" && apiLang == "dub") "hiya-dub" else candidate
                            finalLang = apiLang
                            break
                        }
                    }
                }

                if (!hasLinks) {
                    targetServerName = when {
                        !reqServer.isNullOrBlank() -> reqServer.lowercase().let { if (it == "zen-2") "zen2" else it }
                        reqLang.equals("dub", ignoreCase = true) -> "hiya-dub"
                        else -> fallbackPriority.first()
                    }
                    finalLang = when {
                        !reqLang.isNullOrBlank() -> reqLang
                        targetServerName == "hiya-dub" -> "dub"
                        else -> "sub"
                    }
                }

                val serverName = targetServerName ?: "zen2"
                println("[WatchVM] Selected server: $serverName (requested: $reqServer, lang: $reqLang, targetLang: $finalLang)")
                
                _uiState.update { it.copy(targetLang = finalLang) }

                loadVideoStream(serverName)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
    }

    private fun fetchThumbnails(anilistId: Int) {
        viewModelScope.launch {
            val response = infoService.getThumbnails(anilistId)
            if (response != null && response.success && response.thumbnails != null) {
                _uiState.update { it.copy(thumbnails = response.thumbnails) }
            }
        }
    }

    private suspend fun loadVideoStream(serverName: String) {
        // Guard: if we're in offline mode, don't load online stream
        if (_uiState.value.offlinePath != null) {
            println("[WatchVM] loadVideoStream aborted - offlinePath is set, should not load online stream")
            return
        }

        val currState = _uiState.value
        val anilistId = currState.episodeData?.animeInfo?.anilist ?: return
        val episodeNum = currState.currentEpisodeNumber

        _uiState.update { it.copy(isLoadingVideo = true, currentServer = serverName, loadingMessage = "Fetching streaming URL...") }

            // Convert zen2 to zen-2 for kuudere API
            val apiServerName = if (serverName == "zen2") "zen-2" else serverName
            val response = infoService.getVideoStream(anilistId, episodeNum, apiServerName)

            // Check if cancelled before updating state
            if (!coroutineContext.isActive) return

            val streamData = response?.directLink?.data ?: response?.data
            if (streamData != null) {
                val qualities = mutableListOf<Pair<String, String>>()
                if (!streamData.sources.isNullOrEmpty()) {
                    streamData.sources.forEach {
                        if (it.quality != null && it.url != null) {
                            qualities.add(it.quality to it.url)
                        }
                    }
                } else if (streamData.m3u8_url != null) {
                    qualities.add("Auto" to streamData.m3u8_url)
                }

                val subtitles = streamData.subtitles ?: emptyList()
                
                // Refined Subtitle Selection Logic mirroring +page.server.ts
                var selectedSubUrl: String? = null
                val isDub = currState.targetLang == "dub"
                val englishSubs = subtitles.filter { 
                    val lang = (it.language ?: it.lang ?: "").lowercase()
                    lang == "en" || lang == "eng" || it.languageName?.lowercase()?.contains("english") == true
                }

                if (englishSubs.isNotEmpty()) {
                    val candidates = englishSubs
                    if (isDub) {
                        // Priority for DUB: 1. dubtitle, 2. cc, 3. forced, 4. regular English, 5. first available
                        selectedSubUrl = candidates.find { it.title?.lowercase()?.contains("dubtitle") == true }?.url
                            ?: candidates.find { it.title?.lowercase()?.contains("cc") == true }?.url
                            ?: candidates.find { it.title?.lowercase()?.contains("forced") == true }?.url
                            ?: candidates.find { !listOf("signs", "songs", "commentary").any { k -> it.title?.lowercase()?.contains(k) == true } }?.url
                            ?: candidates.firstOrNull()?.url
                    } else {
                        // Priority for SUB: 1. regular English, 2. full, 3. first available
                        selectedSubUrl = candidates.find { !listOf("cc", "forced", "dubtitle", "signs", "songs", "commentary").any { k -> it.title?.lowercase()?.contains(k) == true } }?.url
                            ?: candidates.find { it.title?.lowercase()?.contains("full") == true && !listOf("cc", "forced", "dubtitle").any { k -> it.title?.lowercase()?.contains(k) == true } }?.url
                            ?: candidates.find { !listOf("cc", "forced", "dubtitle").any { k -> it.title?.lowercase()?.contains(k) == true } }?.url
                            ?: candidates.firstOrNull()?.url
                    }
                }

                // Final fallbacks
                if (selectedSubUrl == null) {
                    selectedSubUrl = subtitles.find { it.is_default == true }?.url 
                        ?: subtitles.firstOrNull()?.url
                }

                // Handle missing chapters/intro/outro for Zen servers
                var intro = streamData.intro
                var outro = streamData.outro
                val chapters = streamData.chapters ?: emptyList()

                if (chapters.isNotEmpty()) {
                    if (intro == null) {
                        // Priority 1: Openings
                        var foundIntro = chapters.find { ch ->
                            val t = ch.title?.lowercase()?.trim() ?: ""
                            t == "opening" || t == "title sequence" || t == "op" ||
                            t.contains("opening") || t.contains("theme") || t.contains(" op") || t.contains("op ")
                        }
                        // Priority 2: Intro
                        if (foundIntro == null) {
                            foundIntro = chapters.find { ch ->
                                val t = ch.title?.lowercase()?.trim() ?: ""
                                t == "intro" || t.contains("intro")
                            }
                        }
                        foundIntro?.let { ch ->
                            intro = to.kuudere.anisuge.data.models.SkipData(ch.resolvedStart, ch.resolvedEnd)
                        }
                    }
                    if (outro == null) {
                        chapters.find { ch ->
                            val t = ch.title?.lowercase()?.trim() ?: ""
                            t.contains("credit") || t.contains("end") || t.contains("ed") ||
                            t.contains("outro") || t.contains("closing") || t.contains("credits") ||
                            t.contains(" ed") || t.contains("ed ")
                        }?.let { ch ->
                            outro = to.kuudere.anisuge.data.models.SkipData(ch.resolvedStart, ch.resolvedEnd)
                        }
                    }
                }

                val finalStreamData = streamData.copy(intro = intro, outro = outro)

                // Download fonts if available
                var localFontsDir: String? = null
                if (!streamData.fonts.isNullOrEmpty()) {
                    _uiState.update { it.copy(loadingMessage = "Downloading fonts...") }
                    localFontsDir = to.kuudere.anisuge.utils.downloadFontsAndGetDir(streamData.fonts) { fontMsg ->
                        _uiState.update { it.copy(loadingMessage = fontMsg) }
                    }
                }

                // Final guard: if offlinePath was set while we were fetching, abort
                if (_uiState.value.offlinePath != null) {
                    println("[WatchVM] loadVideoStream aborted at final step - offlinePath was set")
                    return
                }

                _uiState.update { state ->
                    state.copy(
                        isLoadingVideo = false,
                        loadingMessage = null,
                        streamingData = finalStreamData,
                        availableQualities = qualities,
                        currentQuality = qualities.firstOrNull()?.first ?: "Auto",
                        availableSubtitles = subtitles,
                        currentSubtitleUrl = selectedSubUrl,
                        currentFontsDir = localFontsDir,
                        offlinePath = null
                    )
                }
                println("[WatchVM] Selected sub=$selectedSubUrl, intro=${intro?.start}-${intro?.end}, outro=${outro?.start}-${outro?.end}")
            } else {
                _uiState.update { it.copy(isLoadingVideo = false, loadingMessage = null, offlinePath = null) }
            }
    }

    fun setQuality(quality: String) {
        _uiState.update { it.copy(currentQuality = quality, showSettingsOverlay = false) }
    }

    fun setSubtitle(url: String?, subtitleLang: String? = null) {
        _uiState.update { 
            it.copy(
                currentSubtitleUrl = url, 
                targetSubtitleLang = subtitleLang ?: it.targetSubtitleLang,
                showSettingsOverlay = false
            ) 
        }
    }

    fun setSpeed(speed: Double) {
         _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setServer(server: String) {
        _uiState.update { it.copy(showSettingsOverlay = false) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadVideoStream(server)
        }
    }

    fun changeServerWithState(
        newServer: String, 
        position: Double, 
        targetAudioLang: String?, 
        targetSubtitleLang: String?,
        targetSubtitleLangCode: String? = null
    ) {
        _uiState.update { 
            it.copy(
                savedWatchPosition = position,
                targetLang = targetAudioLang,
                targetSubtitleLang = targetSubtitleLang,
                targetSubtitleLangCode = targetSubtitleLangCode,
                showSettingsOverlay = false
            ) 
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadVideoStream(newServer)
        }
    }

    fun toggleSettingsOverlay(page: SettingsMenuPage? = null) {
        _uiState.update { 
            it.copy(
                showSettingsOverlay = !it.showSettingsOverlay,
                initialSettingsPage = page ?: SettingsMenuPage.MAIN
            ) 
        }
    }

    fun toggleSidePanel(panel: String?) {
        _uiState.update { it.copy(activeSidePanel = if (it.activeSidePanel == panel) null else panel) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun onEpisodeSelected(episodeNumber: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(currentEpisodeNumber = episodeNumber, activeSidePanel = null, didMarkWatched = false, offlinePath = null) }
            fetchEpisodeData(episodeNumber)
        }
    }

    fun saveProgress(currentTime: Double, duration: Double, language: String = "sub") {
        val currState = _uiState.value
        val episodeId = currState.episodeData?.episodeId ?: return
        var server = currState.currentServer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (server == "Zen2") server = "Zen-2"

        if (currentAnimeId.isEmpty() || duration <= 0) return

        viewModelScope.launch {
            infoService.saveProgress(
                animeId = currentAnimeId,
                episodeId = episodeId,
                currentTime = currentTime,
                duration = duration,
                server = server,
                language = language
            )

            // AniList Sync
            val state = _uiState.value
            if (!state.didMarkWatched && duration > 0) {
                val ratio = currentTime / duration
                if (ratio * 100 >= state.syncPercentage) {
                    val anilistId = state.episodeData?.animeInfo?.anilist
                    if (anilistId != null) {
                        _uiState.update { it.copy(didMarkWatched = true) }
                        println("[WatchVM] Progress threshold met (${state.syncPercentage}%). Syncing episode ${state.currentEpisodeNumber} to AniList.")
                        
                        // Fetch token and sync
                        to.kuudere.anisuge.AppComponent.authService.getAniListToken()?.let { token ->
                            val folder = state.episodeData.folder ?: "Watching"
                            val result = to.kuudere.anisuge.AppComponent.aniListService.updateStatus(
                                accessToken = token,
                                anilistId = anilistId,
                                folder = folder,
                                progress = state.currentEpisodeNumber
                            )
                            println("[WatchVM] AniList progress sync result: $result")
                        } ?: println("[WatchVM] Failed to sync progress: No AniList token found.")
                    }
                }
            }
        }
    }

    fun updateWatchlistStatus(folder: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingWatchlist = true) }
            try {
                if (currentAnimeId.isEmpty()) return@launch
                val response = infoService.updateWatchlistStatus(currentAnimeId, folder)
                if (response != null && response.success) {
                    // AniList Sync
                    response.data?.token?.let { token ->
                        response.data.anilist?.let { anilistId ->
                            println("[WatchVM] Triggering AniList sync for $anilistId to $folder")
                            viewModelScope.launch {
                                val syncResult = to.kuudere.anisuge.AppComponent.aniListService.updateStatus(token, anilistId, folder)
                                println("[WatchVM] AniList sync result for $anilistId: $syncResult")
                            }
                        } ?: println("[WatchVM] No anilistId returned for sync")
                    } ?: println("[WatchVM] No token returned for sync")

                    _uiState.update { state ->
                        state.copy(
                            episodeData = state.episodeData?.copy(
                                inWatchlist = folder != "Remove",
                                folder = if (folder == "Remove") null else folder
                            )
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(isUpdatingWatchlist = false) }
            }
        }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { 
            settingsStore.setAutoPlay(enabled)
            syncPreferencesToServer()
        }
    }

    fun setAutoNext(enabled: Boolean) {
        viewModelScope.launch { 
            settingsStore.setAutoNext(enabled)
            syncPreferencesToServer()
        }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        viewModelScope.launch { 
            settingsStore.setAutoSkipIntro(enabled)
            syncPreferencesToServer()
        }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        viewModelScope.launch { 
            settingsStore.setAutoSkipOutro(enabled)
            syncPreferencesToServer()
        }
    }

    private fun syncPreferencesToServer() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                // We fetch first to avoid clobbering settings not managed by the player (like syncPercentage)
                val current = settingsService.getPreferences()
                val basePrefs = current?.preferences ?: to.kuudere.anisuge.data.models.UserPreferences()
                
                val updated = basePrefs.copy(
                    autoPlay = state.autoPlay,
                    autoNext = state.autoNext,
                    skipIntro = state.autoSkipIntro,
                    skipOutro = state.autoSkipOutro,
                    defaultLang = state.defaultLang,
                    syncPercentage = state.syncPercentage
                )
                settingsService.updatePreferences(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
