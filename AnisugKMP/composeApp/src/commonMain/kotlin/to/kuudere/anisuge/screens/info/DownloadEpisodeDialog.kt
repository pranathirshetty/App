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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadEpisodeDialog(
    animeId: String,
    episodeId: String,
    episodeNumber: Int,
    anilistId: Int,
    infoService: InfoService,
    onDismiss: () -> Unit,
    onStartDownload: (server: String, subLang: String?, audioLang: String?, downloadFonts: Boolean) -> Unit
) {
    var selectedServer by remember { mutableStateOf("zen2") }
    var selectedSubLang by remember { mutableStateOf<String?>("English") }
    var selectedAudioLang by remember { mutableStateOf<String?>("sub") } // 'sub' or 'dub'
    
    var availableSubtitles by remember { mutableStateOf<List<String>>(listOf("All", "English")) }
    var isLoadingSubs by remember { mutableStateOf(false) }

    val downloadTasks by to.kuudere.anisuge.utils.DownloadManager.tasks.collectAsState()
    val currentTask = downloadTasks.find { it.animeId == animeId && it.episodeNumber == episodeNumber }

    LaunchedEffect(selectedServer) {
        isLoadingSubs = true
        try {
            val apiServer = if (selectedServer == "zen2") "zen-2" else selectedServer
            val response = infoService.getVideoStream(anilistId, episodeNumber, apiServer)
            val streamData = response?.directLink?.data ?: response?.data
            val subs = streamData?.subtitles?.mapNotNull { it.resolvedLang }?.distinct() ?: emptyList()
            availableSubtitles = listOf("All") + subs
            if (selectedSubLang !in availableSubtitles) {
                selectedSubLang = if ("English" in availableSubtitles) "English" else availableSubtitles.getOrNull(1) ?: "All"
            }
        } catch (e: Exception) {
            println("Failed to fetch subs for $selectedServer: ${e.message}")
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
            if (selectedServer.startsWith("zen")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audio Track (Zen)", color = Color.Gray, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("sub" to "Subbed", "dub" to "Dubbed").forEach { (value, label) ->
                            val isSelected = selectedAudioLang == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF222222))
                                    .clickable { selectedAudioLang = value }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
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
                        text = currentTask.status,
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

            Button(
                onClick = {
                    if (currentTask == null || currentTask.status.startsWith("Failed")) {
                        onStartDownload(selectedServer, selectedSubLang, selectedAudioLang, true)
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        currentTask == null -> "Start Download"
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
