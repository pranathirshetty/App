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
    val currentServer: String = "zen",
    val streamingData: StreamingData? = null,
    val availableQualities: List<Pair<String, String>> = emptyList(), // Pair(Quality, URL)
    val currentQuality: String = "Auto",
    val availableSubtitles: List<Pair<String, String>> = emptyList(), // Pair(Lang, URL)
    val currentSubtitleUrl: String? = null,
    val currentFontsDir: String? = null,
    val playbackSpeed: Double = 1.0,
    val savedWatchPosition: Double = 0.0,
    val showSettingsOverlay: Boolean = false,
    val initialSettingsPage: SettingsMenuPage? = SettingsMenuPage.MAIN,
    val activeSidePanel: String? = null, // "episodes" or "comments"
    val isFullscreen: Boolean = false
)

class WatchViewModel(
    private val infoService: InfoService
) : ViewModel() {
    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState = _uiState.asStateFlow()

    private var currentAnimeId: String = ""

    fun initialize(animeId: String, episodeNumber: Int) {
        currentAnimeId = animeId
        _uiState.update { it.copy(currentEpisodeNumber = episodeNumber, isLoading = true) }
        fetchEpisodeData(episodeNumber)
    }

    private fun fetchEpisodeData(episodeNumber: Int) {
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

                val serverName = "zen"
                println("[WatchVM] Selected server: $serverName")

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

                val subtitles = mutableListOf<Pair<String, String>>()
                var defaultSubUrl: String? = null
                
                streamData.subtitles?.forEach {
                    // Skip PGS/SUP format — not supported by libmpv sub-add
                    if (it.format?.lowercase() == "sup") return@forEach
                    val label = it.resolvedLang ?: return@forEach
                    val url = it.url ?: return@forEach
                    subtitles.add(label to url)
                    if (it.is_default == true) {
                        defaultSubUrl = url
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
                        currentSubtitleUrl = defaultSubUrl ?: subtitles.firstOrNull()?.second,
                        currentFontsDir = localFontsDir
                    )
                }
                println("[WatchVM] subtitle=${defaultSubUrl ?: subtitles.firstOrNull()?.second}, fontsDir=$localFontsDir, subtitleCount=${subtitles.size}")
            } else {
                _uiState.update { it.copy(isLoadingVideo = false) }
            }
        }
    }

    fun setQuality(quality: String) {
        _uiState.update { it.copy(currentQuality = quality, showSettingsOverlay = false) }
    }

    fun setSubtitle(url: String?) {
        _uiState.update { it.copy(currentSubtitleUrl = url, showSettingsOverlay = false) }
    }

    fun setSpeed(speed: Double) {
         _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setServer(server: String) {
        _uiState.update { it.copy(showSettingsOverlay = false) }
        loadVideoStream(server)
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
