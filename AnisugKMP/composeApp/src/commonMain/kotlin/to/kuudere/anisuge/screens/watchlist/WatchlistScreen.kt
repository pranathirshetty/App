package to.kuudere.anisuge.screens.watchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil3.compose.AsyncImage

// Dummy data structure matching Seanime's card
data class WatchlistEntry(
    val title: String,
    val coverImage: String,
    val progress: Int,
    val totalEpisodes: Int?,
    val status: String,
    val listType: String // "Current", "Planning", "Completed"
)

val sampleWatchlist = listOf(
    WatchlistEntry("Jujutsu Kaisen Season 1", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113415-bbBWj4pEFseh.jpg", 10, 24, "Releasing", "Current"),
    WatchlistEntry("Frieren: Beyond Journey's End", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n1MjsNOzHOwk.jpg", 4, 28, "Releasing", "Current"),
    WatchlistEntry("Sword Art Online", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/nx11757-Q9P2zjCPICq5.jpg", 25, 25, "Finished", "Completed"),
    WatchlistEntry("My Hero Academia FINALE", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171927-oONC2oPZzF2p.jpg", 0, null, "Not Yet Released", "Planning"),
    WatchlistEntry("Sentenced to Be a Hero", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171350-oG6o6r6G6R2G.jpg", 0, 12, "Not Yet Released", "Planning"),
    WatchlistEntry("Persona 4 the Animation", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/nx10588-sVEXFp0vQhB8.jpg", 1, 25, "Finished", "Planning")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onAnimeClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Anime") }
    var selectedList by remember { mutableStateOf("All lists") }
    var searchQuery by remember { mutableStateOf("") }

    val currentList = sampleWatchlist.filter { it.listType == "Current" }
    val planningList = sampleWatchlist.filter { it.listType == "Planning" }
    val completedList = sampleWatchlist.filter { it.listType == "Completed" }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 800.dp

        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0B)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val searchOptionsBlock = @Composable { modifier: Modifier ->
                var expandedFilters by remember { mutableStateOf(false) }

                Column(
                    modifier = modifier
                ) {
                    if (isDesktop) {
                        // Desktop layout: list selector, search bar, trash icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dropdown for list selector
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .weight(0.3f)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { selectedList = if (selectedList == "All lists") "Current" else "All lists" }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedList, color = Color.White, fontSize = 14.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            // Search field
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .weight(0.6f)
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                        singleLine = true,
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text("Search list", color = Color.Gray, fontSize = 14.sp)
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }

                            // Trash icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .clickable { searchQuery = "" }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Second row: Advanced filters grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AdvancedFilterSelect("Genre", "All genres", Icons.Default.Style, Modifier.weight(1f))
                            AdvancedFilterSelect("Sorting", "SCORE_DESC", Icons.Default.Sort, Modifier.weight(1f))
                            AdvancedFilterSelect("Format", "All formats", Icons.Default.Tv, Modifier.weight(1f))
                            AdvancedFilterSelect("Status", "All statuses", Icons.Default.SignalCellularAlt, Modifier.weight(1f))
                        }
                    } else {
                        // Mobile Layout
                        // First row: search, clear, expand button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search field
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                        singleLine = true,
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text("Search list", color = Color.Gray, fontSize = 14.sp)
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }

                            // Trash icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .clickable { searchQuery = "" }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray)
                            }
                            
                            // Expand Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .clickable { expandedFilters = !expandedFilters }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (expandedFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = Color.Gray)
                            }
                        }

                        AnimatedVisibility(expandedFilters) {
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                // Next row: list selector
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable { selectedList = if (selectedList == "All lists") "Current" else "All lists" }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedList, color = Color.White, fontSize = 14.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))

                                // Next 2 rows: 2 advanced filters per row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdvancedFilterSelect("Genre", "All genres", Icons.Default.Style, Modifier.weight(1f))
                                    AdvancedFilterSelect("Sorting", "SCORE_DESC", Icons.Default.Sort, Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdvancedFilterSelect("Format", "All formats", Icons.Default.Tv, Modifier.weight(1f))
                                    AdvancedFilterSelect("Status", "All statuses", Icons.Default.SignalCellularAlt, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (isDesktop) {
                searchOptionsBlock(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 8.dp))
            }

            // Lists content
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = if (isDesktop) 8.dp else 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isDesktop) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        searchOptionsBlock(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    }
                }

            val showAll = selectedList == "All lists"

            if (showAll || selectedList == "Current") {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Current", currentList.size)
                }
                items(currentList) { WatchlistCard(it, onAnimeClick) }
            }

            if (showAll || selectedList == "Planning") {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Planning", planningList.size)
                }
                items(planningList) { WatchlistCard(it, onAnimeClick) }
            }

            if (showAll || selectedList == "Completed") {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Completed", completedList.size)
                }
                items(completedList) { WatchlistCard(it, onAnimeClick) }
            }
        }
    }
}
}

@Composable
fun AdvancedFilterSelect(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable { },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Text(count.toString(), color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WatchlistCard(entry: WatchlistEntry, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick("fake-id") }
    ) {
        // Image box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.4f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = entry.coverImage,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Bottom gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 100f
                        )
                    )
            )

            // Progress text
            Text(
                text = "Ep ${entry.progress} / ${entry.totalEpisodes ?: "?"}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )

            // Air status icon top right for planning/current
            if (entry.status == "Releasing") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
            }
        }

        // Title text outside image
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
    }
}
