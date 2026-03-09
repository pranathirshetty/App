package to.kuudere.anisuge.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import to.kuudere.anisuge.AppComponent
import to.kuudere.anisuge.data.models.StreamingData

import kotlinx.coroutines.Job

data class DownloadTask(
    val id: String, // animeId_epNum
    val animeId: String,
    val title: String,
    val episodeNumber: Int,
    val coverImage: String? = null,
    val progress: Float = 0f,
    val status: String = "Queued", // Queued, Downloading, Finished, Failed, Paused
    val localPath: String? = null,
    val isPaused: Boolean = false,
    @kotlin.jvm.Transient internal var job: Job? = null
)

object DownloadManager {
    val tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = AppComponent.httpClient
    private val infoService = AppComponent.infoService

    fun startDownload(
        animeId: String,
        anilistId: Int,
        episodeNumber: Int,
        title: String,
        coverImage: String?,
        server: String,
        subLang: String?,
        audioLang: String?,
        downloadFonts: Boolean
    ) {
        val taskId = "${animeId}_$episodeNumber"
        val existing = tasks.value.find { it.id == taskId }
        if (existing != null) {
            if (existing.status == "Paused") resumeDownload(taskId)
            return
        }

        val newTask = DownloadTask(taskId, animeId, title, episodeNumber, coverImage, 0f, "Fetching stream...")
        tasks.update { it + newTask }

        executeDownload(newTask, anilistId, server, subLang, audioLang, downloadFonts)
    }

    private fun executeDownload(
        task: DownloadTask,
        anilistId: Int,
        server: String,
        subLang: String?,
        audioLang: String?,
        downloadFonts: Boolean
    ) {
        val taskId = task.id
        val job = scope.launch {
            try {
                // 1. Fetch stream URL
                val apiServer = if (server == "zen2") "zen-2" else server
                val response = infoService.getVideoStream(anilistId, task.episodeNumber, apiServer)
                val streamData = response?.directLink?.data ?: response?.data
                
                if (streamData == null) {
                    updateTask(taskId) { it.copy(status = "Failed: No stream") }
                    return@launch
                }

                // Strictly use M3U8 as requested
                val m3u8Url = streamData.m3u8_url
                
                if (m3u8Url == null) {
                    updateTask(taskId) { it.copy(status = "Failed: No M3U8 URL") }
                    return@launch
                }

                // Create folder
                val baseDir = getDownloadsDirectory()
                val animeDir = "$baseDir/${task.animeId.replace("[^A-Za-z0-9]".toRegex(), "_")}"
                val epDir = "$animeDir/ep_${task.episodeNumber}"
                FileSystem.SYSTEM.createDirectories(epDir.toPath())

                // 2. Download Fonts
                if (!streamData.fonts.isNullOrEmpty()) {
                    updateTask(taskId) { it.copy(status = "Downloading fonts...") }
                    streamData.fonts.forEach { font ->
                        if (font.url != null && font.name != null) {
                            try {
                                val fontBytes = httpClient.get(font.url).readBytes()
                                val fontFile = "$epDir/${font.name}"
                                FileSystem.SYSTEM.write(fontFile.toPath()) { write(fontBytes) }
                            } catch (e: Exception) { }
                        }
                    }
                }

                // 3. Download Subtitles
                val subsToDownload = if (subLang == "All") {
                    streamData.subtitles ?: emptyList()
                } else {
                    val target = streamData.subtitles?.find { it.resolvedLang?.contains(subLang ?: "English", true) == true }
                    if (target != null) listOf(target) else emptyList()
                }

                if (subsToDownload.isNotEmpty()) {
                    updateTask(taskId) { it.copy(status = "Downloading subtitles...") }
                    subsToDownload.forEach { sub ->
                        if (sub.url != null) {
                            try {
                                val subBytes = httpClient.get(sub.url).readBytes()
                                val label = sub.resolvedLang?.replace("[^A-Za-z0-9]".toRegex(), "_") ?: "unknown"
                                val format = if (sub.url.contains(".vtt")) "vtt" else if (sub.url.contains(".srt")) "srt" else "ass"
                                val fileName = if (subsToDownload.size == 1 || label.contains("English", true)) "subtitle.$format" else "subtitle_$label.$format"
                                val subFile = "$epDir/$fileName"
                                FileSystem.SYSTEM.write(subFile.toPath()) { write(subBytes) }
                            } catch (e: Exception) { }
                        }
                    }
                }

                // 4. Download Video
                updateTask(taskId) { it.copy(status = "Parsing playlist...") }
                val finalPlaylistUrl = getFinalPlaylistUrl(m3u8Url)
                val playlistContent = httpClient.get(finalPlaylistUrl).bodyAsText()
                val segments = parseSegments(finalPlaylistUrl, playlistContent)
                
                if (segments.isEmpty()) {
                    updateTask(taskId) { it.copy(status = "Failed: No segments") }
                    return@launch
                }

                val finalPath = "$epDir/video.mkv"
                // Check if we already have some segments (Resume support)
                // For simplicity here, we overwrite, but check for cancellation/pause in loop
                val sink = FileSystem.SYSTEM.sink(finalPath.toPath()).buffer()
                
                try {
                    segments.forEachIndexed { index, segmentUrl ->
                        // Check for pause/cancel
                        while (tasks.value.find { it.id == taskId }?.isPaused == true) {
                            kotlinx.coroutines.delay(1000)
                        }

                        val segmentBytes = httpClient.get(segmentUrl).readBytes()
                        sink.write(segmentBytes)
                        
                        val progress = (index + 1).toFloat() / segments.size
                        updateTask(taskId) { it.copy(
                            status = "Downloading stream: ${(progress * 100).toInt()}%",
                            progress = progress
                        ) }
                    }
                    updateTask(taskId) { it.copy(status = "Finalizing MKV container...", progress = 1f) }
                } finally {
                    sink.close()
                }

                updateTask(taskId) { it.copy(status = "Finished", progress = 1f, localPath = finalPath) }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    updateTask(taskId) { it.copy(status = "Failed: ${e.message}") }
                }
            }
        }
        updateTask(taskId) { it.copy(job = job) }
    }

    fun pauseDownload(id: String) {
        updateTask(id) { it.copy(isPaused = true, status = "Paused") }
    }

    fun resumeDownload(id: String) {
        updateTask(id) { it.copy(isPaused = false, status = "Resuming...") }
    }

    fun cancelDownload(id: String) {
        tasks.value.find { it.id == id }?.job?.cancel()
        tasks.update { it.filterNot { t -> t.id == id } }
    }

    fun removeTask(id: String) {
        cancelDownload(id)
    }

    private suspend fun getFinalPlaylistUrl(url: String): String {
        val content = httpClient.get(url).bodyAsText()
        if (content.contains("#EXT-X-STREAM-INF")) {
            val lines = content.lines()
            val variantLine = lines.firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            if (variantLine != null) {
                val base = url.substringBeforeLast("/")
                return if (variantLine.startsWith("http")) variantLine else "$base/$variantLine"
            }
        }
        return url
    }

    private fun parseSegments(playlistUrl: String, content: String): List<String> {
        val base = playlistUrl.substringBeforeLast("/")
        return content.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                if (line.startsWith("http")) line else "$base/$line"
            }
    }

    private fun updateTask(id: String, update: (DownloadTask) -> DownloadTask) {
        tasks.update { list ->
            list.map { if (it.id == id) update(it) else it }
        }
    }
}
