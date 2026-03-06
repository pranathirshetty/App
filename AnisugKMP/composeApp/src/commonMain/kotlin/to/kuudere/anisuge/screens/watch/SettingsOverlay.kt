package to.kuudere.anisuge.screens.watch

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SettingsMenuPage {
    MAIN, SERVER, AUDIO, QUALITY, SUBTITLES, SPEED
}

@Composable
fun SettingsOverlay(
    uiState: WatchUiState,
    servers: List<String>,
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
    var currentPage by remember { mutableStateOf(SettingsMenuPage.MAIN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (uiState.isFullscreen) 0.45f else 0.95f)
                .widthIn(max = 400.dp, min = 300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = false, onClick = {}) // block touch propagation
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 12.dp)
                .animateContentSize(animationSpec = tween(durationMillis = 300)) // smooth size change
        ) {
            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = currentPage,
                transitionSpec = {
                    if (targetState != SettingsMenuPage.MAIN && initialState == SettingsMenuPage.MAIN) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else if (targetState == SettingsMenuPage.MAIN && initialState != SettingsMenuPage.MAIN) {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    } else {
                        fadeIn().togetherWith(fadeOut())
                    }
                }
            ) { page ->
                when (page) {
                    SettingsMenuPage.MAIN -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Drag Handle indicator
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.DarkGray))
                            }

                            // Quality
                            if (uiState.availableQualities.isNotEmpty()) {
                                SettingsMenuItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }, // Icon
                                    title = "Quality",
                                    subtitle = uiState.currentQuality,
                                    onClick = { currentPage = SettingsMenuPage.QUALITY }
                                )
                            }

                            // Playback Speed
                            SettingsMenuItem(
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                                title = "Playback speed",
                                subtitle = if (uiState.playbackSpeed == 1.0) "Normal" else "${uiState.playbackSpeed}x",
                                onClick = { currentPage = SettingsMenuPage.SPEED }
                            )

                            // Captions / Subtitles
                            if (uiState.availableSubtitles.isNotEmpty()) {
                                val currentLabel = uiState.availableSubtitles.firstOrNull { it.second == uiState.currentSubtitleUrl }?.first ?: "Off"
                                SettingsMenuItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.White) },
                                    title = "Captions",
                                    subtitle = currentLabel,
                                    onClick = { currentPage = SettingsMenuPage.SUBTITLES }
                                )
                            }

                            // Audio Track
                            if (audioTracks.isNotEmpty()) {
                                val currentLabel = audioTracks.firstOrNull { it.first == selectedAudioTrack }?.second ?: "Default"
                                SettingsMenuItem(
                                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White) },
                                    title = "Audio Track",
                                    subtitle = currentLabel,
                                    onClick = { currentPage = SettingsMenuPage.AUDIO }
                                )
                            } else {
                                // Fallback cycle
                                SettingsMenuItem(
                                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White) },
                                    title = "Audio Track",
                                    subtitle = "Cycle",
                                    onClick = { onCycleAudio(); onDismiss() }
                                )
                            }

                            // Server
                            if (servers.isNotEmpty()) {
                                SettingsMenuItem(
                                    icon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                                    title = "Server",
                                    subtitle = uiState.currentServer,
                                    onClick = { currentPage = SettingsMenuPage.SERVER }
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    SettingsMenuPage.SERVER -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubMenuHeader("Server") { currentPage = SettingsMenuPage.MAIN }
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                                items(servers) { serverName ->
                                    SubMenuItem(
                                        title = serverName,
                                        isSelected = serverName == uiState.currentServer,
                                        onClick = { onServerSelected(serverName); onDismiss() }
                                    )
                                }
                            }
                        }
                    }
                    SettingsMenuPage.QUALITY -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubMenuHeader("Quality") { currentPage = SettingsMenuPage.MAIN }
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                                items(uiState.availableQualities) { (quality, _) ->
                                    SubMenuItem(
                                        title = quality,
                                        isSelected = quality == uiState.currentQuality,
                                        onClick = { onQualitySelected(quality); onDismiss() }
                                    )
                                }
                            }
                        }
                    }
                    SettingsMenuPage.AUDIO -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubMenuHeader("Audio Track") { currentPage = SettingsMenuPage.MAIN }
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                                items(audioTracks) { (id, label) ->
                                    SubMenuItem(
                                        title = label,
                                        isSelected = id == selectedAudioTrack,
                                        onClick = { onAudioTrackSelected(id); onDismiss() }
                                    )
                                }
                            }
                        }
                    }
                    SettingsMenuPage.SPEED -> {
                        val speeds = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubMenuHeader("Playback speed") { currentPage = SettingsMenuPage.MAIN }
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                                items(speeds) { speed ->
                                    val titleStr = if (speed == 1.0) "Normal" else "${speed}x"
                                    SubMenuItem(
                                        title = titleStr,
                                        isSelected = speed == uiState.playbackSpeed,
                                        onClick = { onSpeedSelected(speed); onDismiss() }
                                    )
                                }
                            }
                        }
                    }
                    SettingsMenuPage.SUBTITLES -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubMenuHeader("Captions") { currentPage = SettingsMenuPage.MAIN }
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                                item {
                                    SubMenuItem(
                                        title = "Off",
                                        isSelected = uiState.currentSubtitleUrl == null,
                                        onClick = { onSubtitleSelected(null); onDismiss() }
                                    )
                                }
                                items(uiState.availableSubtitles) { (lang, url) ->
                                    SubMenuItem(
                                        title = lang,
                                        isSelected = url == uiState.currentSubtitleUrl,
                                        onClick = { onSubtitleSelected(url); onDismiss() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Text(subtitle, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SubMenuHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SubMenuItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1)
    }
}
