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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class DownloadTask(
    val id: String, // animeId_epNum
    val animeId: String,
    val title: String,
    val episodeNumber: Int,
    val coverImage: String? = null,
    val progress: Float = 0f,
    val status: String = "Queued", // Queued, Downloading, Finished, Failed, Paused
    val downloadSpeed: String = "",
    val eta: String = "",
    val localPath: String? = null,
    val isPaused: Boolean = false,
    @kotlinx.serialization.Transient internal var job: Job? = null
)

object DownloadManager {
    val tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = AppComponent.httpClient
    private val infoService = AppComponent.infoService
    
    private val persistenceFile = "${getDownloadsDirectory()}/tasks.json".toPath()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        loadTasks()
    }

    private fun loadTasks() {
        scope.launch {
            try {
                if (FileSystem.SYSTEM.exists(persistenceFile)) {
                    val content = FileSystem.SYSTEM.read(persistenceFile) { readUtf8() }
                    val loaded = json.decodeFromString<List<DownloadTask>>(content)
                    // Reset transient states
                    val sanitized = loaded.map { 
                        if (it.status != "Finished" && !it.status.startsWith("Failed")) {
                            it.copy(status = "Paused", isPaused = true)
                        } else it
                    }
                    tasks.value = sanitized
                }
            } catch (e: Exception) {
                println("Failed to load tasks: ${e.message}")
            }
        }
    }

    private fun saveTasks() {
        scope.launch {
            try {
                val content = json.encodeToString(tasks.value)
                FileSystem.SYSTEM.write(persistenceFile) { writeUtf8(content) }
            } catch (e: Exception) {
                println("Failed to save tasks: ${e.message}")
            }
        }
    }

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
            if (existing.isPaused || existing.status.startsWith("Failed")) resumeDownload(taskId)
            return
        }

        val newTask = DownloadTask(taskId, animeId, title, episodeNumber, coverImage, 0f, "Fetching stream...")
        tasks.update { it + newTask }
        saveTasks()

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
                val sink = FileSystem.SYSTEM.sink(finalPath.toPath()).buffer()
                
                var totalBytesDownloaded = 0L
                var lastMeasureTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                var bytesSinceLastMeasure = 0L

                try {
                    segments.forEachIndexed { index, segmentUrl ->
                        while (tasks.value.find { it.id == taskId }?.isPaused == true) {
                            kotlinx.coroutines.delay(1000)
                            lastMeasureTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        }

                        val segmentBytes = httpClient.get(segmentUrl).readBytes()
                        sink.write(segmentBytes)
                        
                        totalBytesDownloaded += segmentBytes.size
                        bytesSinceLastMeasure += segmentBytes.size
                        
                        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        val diff = now - lastMeasureTime
                        
                        var speedText = ""
                        var etaText = ""
                        
                        if (diff >= 1000) {
                            val speed = (bytesSinceLastMeasure.toDouble() / (diff / 1000.0)) // bytes/sec
                            speedText = formatSpeed(speed)
                            val avgSegSize = totalBytesDownloaded.toDouble() / (index + 1)
                            val remainingSegs = segments.size - (index + 1)
                            val remainingBytes = remainingSegs * avgSegSize
                            val etaSecs = if (speed > 0) (remainingBytes / speed).toInt() else 0
                            etaText = formatEta(etaSecs)
                            
                            lastMeasureTime = now
                            bytesSinceLastMeasure = 0
                        }

                        val progress = (index + 1).toFloat() / segments.size
                        updateTask(taskId) { it.copy(
                            status = "Downloading stream: ${(progress * 100).toInt()}%",
                            progress = progress,
                            downloadSpeed = speedText.ifEmpty { it.downloadSpeed },
                            eta = etaText.ifEmpty { it.eta }
                        ) }
                    }
                    updateTask(taskId) { it.copy(status = "Finalizing MKV container...", progress = 1f, downloadSpeed = "", eta = "") }
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

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024)
            else -> String.format("%.0f B/s", bytesPerSec)
        }
    }

    private fun formatEta(seconds: Int): String {
        return when {
            seconds >= 3600 -> String.format("%dh %dm left", seconds / 3600, (seconds % 3600) / 60)
            seconds >= 60 -> String.format("%dm %ds left", seconds / 60, seconds % 60)
            else -> String.format("%ds left", seconds)
        }
    }

    fun pauseDownload(id: String) {
        updateTask(id) { it.copy(isPaused = true, status = "Paused") }
        saveTasks()
    }

    fun resumeDownload(id: String) {
        updateTask(id) { it.copy(isPaused = false, status = "Resuming...") }
        // Simple resume for now. For full resume, need to re-execute download loop.
    }

    fun cancelDownload(id: String) {
        tasks.value.find { it.id == id }?.job?.cancel()
        tasks.update { it.filterNot { t -> t.id == id } }
        saveTasks()
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
        val task = tasks.value.find { it.id == id }
        if (task?.status == "Finished" || task?.status?.startsWith("Failed") == true) {
            saveTasks()
        }
    }
}
