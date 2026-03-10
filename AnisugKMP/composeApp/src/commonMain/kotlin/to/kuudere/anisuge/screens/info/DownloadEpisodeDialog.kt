package to.kuudere.anisuge.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import to.kuudere.anisuge.data.models.WatchServerResponse
import to.kuudere.anisuge.data.services.InfoService
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadEpisodeDialog(
    animeId: String,
    episodeId: String,
    episodeNumber: Int,
    anilistId: Int,
    durationMins: Int,
    infoService: InfoService,
    onDismiss: () -> Unit,
    onStartDownload: (server: String, subLang: String?, audioLang: String?, downloadFonts: Boolean) -> Unit
) {
    var selectedServer by remember { mutableStateOf("zen2") }
    var selectedSubLang by remember { mutableStateOf<String?>("English") }
    var selectedAudioLang by remember { mutableStateOf<String?>("sub") } // 'sub' or 'dub'
    
    var availableSubtitles by remember { mutableStateOf<List<String>>(listOf("All", "English")) }
    var availableAudioTracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoadingSubs by remember { mutableStateOf(false) }
    var estimatedSizeBytes by remember { mutableStateOf(0L) }

    val downloadTasks by to.kuudere.anisuge.utils.DownloadManager.tasks.collectAsState()
    val currentTask = downloadTasks.find { it.animeId == animeId && it.episodeNumber == episodeNumber }

    LaunchedEffect(selectedServer) {
        isLoadingSubs = true
        estimatedSizeBytes = 0L
        try {
            val apiServer = if (selectedServer == "zen2") "zen-2" else selectedServer
            val response = infoService.getVideoStream(anilistId, episodeNumber, apiServer)
            val streamData = response?.directLink?.data ?: response?.data
            
            // 1. Subtitles
            val subs = streamData?.subtitles?.mapNotNull { 
                it.title ?: it.resolvedLang 
            }?.distinct() ?: emptyList()
            availableSubtitles = listOf("All") + subs
            if (selectedSubLang !in availableSubtitles) {
                selectedSubLang = if ("English" in availableSubtitles) "English" else availableSubtitles.getOrNull(1) ?: "All"
            }

            // 2. Audio Tracks and Size Estimation from M3U8
            val m3u8Url = streamData?.m3u8_url
            if (m3u8Url != null) {
                val masterContent = to.kuudere.anisuge.AppComponent.httpClient.get(m3u8Url).bodyAsText()
                val tracks = mutableListOf<Pair<String, String>>()
                var maxBandwidth = 0L

                masterContent.lines().forEach { line ->
                    if (line.startsWith("#EXT-X-MEDIA:TYPE=AUDIO")) {
                        val lang = Regex("LANGUAGE=\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: "unknown"
                        val name = Regex("NAME=\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: lang
                        tracks.add(lang to name)
                    }
                    if (line.startsWith("#EXT-X-STREAM-INF:")) {
                        val bwMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                        val bw = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        if (bw > maxBandwidth) maxBandwidth = bw
                    }
                }
                
                // Estimate size: Bandwidth is bits/sec. 
                // Formula: (bits/sec / 8) * duration_seconds
                // If bandwidth is weirdly low (like 5184 from the curl), it might be kbps.
                val adjustedBps = if (maxBandwidth > 0 && maxBandwidth < 100000) maxBandwidth * 1000 else maxBandwidth
                if (adjustedBps > 0) {
                    estimatedSizeBytes = (adjustedBps / 8) * (durationMins * 60)
                }

                availableAudioTracks = tracks.distinctBy { it.first }
                
                // Set default audio lang
                if (availableAudioTracks.isNotEmpty()) {
                    if (selectedAudioLang == null || availableAudioTracks.none { it.first == selectedAudioLang }) {
                        selectedAudioLang = availableAudioTracks.find { it.first == "jpn" || it.first == "ja" }?.first 
                            ?: availableAudioTracks.first().first
                    }
                }
            } else {
                availableAudioTracks = emptyList()
            }
        } catch (e: Exception) {
            println("Failed to fetch subs/audio for $selectedServer: ${e.message}")
        } finally {
            isLoadingSubs = false
        }
    }

    val servers = listOf("hiya", "hiya-dub", "zen", "zen2")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Download Episode $episodeNumber",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Server Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Server", color = Color.Gray, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    servers.forEach { server ->
                        val isSelected = server == selectedServer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color(0xFF222222))
                                .clickable { 
                                    selectedServer = server 
                                    if (server == "hiya-dub") selectedAudioLang = "dub"
                                    if (server == "hiya") selectedAudioLang = "sub"
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = server.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Audio Selection (Only relevant for Zen servers which embed multiple tracks)
            if (availableAudioTracks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audio Track", color = Color.Gray, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAudioTracks.forEach { (code, name) ->
                            val isSelected = selectedAudioLang == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF222222))
                                    .clickable { selectedAudioLang = code }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Subtitle Language", color = Color.Gray, fontSize = 14.sp)
                if (isLoadingSubs) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else if (availableSubtitles.size <= 1) {
                    Text("No subtitles available", color = Color.Gray, fontSize = 13.sp)
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSubtitles.size) { index ->
                            val sub = availableSubtitles[index]
                            val isSelected = selectedSubLang == sub
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF222222))
                                    .clickable { selectedSubLang = sub }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Options
            /* Removed Download Fonts Switch as it is now strictly automatic in DownloadManager */

            if (currentTask != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentTask.status.startsWith("Downloading") && currentTask.downloadSpeed.isNotEmpty()) {
                            "${currentTask.status.substringBefore(":")}: ${currentTask.downloadSpeed} • ${currentTask.eta}"
                        } else {
                            currentTask.status
                        },
                        color = if (currentTask.status == "Finished") Color.Green else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (currentTask.status != "Finished" && !currentTask.status.startsWith("Failed")) {
                        LinearProgressIndicator(
                            progress = { currentTask.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentTask?.status == "Finished") {
                    Button(
                        onClick = {
                            to.kuudere.anisuge.utils.DownloadManager.removeTask(currentTask.id)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Button(
                    onClick = {
                        if (currentTask == null || currentTask.status.startsWith("Failed")) {
                            onStartDownload(selectedServer, selectedSubLang, selectedAudioLang, true)
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val sizeText = if (estimatedSizeBytes > 0) " (~${formatFileSize(estimatedSizeBytes)})" else ""
                    Text(
                        text = when {
                            currentTask == null -> "Start Download$sizeText"
                            currentTask.status == "Finished" -> "Close"
                            currentTask.status.startsWith("Failed") -> "Retry Download"
                            else -> "Keep Downloading in Background"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.0f MB", bytes.toDouble() / (1024 * 1024))
        else -> String.format("%.0f KB", bytes.toDouble() / 1024)
    }
}
