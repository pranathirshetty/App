package to.kuudere.anisuge.screens.info

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import to.kuudere.anisuge.data.models.AnimeDetails
import to.kuudere.anisuge.data.models.EpisodeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeInfoScreen(
    animeId: String,
    viewModel: AnimeInfoViewModel,
    onBack: () -> Unit,
    onWatchEpisode: (String, String, Int) -> Unit,
    onGenreClick: (String) -> Unit = {}
) {
    LaunchedEffect(animeId) {
        viewModel.loadAnimeInfo(animeId)
    }

    val state by viewModel.uiState.collectAsState()
    var showEpisodes by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF4444), strokeWidth = 3.dp)
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Unknown error", color = Color.White)
            }
        } else if (state.details != null) {
            val anime = state.details!!
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isDesktop = maxWidth >= 1024.dp
                if (isDesktop) {
                    DesktopLayout(
                        anime = anime,
                        state = state,
                        onWatchlistUpdate = { viewModel.updateWatchlist(it) },
                        onWatchNow = { onWatchEpisode(anime.id, "sub", 1) },
                        onGenreClick = onGenreClick
                    )
                    // Top Bar Back Button overlay
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .statusBarsPadding()
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                } else {
                    MobileLayout(
                        anime = anime,
                        state = state,
                        showEpisodes = showEpisodes,
                        onToggleEpisodes = {
                            showEpisodes = it
                            if (it && state.episodes.isEmpty()) {
                                viewModel.loadEpisodes()
                            }
                        },
                        onBack = onBack,
                        onWatchlistUpdate = { viewModel.updateWatchlist(it) },
                        onWatchNow = { onWatchEpisode(anime.id, "sub", 1) },
                        onWatchEpisode = { epNum -> onWatchEpisode(anime.id, "sub", epNum) },
                        onGenreClick = onGenreClick
                    )
                }
            }
        }
    }
}

private fun stripHtmlTags(htmlContent: String): String {
    return htmlContent.replace(Regex("<.*?>"), "").replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'").replace("<br>", "\n").replace("<br/>", "\n")
}

