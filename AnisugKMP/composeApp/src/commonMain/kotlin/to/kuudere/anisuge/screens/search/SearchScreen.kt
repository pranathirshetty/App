package to.kuudere.anisuge.screens.search

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import to.kuudere.anisuge.data.models.AnimeItem
import to.kuudere.anisuge.screens.home.HomeOfflineState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {}
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
    ) { _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isSmall = maxWidth < 800.dp
            val columns = if (isSmall) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 180.dp)
            val hPadding = if (isSmall) 12.dp else 24.dp
            val itemSpacing = if (isSmall) 8.dp else 16.dp

            LazyVerticalGrid(
                columns = columns,
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = hPadding, end = hPadding, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
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
                } else if (state.isOffline && state.results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(400.dp)) {
                            HomeOfflineState(onRetry = { viewModel.search() })
                        }
                    }
                } else {
                    items(state.results) { anime ->
                        to.kuudere.anisuge.ui.AnimeCard(
                            item     = anime,
                            modifier = Modifier.fillMaxWidth(),
                            onClick  = { onAnimeClick(anime.id) }
                        )
                    }
                }

                if (state.isLoadingMore && !state.isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(state: SearchUiState, viewModel: SearchViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 800.dp) {
            SmallScreenFilterSection(state, viewModel)
        } else {
            LargeScreenFilterSection(state, viewModel)
        }
    }
}

// ─── Shared filter data (1:1 with Kuudere API) ──────────────────────────────
val KUUDERE_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
    "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports",
    "Supernatural", "Thriller", "Ecchi", "Harem", "Isekai", "Mecha",
    "Music", "Psychological", "School", "Military", "Historical",
    "Demons", "Magic", "Vampire", "Hentai"
)
private val KUUDERE_SORTS    = listOf("Popularity", "Latest", "Score", "Year", "Episodes")
private val KUUDERE_SEASONS  = listOf("Winter", "Spring", "Summer", "Fall")
private val KUUDERE_YEARS    = (2025 downTo 1975).map { it.toString() }
private val KUUDERE_STATUSES = listOf("Finished", "Releasing", "Not Yet Released", "Cancelled")
private val KUUDERE_FORMATS  = listOf("TV", "TV Short", "Movie", "Special", "OVA", "ONA", "Music")
private val KUUDERE_ORIGINS  = listOf("Japan", "South Korea", "China", "Taiwan")

// ─── Large screen layout ─────────────────────────────────────────────────────
@Composable
private fun LargeScreenFilterSection(state: SearchUiState, viewModel: SearchViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text("Search", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Row 1: Search | Genres | Sort by | Year | Status | Format  (equal weights)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Search", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                KSearchInput(state.keyword, viewModel::onKeywordChange, onSearch = { viewModel.search() })
            }
            FilterDropdown("Genres", "Any", state.selectedGenres.joinToString(", ").ifBlank { null }, KUUDERE_GENRES, Modifier.weight(1f), multiSelect = true) {
                if (it != null) viewModel.onGenreToggle(it) else viewModel.clearGenres()
            }
            FilterDropdown("Sort by", "Popularity", null, KUUDERE_SORTS, Modifier.weight(1f)) {}
            FilterDropdown("Year", "Any", state.selectedYear, KUUDERE_YEARS, Modifier.weight(1f)) { viewModel.onYearChange(it) }
            FilterDropdown("Status", "Any", state.selectedStatus, KUUDERE_STATUSES, Modifier.weight(1f)) { viewModel.onStatusChange(it) }
            FilterDropdown("Format", "Any", state.selectedType, KUUDERE_FORMATS, Modifier.weight(1f)) { viewModel.onTypeChange(it) }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("More filters", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = Color.Gray, modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                // Row 2: Season | Origin (partial row, left-aligned with same column width)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Season", "Any", state.selectedSeason, KUUDERE_SEASONS, Modifier.weight(1f)) { viewModel.onSeasonChange(it) }
                    FilterDropdown("Origin", "Any", state.selectedLanguage, KUUDERE_ORIGINS, Modifier.weight(1f)) { viewModel.onLanguageChange(it) }
                    // empty slots to push dropdowns to same width as row above
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KActionButton("Apply") { viewModel.search() }
                    KActionButton("Reset", secondary = true) { viewModel.clearFilters() }
                }
            }
        }

        AnimatedVisibility(visible = !isExpanded) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KActionButton("Apply") { viewModel.search() }
                KActionButton("Reset", secondary = true) { viewModel.clearFilters() }
            }
        }
    }
}

