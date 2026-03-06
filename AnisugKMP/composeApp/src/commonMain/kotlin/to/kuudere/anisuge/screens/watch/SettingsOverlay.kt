package to.kuudere.anisuge.screens.watch

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import to.kuudere.anisuge.data.models.EpisodeLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlay(
    uiState: WatchUiState,
    allLinks: List<EpisodeLink>,
    onDismiss: () -> Unit,
    onQualitySelected: (String) -> Unit,
    onSubtitleSelected: (String?) -> Unit,
    onServerSelected: (String) -> Unit,
    onSpeedSelected: (Double) -> Unit,
    onCycleAudio: () -> Unit,
    audioTracks: List<Pair<Int, String>> = emptyList(),
    selectedAudioTrack: Int? = null,
    onAudioTrackSelected: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (uiState.isFullscreen) 0.5f else 1f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFF1E1E1E))
                .clickable(enabled = false, onClick = {}) // block touch propagation
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                if (allLinks.isNotEmpty()) {
                    item {
                        Text("Server", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(allLinks) { link ->
                        val isSelected = link.serverName == uiState.currentServer
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onServerSelected(link.serverName ?: "zen") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${link.serverName} (${link.dataType})",
                                color = if (isSelected) Color.Red else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (audioTracks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Audio Track", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(audioTracks) { (id, label) ->
                        val isSelected = id == selectedAudioTrack
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onAudioTrackSelected(id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.Red else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Audio Track", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCycleAudio() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Cycle Audio Track",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (uiState.availableQualities.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Quality", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(uiState.availableQualities) { (quality, _) ->
                        val isSelected = quality == uiState.currentQuality
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelected(quality) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                quality,
                                color = if (isSelected) Color.Red else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (uiState.availableSubtitles.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Subtitles", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    val noneSelected = uiState.currentSubtitleUrl == null
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSubtitleSelected(null) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "None",
                                color = if (noneSelected) Color.Red else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    items(uiState.availableSubtitles) { (lang, url) ->
                        val isSelected = url == uiState.currentSubtitleUrl
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSubtitleSelected(url) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                lang,
                                color = if (isSelected) Color.Red else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } // end LazyColumn
        } // end Column
    } // end Inner Box
} // end Outer Box
} // end SettingsOverlay
