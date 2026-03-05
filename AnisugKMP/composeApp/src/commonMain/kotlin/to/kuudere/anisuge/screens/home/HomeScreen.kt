package to.kuudere.anisuge.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import coil3.compose.AsyncImage
import to.kuudere.anisuge.data.models.AnimeItem
import to.kuudere.anisuge.data.models.ContinueWatchingItem
import to.kuudere.anisuge.platform.DraggableWindowArea

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (animeId: String) -> Unit,
    onWatchClick: (animeId: String, episode: Int, lang: String) -> Unit,
    onExit: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)), Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF4444), strokeWidth = 3.dp)
        }
        return
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error ?: "Unknown error", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("Retry") }
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        val isDesktop = maxWidth >= 800.dp

        Row(Modifier.fillMaxSize()) {
            if (isDesktop) {
                AnisugSidebar(
                    avatarUrl = state.userProfile?.avatar,
                    onExit = onExit
                )
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.05f)))
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {

                    // ── Hero Carousel ──────────────────────────────────────────────
            if (state.topAiring.isNotEmpty()) {
                HeroCarousel(
                    items = state.topAiring,
                    onAnimeClick = onAnimeClick,
                    onWatchClick = onWatchClick,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Continue Watching ──────────────────────────────────────────
            if (state.continueWatching.isNotEmpty()) {
                SectionHeader(title = "Continue Watching", onViewMore = null)
                ContinueWatchingRow(
                    items = state.continueWatching,
                    onWatchClick = onWatchClick,
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Latest Episodes ────────────────────────────────────────────
            if (state.latestEpisodes.isNotEmpty()) {
                AnimeSection(
                    title = "Latest Episodes",
                    items = state.latestEpisodes,
                    onItemClick = { item -> onWatchClick(item.id, item.epCount ?: 1, "sub") },
                )
            }

            // ── New On Site ────────────────────────────────────────────────
            if (state.newOnSite.isNotEmpty()) {
                AnimeSection(
                    title = "New On App",
                    items = state.newOnSite,
                    onItemClick = { onAnimeClick(it.id) },
                )
            }

            // ── Top Upcoming ───────────────────────────────────────────────
            if (state.topUpcoming.isNotEmpty()) {
                AnimeSection(
                    title = "Top Upcoming",
                    items = state.topUpcoming,
                    onItemClick = { onAnimeClick(it.id) },
                    showViewMore = false,
                )
            }

            Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// ── Hero Carousel ──────────────────────────────────────────────────────────

@Composable
private fun HeroCarousel(
    items: List<AnimeItem>,
    onAnimeClick: (String) -> Unit,
    onWatchClick: (String, Int, String) -> Unit,
) {
    var currentIndex by remember { mutableStateOf(0) }
    val item = items[currentIndex]

    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val screenHeightDp = with(density) { windowSize.height.toDp() }
    val carouselHeight = screenHeightDp * 0.85f

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val isDesktop = maxWidth >= 1024.dp

        Box(
            Modifier
                .fillMaxWidth()
                .height(carouselHeight)
        ) {
            val imageUrl = if (isDesktop) {
                item.bannerUrl ?: item.imageUrl
            } else {
                item.imageUrl.ifEmpty { item.bannerUrl }
            }

            // Background image
            AsyncImage(
                model           = imageUrl,
                contentDescription = item.title,
                contentScale    = ContentScale.Crop,
                modifier        = Modifier.fillMaxSize(),
            )

            // Gradient overlay
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black,
                        ),
                        startY = 0f,
                        endY   = Float.POSITIVE_INFINITY,
                    )
                )
            )

            // Content
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = if (isDesktop) 32.dp else 16.dp,
                        end = if (isDesktop) 32.dp else 16.dp,
                        bottom = if (isDesktop) 40.dp else 24.dp
                    ),
            ) {
                // Spotlight tag
                SpotlightTag(index = currentIndex)
                Spacer(Modifier.height(16.dp))

                // Title
                Text(
                    text       = item.title,
                    color      = Color.White,
                    fontSize   = if (isDesktop) 48.sp else 28.sp,
                    fontWeight = if (isDesktop) FontWeight.ExtraBold else FontWeight.Bold,
                    lineHeight = if (isDesktop) 54.sp else 34.sp,
                    maxLines   = if (isDesktop) 3 else 2,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = if (isDesktop) Modifier.fillMaxWidth(0.55f) else Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Meta tags (Desktop only)
                if (isDesktop) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.type != null) {
                            MetaTag(item.type, Icons.Default.Movie)
                            Spacer(Modifier.width(10.dp))
                        }
                        MetaTag("${item.epCount ?: "?"} Episodes", Icons.Default.Layers)
                        if (item.malScore != null) {
                            Spacer(Modifier.width(10.dp))
                            MalScoreBadge(item.malScore)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Description
                Text(
                    text       = stripHtmlTags(item.description ?: ""),
                    color      = Color.White.copy(alpha = 0.85f),
                    fontSize   = if (isDesktop) 16.sp else 14.sp,
                    lineHeight = if (isDesktop) 24.sp else 21.sp,
                    maxLines   = if (isDesktop) 4 else 3,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = if (isDesktop) Modifier.fillMaxWidth(0.4f) else Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(if (isDesktop) 32.dp else 20.dp))

                // Buttons
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (isDesktop) {
                        Button(
                            onClick = { onWatchClick(item.id, 1, "sub") },
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor   = Color.Black,
                            ),
                            shape   = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Watching", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { onAnimeClick(item.id) },
                            shape   = RoundedCornerShape(12.dp),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Details", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    } else {
                        Button(
                            onClick = { onWatchClick(item.id, 1, "sub") },
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF4444),
                                contentColor   = Color.White,
                            ),
                            shape   = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { onAnimeClick(item.id) },
                            shape   = RoundedCornerShape(8.dp),
                            colors  = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    }
                }
            }

            // Carousel nav arrows (desktop only)
            if (isDesktop) {
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 32.dp, bottom = 40.dp),
                ) {
                    CarouselNavBtn(Icons.AutoMirrored.Filled.ArrowBack) {
                        currentIndex = (currentIndex - 1 + items.size) % items.size
                    }
                    Spacer(Modifier.width(12.dp))
                    CarouselNavBtn(Icons.AutoMirrored.Filled.ArrowForward) {
                        currentIndex = (currentIndex + 1) % items.size
                    }
                }
            }
        }
    }
}