// ─── Small screen layout ─────────────────────────────────────────────────────
@Composable
private fun SmallScreenFilterSection(state: SearchUiState, viewModel: SearchViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text("Search", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KSearchInput(state.keyword, viewModel::onKeywordChange, onSearch = { viewModel.search() }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { isExpanded = !isExpanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = if (isExpanded) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Genres", "Any", state.selectedGenres.joinToString(", ").ifBlank { null }, KUUDERE_GENRES, Modifier.weight(1f), multiSelect = true) {
                        if (it != null) viewModel.onGenreToggle(it) else viewModel.clearGenres()
                    }
                    FilterDropdown("Sort by", "Popularity", null, KUUDERE_SORTS, Modifier.weight(1f)) {}
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Season", "Any", state.selectedSeason, KUUDERE_SEASONS, Modifier.weight(1f)) { viewModel.onSeasonChange(it) }
                    FilterDropdown("Year", "Any", state.selectedYear, KUUDERE_YEARS, Modifier.weight(1f)) { viewModel.onYearChange(it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Status", "Any", state.selectedStatus, KUUDERE_STATUSES, Modifier.weight(1f)) { viewModel.onStatusChange(it) }
                    FilterDropdown("Format", "Any", state.selectedType, KUUDERE_FORMATS, Modifier.weight(1f)) { viewModel.onTypeChange(it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Origin", "Any", state.selectedLanguage, KUUDERE_ORIGINS, Modifier.weight(1f)) { viewModel.onLanguageChange(it) }
                    Column(Modifier.weight(1f)) {
                        Text("", color = Color.Transparent, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        KApplyButton { isExpanded = false; viewModel.search() }
                    }
                }
            }
        }
    }
}

// ─── Shared UI primitives ────────────────────────────────────────────────────

@Composable
private fun KSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(44.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Color(0xFF71717A), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text("Search", color = Color(0xFF71717A), fontSize = 14.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            }
        }
    }
}

@Composable
private fun KActionButton(label: String, secondary: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (secondary) Color.Transparent else Color.White)
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (secondary) Color.White else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun KApplyButton(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Text("Apply", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterDropdown(
    title: String,
    hint: String,
    selected: String?,
    items: List<String>,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    onItemSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val triggerWidthDp: Dp = with(density) { triggerWidthPx.toDp() }
    val selectedItems = selected?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()

    Column(modifier = modifier) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Box {
            // ── Trigger ────────────────────────────────────────────────────
            Box(
                Modifier
                    .height(44.dp)
                    .fillMaxWidth()
                    .onSizeChanged { triggerWidthPx = it.width }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selected ?: hint,
                        color = if (selected == null) Color(0xFF71717A) else Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected != null) {
                        Icon(
                            Icons.Default.Close, "Clear",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(14.dp).clickable {
                                onItemSelected(null)
                                expanded = false
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── DropdownMenu (built-in animated expand/collapse) ────────────────
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(triggerWidthDp.coerceAtLeast(150.dp))
                    .heightIn(max = 280.dp),
                offset = DpOffset(0.dp, 6.dp),
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                items.forEach { item ->
                    val isSelected = selectedItems.contains(item)
                    DropdownMenuItem(
                        text = {
                            Text(
                                item,
                                color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            if (!multiSelect) expanded = false
                        },
                        modifier = Modifier.background(
                            if (isSelected) Color.White.copy(alpha = 0.07f) else Color.Transparent
                        ),
                        trailingIcon = if (multiSelect) {
                            {
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) Color.White
                                            else Color.White.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        } else null,
                        colors = MenuDefaults.itemColors(
                            textColor = Color.White,
                            disabledTextColor = Color.Gray,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

