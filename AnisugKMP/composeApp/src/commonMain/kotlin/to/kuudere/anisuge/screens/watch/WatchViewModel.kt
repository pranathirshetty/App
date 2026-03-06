package to.kuudere.anisuge.screens.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.EpisodeDataResponse
import to.kuudere.anisuge.data.models.EpisodeLink
import to.kuudere.anisuge.data.models.StreamingData
import to.kuudere.anisuge.data.services.InfoService
import to.kuudere.anisuge.player.VideoPlayerConfig
import to.kuudere.anisuge.player.VideoPlayerState

import to.kuudere.anisuge.screens.watch.SettingsMenuPage

data class WatchUiState(
    val isLoading: Boolean = true,
    val isLoadingVideo: Boolean = false,
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
    val targetSubtitleLangCode: String? = null
)

class WatchViewModel(
    private val infoService: InfoService
) : ViewModel() {
    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState = _uiState.asStateFlow()

    private var currentAnimeId: String = ""

    fun initialize(animeId: String, episodeNumber: Int, server: String? = null, lang: String? = null) {
        currentAnimeId = animeId
        _uiState.update { it.copy(currentEpisodeNumber = episodeNumber, isLoading = true) }
        fetchEpisodeData(episodeNumber, server, lang)
    }

    private fun fetchEpisodeData(episodeNumber: Int, reqServer: String? = null, reqLang: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val data = infoService.getEpisodes(currentAnimeId, episodeNumber)
            
            if (data != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        episodeData = data,
                        savedWatchPosition = data.current?.toDouble() ?: 0.0
                    )
                }

                data.animeInfo?.anilist?.let { anilistId ->
                    fetchThumbnails(anilistId)
                }

                val fallbackPriority = listOf("zen2", "zen", "hiya", "hiya-dub")
                var targetServerName: String? = null
                var finalLang: String? = reqLang
                val links = data.episodeLinks ?: emptyList()
                
                if (reqServer != null && reqLang != null) {
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
                
                if (targetServerName == null) {
                    val fallbackLang = reqLang ?: "sub"
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

                val serverName = targetServerName ?: "zen2"
                println("[WatchVM] Selected server: $serverName (requested: $reqServer, lang: $reqLang, targetLang: $finalLang)")
                
                _uiState.update { it.copy(targetLang = finalLang) }

                loadVideoStream(serverName)
            } else {
                _uiState.update { it.copy(isLoading = false) } // Add error state in real app
            }
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

    fun loadVideoStream(serverName: String) {
        val currState = _uiState.value
        val anilistId = currState.episodeData?.animeInfo?.anilist ?: return
        val episodeNum = currState.currentEpisodeNumber

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVideo = true, currentServer = serverName) }

            val response = infoService.getVideoStream(anilistId, episodeNum, serverName)
            
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

                val subtitles = mutableListOf<to.kuudere.anisuge.data.models.SubtitleData>()
                var defaultSubUrl: String? = null
                var matchedTargetSubUrl: String? = null
                var partialMatchedTargetSubUrl: String? = null
                
                streamData.subtitles?.forEach { subData ->
                    // Skip PGS/SUP format — not supported by libmpv sub-add
                    if (subData.format?.lowercase() == "sup") return@forEach
                    val url = subData.url ?: return@forEach
                    subtitles.add(subData)
                    if (subData.is_default == true) {
                        defaultSubUrl = url
                    }
                    val label = subData.title ?: subData.resolvedLang ?: ""
                    val langCode = subData.language ?: subData.lang ?: ""
                    
                    if (currState.targetSubtitleLang != null) {
                        if (label.equals(currState.targetSubtitleLang, ignoreCase = true) || 
                            langCode.equals(currState.targetSubtitleLangCode, ignoreCase = true)) {
                            matchedTargetSubUrl = url
                        } else if (
                            label.lowercase().contains(currState.targetSubtitleLang.lowercase()) ||
                            currState.targetSubtitleLang.lowercase().contains(label.lowercase())
                        ) {
                            if (partialMatchedTargetSubUrl == null) partialMatchedTargetSubUrl = url
                        }
                    } else if (currState.targetSubtitleLangCode != null) {
                        if (langCode.equals(currState.targetSubtitleLangCode, ignoreCase = true)) {
                            matchedTargetSubUrl = url
                        }
                    }
                }

                // Download fonts if available
                var localFontsDir: String? = null
                if (!streamData.fonts.isNullOrEmpty()) {
                    localFontsDir = to.kuudere.anisuge.utils.downloadFontsAndGetDir(streamData.fonts)
                }

                _uiState.update { state ->
                    state.copy(
                        isLoadingVideo = false,
                        streamingData = streamData,
                        availableQualities = qualities,
                        currentQuality = qualities.firstOrNull()?.first ?: "Auto",
                        availableSubtitles = subtitles,
                        currentSubtitleUrl = matchedTargetSubUrl ?: partialMatchedTargetSubUrl ?: defaultSubUrl ?: subtitles.firstOrNull()?.url,
                        currentFontsDir = localFontsDir
                    )
                }
                println("[WatchVM] subtitle=${defaultSubUrl ?: subtitles.firstOrNull()?.url}, fontsDir=$localFontsDir, subtitleCount=${subtitles.size}")
            } else {
                _uiState.update { it.copy(isLoadingVideo = false) }
            }
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
        loadVideoStream(server)
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
        loadVideoStream(newServer)
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
        _uiState.update { it.copy(currentEpisodeNumber = episodeNumber, activeSidePanel = null) }
        fetchEpisodeData(episodeNumber)
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
        }
    }
}
