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
import to.kuudere.anisuge.ui.WatchlistBottomSheet
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
    var showEpisodes by remember { mutableStateOf(true) }

    // Auto-load episodes since it's the default tab
    LaunchedEffect(state.details) {
        if (state.details != null && state.episodes.isEmpty() && !state.isLoadingEpisodes) {
            viewModel.loadEpisodes()
        }
    }

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
                    // Blur Background — use cover as fallback with stronger blur
                    val hasBanner = !anime.banner.isNullOrEmpty()
                    val bgImage = if (hasBanner) anime.banner else anime.cover
                    AsyncImage(
                        model = bgImage,
                        contentDescription = "Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .blur(if (hasBanner) 20.dp else 40.dp)
                            .alpha(if (hasBanner) 0.6f else 0.8f)
                    )
                    // Background Gradient
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.2f),
                                    0.5f to Color.Black.copy(alpha = 0.6f),
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
                     text = stripHtmlTags(anime.description ?: ""),
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
                model = anime.banner?.ifEmpty { anime.cover },
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
                            Text(anime.views ?: "0", color = Color.Gray, fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Default.Favorite, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(anime.likes ?: "0", color = Color.Gray, fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            Box(
                                Modifier
                                    .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(anime.status ?: "Unknown", color = Color.Green, fontSize = 12.sp)
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
                            text = stripHtmlTags(anime.description ?: ""),
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WatchlistDropdownIcon(
    state: AnimeInfoUiState,
    onUpdate: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    IconButton(onClick = { showSheet = true }) {
        if (state.isUpdatingWatchlist) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(
                if (state.inWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Watchlist",
                tint = Color.White
            )
        }
    }

    if (showSheet) {
        WatchlistBottomSheet(
            currentFolder = state.folder,
            onSelect = { option ->
                showSheet = false
                onUpdate(option)
            },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun WatchlistDropdownButton(
    state: AnimeInfoUiState,
    onUpdate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val buttonText = when {
        state.isUpdatingWatchlist -> "Updating..."
        state.inWatchlist -> (state.folder ?: "In Watchlist")
        else -> "Add to Watchlist"
    }

    Button(
        onClick = { showSheet = true },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (state.isUpdatingWatchlist) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
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

    if (showSheet) {
        WatchlistBottomSheet(
            currentFolder = state.folder,
            onSelect = { option ->
                showSheet = false
                onUpdate(option)
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeListSection(
    state: AnimeInfoUiState,
    onWatchEpisode: (Int) -> Unit
) {
    if (state.isLoadingEpisodes) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        return
    }

    if (state.episodes.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("No episodes found", color = Color.Gray)
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var currentPageStart by remember { mutableStateOf(1) }
    val episodesPerPage = 100
    val totalEpisodes = state.episodes.size
    val pageGroups = (1..totalEpisodes step episodesPerPage).toList()

    Column(Modifier.fillMaxWidth()) {
        // Season header + Group selector
        var showGroupSheet by remember { mutableStateOf(false) }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
                .clickable { showGroupSheet = true }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val end = (currentPageStart + episodesPerPage - 1).coerceAtMost(totalEpisodes)
                    Text(
                        "Episodes $currentPageStart - $end",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$totalEpisodes episodes / Released ${state.details?.subbedCount ?: totalEpisodes}",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
        }

        if (showGroupSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGroupSheet = false },
                containerColor = Color(0xFF111111),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                dragHandle = {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Select Episode Range",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    pageGroups.forEach { start ->
                        val end = (start + episodesPerPage - 1).coerceAtMost(totalEpisodes)
                        val isSelected = start == currentPageStart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    currentPageStart = start
                                    showGroupSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Episodes $start - $end",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            placeholder = { Text("Search episode...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.1f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // Filtered episode cards
        val filtered = state.episodes.filter {
            val num = it.number
            val inRange = num >= currentPageStart && num < currentPageStart + episodesPerPage
            if (searchQuery.isEmpty()) return@filter inRange
            val matchNum = num.toString().contains(searchQuery)
            val titleMatches = it.titles?.filterNotNull()?.firstOrNull()?.contains(searchQuery, ignoreCase = true) == true
            inRange && (matchNum || titleMatches)
        }.sortedBy { it.number }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail — 20:9 aspect ratio
        Box(
            Modifier
                .weight(0.45f)
                .aspectRatio(20f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = "Episode ${episode.number}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = episode.number.toString().padStart(2, '0'),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

        // Right side — title, subtitle, download icon top-right
        Box(Modifier.weight(0.55f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val title = episode.titles?.filterNotNull()?.firstOrNull()
                Text(
                    text = title ?: "Episode ${episode.number}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 32.dp)
                )
                Text(
                    text = "Episode ${episode.number}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            // Download icon top-right
            IconButton(
                onClick = { /* Download */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