@Composable
private fun MobileLayout(
    anime: AnimeDetails,
    state: AnimeInfoUiState,
    showEpisodes: Boolean,
    onToggleEpisodes: (Boolean) -> Unit,
    onBack: () -> Unit,
    onWatchlistUpdate: (String) -> Unit,
    onWatchNow: () -> Unit,
    onWatchEpisode: (Int) -> Unit,
    onGenreClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Column(Modifier.fillMaxWidth()) {
                Box {
                    // Blur Background
                    AsyncImage(
                        model = anime.banner.ifEmpty { anime.cover },
                        contentDescription = "Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .blur(24.dp)
                            .alpha(0.6f)
                    )
                    // Background Gradient
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.1f),
                                    0.5f to Color.Black.copy(alpha = 0.5f),
                                    1.0f to Color.Black
                                )
                            )
                    )
                    // Top Bar for Mobile positioned over the image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* Share */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = { /* Download */ }) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                            }
                            WatchlistDropdownIcon(
                                state = state,
                                onUpdate = onWatchlistUpdate
                            )
                        }
                    }
                }
                
                // Lift the top details block significantly into the header
                Box(Modifier.offset(y = (-150).dp)) {
                    Column {
             
             // Top details block
             Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
                 // Poster
                 AsyncImage(
                    model = anime.cover,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(130.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(8.dp))
                 )
                 Spacer(Modifier.width(16.dp))
                 // Right Details
                 Column(Modifier.weight(1f)) {
                     // Title
                     Text(
                         text = anime.title,
                         color = Color.White,
                         fontSize = 24.sp,
                         fontWeight = FontWeight.Bold,
                         maxLines = 2,
                         overflow = TextOverflow.Ellipsis
                     )
                     Spacer(Modifier.height(8.dp))
                     // Stars
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         repeat(4) {
                             Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                         }
                         Icon(Icons.Default.StarBorder, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                     }
                     Spacer(Modifier.height(8.dp))
                     // Duration - Genres
                     Text(
                         text = "${anime.status} • ${anime.genres?.take(2)?.joinToString(" / ") ?: ""}",
                         color = Color.Gray,
                         fontSize = 13.sp
                     )
                     Spacer(Modifier.height(16.dp))

                     // Play Movie button
                     Box(
                         Modifier
                             .clip(RoundedCornerShape(8.dp))
                             .background(Color(0xFF222222))
                             .clickable { onWatchNow() }
                             .padding(horizontal = 16.dp, vertical = 8.dp)
                     ) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                             Spacer(Modifier.width(8.dp))
                             Text("Watch Now", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                         }
                     }
                 }
             }

             Spacer(Modifier.height(16.dp))

             // Storyline
             Column(Modifier.padding(horizontal = 16.dp)) {
                 var isExpanded by remember { mutableStateOf(false) }

                 Text("Storyline", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Normal)
                 Spacer(Modifier.height(12.dp))
                 Text(
                     text = stripHtmlTags(anime.description),
                     color = Color.Gray,
                     fontSize = 14.sp,
                     lineHeight = 22.sp,
                     maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                     overflow = TextOverflow.Ellipsis,
                     modifier = Modifier.clickable { isExpanded = !isExpanded }
                 )
             }

             Spacer(Modifier.height(16.dp))

             // Custom Tabs
             Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                 Text(
                     "Episodes",
                     color = if (showEpisodes) Color.White else Color.Gray,
                     fontSize = 20.sp,
                     fontWeight = FontWeight.Normal,
                     modifier = Modifier.clickable { onToggleEpisodes(true) }
                 )
                 Text(
                     "Details",
                     color = if (!showEpisodes) Color.White else Color.Gray,
                     fontSize = 20.sp,
                     fontWeight = FontWeight.Normal,
                     modifier = Modifier.clickable { onToggleEpisodes(false) }
                 )
             }

             Spacer(Modifier.height(16.dp))

             if (showEpisodes) {
                 Box(Modifier.padding(horizontal = 16.dp)) {
                     EpisodeListSection(state, onWatchEpisode)
                 }
             } else {
                 Column(Modifier.padding(horizontal = 16.dp)) {
                     if (!anime.genres.isNullOrEmpty()) {
                         Text("Genres", color = Color.White, fontSize = 16.sp)
                         Spacer(Modifier.height(8.dp))
                         @OptIn(ExperimentalLayoutApi::class)
                         FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                             anime.genres.forEach { genre ->
                                 Box(
                                     Modifier
                                         .clip(RoundedCornerShape(8.dp))
                                         .background(Color(0xFF222222))
                                         .clickable { onGenreClick(genre) }
                                         .padding(horizontal = 12.dp, vertical = 6.dp)
                                 ) {
                                     Text(genre, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                 }
                             }
                         }
                         Spacer(Modifier.height(24.dp))
                     }

                     if (!anime.studios.isNullOrEmpty()) {
                         Text("Studios", color = Color.White, fontSize = 16.sp)
                         Spacer(Modifier.height(8.dp))
                         @OptIn(ExperimentalLayoutApi::class)
                         FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                             anime.studios.forEach { studio ->
                                 Box(
                                     Modifier
                                         .clip(RoundedCornerShape(8.dp))
                                         .background(Color(0xFF222222))
                                         .padding(horizontal = 12.dp, vertical = 6.dp)
                                 ) {
                                     Text(studio, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                 }
                             }
                         }
                     }
                 }
             }
             Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopLayout(
    anime: AnimeDetails,
    state: AnimeInfoUiState,
    onWatchlistUpdate: (String) -> Unit,
    onWatchNow: () -> Unit,
    onGenreClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Box(Modifier.fillMaxWidth().height(500.dp)) {
            AsyncImage(
                model = anime.banner.ifEmpty { anime.cover },
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.8f to Color.Black.copy(alpha = 0.9f)
                    )
                )
            )

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = 100.dp)
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(start = 24.dp, top = 48.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = anime.cover,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(200.dp).height(280.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            anime.title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RemoveRedEye, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(anime.views, color = Color.Gray, fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Default.Favorite, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(anime.likes, color = Color.Gray, fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            Box(
                                Modifier
                                    .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(anime.status, color = Color.Green, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onWatchNow,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Watch Now")
                            }
                            Spacer(Modifier.width(12.dp))
                            WatchlistDropdownButton(
                                state = state,
                                onUpdate = onWatchlistUpdate,
                                modifier = Modifier.width(180.dp).height(48.dp)
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stripHtmlTags(anime.description),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Genres", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    anime.genres?.forEach { genre ->
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF1E1E1E))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .clickable { onGenreClick(genre) }
                                        ) {
                                            Text(genre, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Studios", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    anime.studios?.forEach { studio ->
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF1E1E1E))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(studio, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistDropdownIcon(
    state: AnimeInfoUiState,
    onUpdate: (String) -> Unit
) {
    val options = listOf("Watching", "On Hold", "Plan To Watch", "Dropped", "Completed", "Remove")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        IconButton(
            onClick = {}, // Handled by ExposedDropdownMenuBox
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        ) {
            if (state.isUpdatingWatchlist) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    if (state.inWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Watchlist",
                    tint = Color.White
                )
            }
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        expanded = false
                        onUpdate(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistDropdownButton(
    state: AnimeInfoUiState,
    onUpdate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Watching", "On Hold", "Plan To Watch", "Dropped", "Completed", "Remove")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        val buttonText = if (state.isUpdatingWatchlist) "Updating..." else if (state.inWatchlist) (state.folder ?: "In Watchlist") else "Add to Watchlist"
        
        Button(
            onClick = {}, // Handled by ExposedDropdownMenuBox
            modifier = Modifier.fillMaxSize().menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            if (state.isUpdatingWatchlist) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Icon(
                    if (state.inWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(buttonText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.Black) },
                    onClick = {
                        expanded = false
                        onUpdate(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun EpisodeListSection(
    state: AnimeInfoUiState,
    onWatchEpisode: (Int) -> Unit
) {
    if (state.isLoadingEpisodes) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF4444))
        }
        return
    }

    if (state.episodes.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("No episodes found", color = Color.White)
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var currentPageStart by remember { mutableStateOf(1) }
    val episodesPerPage = 100
    
    val totalEpisodes = state.episodes.size
    val pageGroups = (1..totalEpisodes step episodesPerPage).toList()
    
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Dropdown for Groups
            var groupsExpanded by remember { mutableStateOf(false) }
            Box {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { groupsExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val end = currentPageStart + episodesPerPage - 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$currentPageStart - $end", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                    }
                }
                DropdownMenu(
                    expanded = groupsExpanded,
                    onDismissRequest = { groupsExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    pageGroups.forEach { start ->
                        DropdownMenuItem(
                            text = { Text("$start - ${start + episodesPerPage - 1}", color = Color.White) },
                            onClick = {
                                currentPageStart = start
                                groupsExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).height(50.dp),
                placeholder = { Text("Search episode...", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }

        Spacer(Modifier.height(16.dp))

        val filtered = state.episodes.filter {
            val num = it.number
            val inRange = num >= currentPageStart && num < currentPageStart + episodesPerPage
            if (searchQuery.isEmpty()) return@filter inRange
            
            val matchNum = num.toString().contains(searchQuery)
            val titleMatches = it.titles?.firstOrNull()?.contains(searchQuery, ignoreCase = true) == true
            inRange && (matchNum || titleMatches)
        }.sortedBy { it.number }

        Column {
            filtered.forEach { episode ->
                EpisodeItemRow(
                    episode = episode,
                    thumbnail = state.thumbnails[episode.number.toString()],
                    onClick = { onWatchEpisode(episode.number) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeItemRow(episode: EpisodeItem, thumbnail: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(100.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }
            Icon(Icons.Default.PlayCircleOutline, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Episode ${episode.number}", color = Color.White, fontWeight = FontWeight.Bold)
            val subTitle = episode.titles?.firstOrNull() ?: ""
            if (subTitle.isNotEmpty() && subTitle != "Episode ${episode.number}") {
                Text(subTitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
