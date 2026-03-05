package to.kuudere.anisuge.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import to.kuudere.anisuge.data.models.AnimeItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAnimeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyGridState()

    // Infinite scroll listener
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo }.collect { layoutInfo ->
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 5) {
                viewModel.search(loadMore = true)
            }
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterSection(state, viewModel)
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Results: ${state.results.size}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                if (state.isLoading && state.results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                } else {
                    items(state.results) { anime ->
                        SearchAnimeCard(item = anime, onClick = { onAnimeClick(anime.id) })
                    }
                }

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(state: SearchUiState, viewModel: SearchViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Filter",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        
        // Search Input
        SearchInputBar(
            value = state.keyword,
            onValueChange = viewModel::onKeywordChange,
            onSearch = { viewModel.search() }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Expansion Header
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = Color.White
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.fillMaxWidth()) {
                FilterGrid(state, viewModel)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.search() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Apply Filters")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearFilters() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ClearAll, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Filters")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputBar(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        placeholder = { Text("Search...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1A1B1E),
            unfocusedContainerColor = Color(0xFF1A1B1E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true
    )
}

@Composable
private fun FilterGrid(state: SearchUiState, viewModel: SearchViewModel) {
    val genres = listOf(
        "Action", "Adventure", "Cars", "Comedy", "Dementia", "Demons", "Drama", "Ecchi", "Fantasy", "Game",
        "Harem", "Historical", "Horror", "Isekai", "Josei", "Kids", "Magic", "Martial Arts", "Mecha", "Military",
        "Music", "Mystery", "Parody", "Police", "Psychological", "Romance", "Samurai", "School", "Sci-Fi", "Seinen",
        "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai", "Slice of Life", "Space", "Sports", "Super Power", "Supernatural", "Thriller",
        "unknown", "Vampire"
    )
    val seasons = listOf("Winter", "Spring", "Summer", "Fall")
    val years = (2025 downTo 1980).map { it.toString() }
    val languages = listOf("Japanese", "English")
    val ratings = listOf("G", "PG", "PG-13", "R", "R+")
    val types = listOf("TV", "Movie", "OVA")
    val statuses = listOf("Finished", "Releasing", "Not Yet Released", "Cancelled")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterDropdown("Select genre", state.selectedGenres.joinToString(", ").ifBlank { null }, genres, Modifier.weight(1f)) {
                viewModel.onGenreToggle(it)
            }
            FilterDropdown("Select seasons", state.selectedSeason, seasons, Modifier.weight(1f)) {
                viewModel.onSeasonChange(it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterDropdown("Select years", state.selectedYear, years, Modifier.weight(1f)) {
                viewModel.onYearChange(it)
            }
            FilterDropdown("Select languages", state.selectedLanguage, languages, Modifier.weight(1f)) {
                viewModel.onLanguageChange(it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterDropdown("Select ratings", state.selectedRating, ratings, Modifier.weight(1f)) {
                viewModel.onRatingChange(it)
            }
            FilterDropdown("Select types", state.selectedType, types, Modifier.weight(1f)) {
                viewModel.onTypeChange(it)
            }
        }
        FilterDropdown("Select statuses", state.selectedStatus, statuses, Modifier.fillMaxWidth(0.5f)) {
            viewModel.onStatusChange(it)
        }
    }
}

@Composable
private fun FilterDropdown(
    hint: String,
    selected: String?,
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1B1E))
            .clickable { expanded = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selected ?: hint,
                color = if (selected == null) Color.Gray else Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1B1E)).width(200.dp)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = Color.White) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchAnimeCard(item: AnimeItem, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = if (item.imageUrl.startsWith("http")) item.imageUrl else "https://kuudere.to${item.imageUrl}",
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            )
        )
        
        // Badges
        FlowRow(
            Modifier.padding(8.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!item.type.isNullOrBlank()) SearchCardBadge(item.type)
            if ((item.epCount ?: 0) > 0) SearchCardBadge("${item.epCount}", Color.Yellow.copy(alpha = 0.8f))
            if ((item.dubbedCount ?: 0) > 0) SearchCardBadge("${item.dubbedCount}", Color.Cyan.copy(alpha = 0.8f))
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(8.dp)
        ) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Episodes ${item.epCount ?: 0}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchCardBadge(text: String, color: Color = Color.White) {
    Box(
        Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