// ── Continue Watching Row ──────────────────────────────────────────────────

@Composable
private fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onWatchClick: (String, Int, String) -> Unit,
) {
    LazyRow(
        contentPadding    = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items) { _, item ->
            val inter = remember { MutableInteractionSource() }
            val hovered by inter.collectIsHoveredAsState()
            // Parse anime id from link e.g. "/watch/{id}?ep=1&lang=sub"
            val animeId = item.link.removePrefix("/").split("/").getOrNull(1) ?: ""
            val lang    = Uri.parseQueryParam(item.link, "lang") ?: "sub"

            Column(
                Modifier
                    .width(260.dp)
                    .hoverable(inter)
                    .clickable { onWatchClick(animeId, item.episode, lang) }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model             = item.thumbnail,
                        contentDescription = item.title,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize(),
                    )
                    // Progress bar
                    val progress = parseProgressFraction(item.progress, item.duration)
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .background(Color(0xFFFF4444))
                        )
                    }
                    // Hover play and delete overlay
                    if (hovered) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Box(
                                Modifier.align(Alignment.Center)
                                    .background(Color.White, CircleShape)
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow, null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            // Delete button placeholder
                            Box(
                                Modifier.align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .clickable { /* Handle delete later */ }
                                    .padding(6.dp)
                            ) {
                                Text("X", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Bottom Left Episode badge
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp, start = 8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("EP ${item.episode}", color = Color.White, fontSize = 12.sp)
                    }

                    // Bottom Right Time badge
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp, end = 8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${item.progress}/${item.duration}", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    item.title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Generic Anime Section ──────────────────────────────────────────────────

@Composable
private fun AnimeSection(
    title: String,
    items: List<AnimeItem>,
    onItemClick: (AnimeItem) -> Unit,
    showViewMore: Boolean = true,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val isXlScreen = maxWidth >= 1280.dp
        Column {
            SectionHeader(title = title, onViewMore = if (showViewMore) ({ }) else null)
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(items) { _, item ->
                    AnimeCard(
                        item   = item,
                        width  = if (isXlScreen) 200.dp else 160.dp,
                        height = if (isXlScreen) 280.dp else 225.dp,
                        onClick = { onItemClick(item) }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AnimeCard(item: AnimeItem, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val inter    = remember { MutableInteractionSource() }
    val hovered  by inter.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (hovered) 12.dp else 0.dp, tween(200))

    Column(
        Modifier
            .width(width)
            .hoverable(inter)
            .clickable(onClick = onClick)
            .offset(y = -elevation / 2)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Background image
            AsyncImage(
                model              = item.imageUrl,
                contentDescription = item.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )

            // Bottom Gradient
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f),
                        ),
                        startY = 0f,
                        endY   = Float.POSITIVE_INFINITY,
                    )
                )
            )

            // Top-left Badges
            androidx.compose.foundation.layout.FlowRow(
                Modifier.align(Alignment.TopStart).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!item.type.isNullOrBlank()) {
                    SmallBadge(text = item.type)
                }
                if ((item.epCount ?: 0) > 0) {
                    SmallBadge(text = item.epCount.toString(), color = Color(0xFFFFEB3B)) // Yellow
                }
                if ((item.dubbedCount ?: 0) > 0) {
                    SmallBadge(text = item.dubbedCount.toString(), color = Color(0xFF42A5F5)) // Blue
                }
            }

            // Bottom Title & Eps
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Episodes ${item.epCount ?: "?"}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Small helpers ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onViewMore: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (onViewMore != null) {
            TextButton(onClick = onViewMore) {
                Text("View more >", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SpotlightTag(index: Int) {
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(Color(0xFFFF4444), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text("#${index + 1} Spotlight", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetaTag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MalScoreBadge(score: Double) {
    Box(
        Modifier
            .background(Color(0xFF00C853), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("MAL $score", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CarouselNavBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
        Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// ── HTML Stripper ──────────────────────────────────────────────────────────

private fun stripHtmlTags(htmlContent: String): String {
    val br    = htmlContent.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    val p     = br.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
    val div   = p.replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
    return div.replace(Regex("<[^>]*>"), "").trim()
}

@Composable
private fun SmallBadge(text: String, color: Color = Color.White) {
    Box(
        Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Sidebar ────────────────────────────────────────────────────────────────

@Composable
private fun AnisugSidebar(avatarUrl: String?, onExit: () -> Unit) {
    val fullAvatarUrl = when {
        avatarUrl == null -> null
        avatarUrl.startsWith("http") -> avatarUrl
        else -> "https://kuudere.to$avatarUrl"
    }

    DraggableWindowArea(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0B)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            // User Avatar / Logo
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (fullAvatarUrl != null) {
                    AsyncImage(
                        model = fullAvatarUrl,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
            
            // Icons
            SidebarIcon(Icons.Outlined.CalendarToday, isSelected = false)
            SidebarIcon(Icons.Outlined.Home, isSelected = true, selectedTint = Color(0xFFFF4444))
            SidebarIcon(Icons.Outlined.Explore, isSelected = false)
            SidebarIcon(Icons.Outlined.Bookmarks, isSelected = false)
            SidebarIcon(Icons.Outlined.Download, isSelected = false)
            SidebarIcon(Icons.Outlined.Settings, isSelected = false)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SidebarIcon(
                Icons.AutoMirrored.Filled.ExitToApp, 
                isSelected = false, 
                defaultTint = Color(0xFFE53935),
                onClick = onExit
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun SidebarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isSelected: Boolean,
    selectedTint: Color = Color(0xFFFF4444),
    defaultTint: Color = Color.Gray.copy(alpha = 0.4f),
    onClick: () -> Unit = {}
) {
    val tint = if (isSelected) selectedTint else defaultTint
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
    Spacer(Modifier.height(8.dp))
}

// ── Utility ────────────────────────────────────────────────────────────────

private fun parseProgressFraction(progress: String, duration: String): Float {
    fun toSeconds(t: String): Int {
        val parts = t.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0
        }
    }
    val cur = toSeconds(progress)
    val tot = toSeconds(duration)
    return if (tot > 0) (cur.toFloat() / tot).coerceIn(0f, 1f) else 0f
}

/** Minimal URL query param parser (no android dependency). */
private object Uri {
    fun parseQueryParam(url: String, key: String): String? {
        val query = url.substringAfter('?', "")
        return query.split('&').firstOrNull { it.startsWith("$key=") }?.substringAfter('=')
    }
}
