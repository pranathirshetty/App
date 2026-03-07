package to.kuudere.anisuge.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import to.kuudere.anisuge.data.models.ScheduleAnime

// ── Calendar helpers ──────────────────────────────────────────────────────────

private data class CalendarEvent(
    val id: String,
    val animeId: String,
    val name: String,
    val time: String,
    val episode: Int,
    val image: String,
    val banner: String?,
    val date: String,
)

private data class CalendarDay(
    val date: String,           // YYYY-MM-DD
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val events: List<CalendarEvent>,
)

private fun buildCalendarDays(
    year: Int,
    month: Int,          // 1-based
    weekStartsOnMonday: Boolean,
    schedule: Map<String, List<ScheduleAnime>>,
): List<CalendarDay> {
    // Determine first day of month (0=Sun … 6=Sat)
    val firstDayOfWeek = dayOfWeek(year, month, 1)   // 0=Sun
    val daysInMonth = daysInMonth(year, month)

    // Offset: how many "previous month" padding cells we need
    val startOffset = if (weekStartsOnMonday) {
        (firstDayOfWeek + 6) % 7   // Mon=0 … Sun=6
    } else {
        firstDayOfWeek               // Sun=0 … Sat=6
    }

    val days = mutableListOf<CalendarDay>()

    // Padding from previous month
    val prevYear  = if (month == 1) year - 1 else year
    val prevMonth = if (month == 1) 12 else month - 1
    val daysInPrevMonth = daysInMonth(prevYear, prevMonth)
    for (i in startOffset downTo 1) {
        val d = daysInPrevMonth - i + 1
        val dateStr = formatDate(prevYear, prevMonth, d)
        days.add(CalendarDay(dateStr, d, false, false, schedule[dateStr].toEvents(dateStr)))
    }

    // Current month
    val today = todayString()
    for (d in 1..daysInMonth) {
        val dateStr = formatDate(year, month, d)
        days.add(CalendarDay(dateStr, d, true, dateStr == today, schedule[dateStr].toEvents(dateStr)))
    }

    // Padding to next month — fill to complete 6 rows (42 cells)
    val nextYear  = if (month == 12) year + 1 else year
    val nextMonth = if (month == 12) 1 else month + 1
    var nd = 1
    while (days.size < 42) {
        val dateStr = formatDate(nextYear, nextMonth, nd)
        days.add(CalendarDay(dateStr, nd, false, false, schedule[dateStr].toEvents(dateStr)))
        nd++
    }

    return days
}

private fun List<ScheduleAnime>?.toEvents(date: String): List<CalendarEvent> =
    this?.mapIndexed { i, a ->
        CalendarEvent(
            id      = "${a.id}-${a.episode}-$date",
            animeId = a.id,
            name    = a.title,
            time    = a.time,
            episode = a.episode,
            image   = a.imageUrl,
            banner  = a.bannerUrl,
            date    = date,
        )
    } ?: emptyList()

/** Zeller-like day of week – returns 0=Sun … 6=Sat */
private fun dayOfWeek(y: Int, m: Int, d: Int): Int {
    val ay = if (m < 3) y - 1 else y
    val am = if (m < 3) m + 12 else m
    val k = ay % 100
    val j = ay / 100
    val h = (d + (13 * (am + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7
    return ((h + 5) % 7)   // 0=Sun, 1=Mon, … 6=Sat
}

private fun daysInMonth(y: Int, m: Int): Int = when (m) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11            -> 30
    2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
    else -> 30
}

private fun formatDate(y: Int, m: Int, d: Int) =
    "${y}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"

/** Returns today's date as "YYYY-MM-DD" using platform-agnostic epoch math. */
private fun todayString(): String {
    // We use currentTimeMillis — no java.time dependency needed for a simple date string
    val epochMs  = System.currentTimeMillis()
    val days     = (epochMs / 86400000L).toInt()   // Days since 1970-01-01
    var remaining = days
    var year = 1970
    while (true) {
        val diy = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        if (remaining < diy) break
        remaining -= diy
        year++
    }
    val monthDays = intArrayOf(31, if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28,
        31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    while (month < 12 && remaining >= monthDays[month]) {
        remaining -= monthDays[month]
        month++
    }
    return formatDate(year, month + 1, remaining + 1)
}

private fun currentYearMonth(): Pair<Int, Int> {
    val today = todayString()
    val parts = today.split("-")
    return parts[0].toInt() to parts[1].toInt()
}

private val MONTH_NAMES = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val SHORT_MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
private val DAY_LABELS_MON = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DAY_LABELS_SUN = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private fun prevMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to month - 1

private fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) year + 1 to 1 else year to month + 1

private fun isCurrentYearMonth(year: Int, month: Int): Boolean {
    val (cy, cm) = currentYearMonth()
    return year == cy && month == cm
}

// ── Colours ──────────────────────────────────────────────────────────────────

private val BG       = Color(0xFF0B0B0B)          // same as rest of app
private val SURFACE  = Color(0xFF0B0B0B)          // pure black — no gray
private val SURFACE2 = Color.Black                 // cells / rows: true black
private val BORDER   = Color.White.copy(alpha = 0.10f) // crisp white border
private val RED      = Color(0xFFFF4444)
private val MUTED    = Color.White.copy(alpha = 0.55f) // white-based, never gray
private val MAX_VISIBLE = 4

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onAnimeClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    var displayYear  by remember { mutableStateOf(currentYearMonth().first) }
    var displayMonth by remember { mutableStateOf(currentYearMonth().second) }
    var weekStartsMon by remember { mutableStateOf(true) }
    var showSettings  by remember { mutableStateOf(false) }
    var prevDirection by remember { mutableStateOf(0) }   // -1 prev, +1 next

    val calendarDays = remember(displayYear, displayMonth, weekStartsMon, state.schedule) {
        buildCalendarDays(displayYear, displayMonth, weekStartsMon, state.schedule)
    }

    Box(Modifier.fillMaxSize().background(BG)) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = RED, strokeWidth = 3.dp)
                }
            }
            state.error != null -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = MUTED, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Failed to load schedule", color = MUTED, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = RED)
                    ) { Text("Retry") }
                }
            }
            else -> {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val isDesktop = maxWidth >= 800.dp

                    Column(Modifier.fillMaxSize()) {
                        // ── Header ────────────────────────────────────────
                        CalendarHeader(
                            year = displayYear,
                            month = displayMonth,
                            isDesktop = isDesktop,
                            showSettings = showSettings,
                            weekStartsMon = weekStartsMon,
                            onPrev = {
                                prevDirection = -1
                                val (y, m) = prevMonth(displayYear, displayMonth)
                                displayYear = y; displayMonth = m
                            },
                            onNext = {
                                prevDirection = 1
                                val (y, m) = nextMonth(displayYear, displayMonth)
                                displayYear = y; displayMonth = m
                            },
                            onToggleSettings = { showSettings = !showSettings },
                            onWeekStartChange = { weekStartsMon = it },
                        )

                        // ── Body ──────────────────────────────────────────
                        if (isDesktop) {
                            DesktopCalendarBody(
                                days = calendarDays,
                                weekStartsMon = weekStartsMon,
                                direction = prevDirection,
                                yearMonth = displayYear to displayMonth,
                                onAnimeClick = onAnimeClick,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                        } else {
                            MobileScheduleContent(
                                calendarDays = calendarDays,
                                year = displayYear,
                                month = displayMonth,
                                direction = prevDirection,
                                onAnimeClick = onAnimeClick,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    isDesktop: Boolean,
    showSettings: Boolean,
    weekStartsMon: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleSettings: () -> Unit,
    onWeekStartChange: (Boolean) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(SURFACE)
            .border(
                width = 1.dp,
                color = BORDER,
                shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Prev button
            NavCircleBtn(Icons.AutoMirrored.Filled.ArrowBack, onClick = onPrev)

            Spacer(Modifier.weight(1f))

            // Month / Year label (centered absolutely)
            val label = if (isDesktop)
                "${MONTH_NAMES[month - 1]} $year"
            else
                "${SHORT_MONTH_NAMES[month - 1]} $year"
            val labelColor = animateColorAsState(
                if (isCurrentYearMonth(year, month)) Color.White else MUTED,
                animationSpec = tween(300)
            ).value
            Text(
                label,
                color = labelColor,
                fontSize = if (isDesktop) 20.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.weight(1f))

            // Settings button — only relevant on desktop (where the calendar grid is)
            if (isDesktop) {
                Box {
                    NavCircleBtn(Icons.Default.Settings, onClick = onToggleSettings)
                    if (showSettings) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            onDismissRequest = onToggleSettings,
                        ) {
                            SettingsPopup(
                                weekStartsMon = weekStartsMon,
                                onWeekStartChange = { onWeekStartChange(it); onToggleSettings() },
                                onDismiss = onToggleSettings,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            // Next button
            NavCircleBtn(Icons.AutoMirrored.Filled.ArrowForward, onClick = onNext)
        }
    }
}

@Composable
private fun NavCircleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val inter = remember { MutableInteractionSource() }
    val hovered by inter.collectIsHoveredAsState()
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (hovered) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, BORDER, CircleShape)
            .hoverable(inter)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsPopup(
    weekStartsMon: Boolean,
    onWeekStartChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, BORDER, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text("Week starts on", color = MUTED, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekStartOption("Monday", weekStartsMon) { onWeekStartChange(true) }
            WeekStartOption("Sunday", !weekStartsMon) { onWeekStartChange(false) }
        }
    }
}

@Composable
private fun WeekStartOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) RED.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
    val border = if (selected) RED.copy(alpha = 0.4f) else BORDER
    val textColor = if (selected) Color.White else MUTED
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ── Desktop Calendar Body ─────────────────────────────────────────────────────

@Composable
private fun DesktopCalendarBody(
    days: List<CalendarDay>,
    weekStartsMon: Boolean,
    direction: Int,
    yearMonth: Pair<Int, Int>,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // Day-of-week header row
        Row(
            Modifier
                .fillMaxWidth()
                .background(SURFACE2)
                .border(width = 1.dp, color = BORDER),
        ) {
            val labels = if (weekStartsMon) DAY_LABELS_MON else DAY_LABELS_SUN
            labels.forEach { label ->
                Box(
                    Modifier.weight(1f).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = MUTED, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Grid
        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = {
                val dir = direction
                val enter = slideInHorizontally(tween(280)) { if (dir >= 0) it / 5 else -it / 5 } + fadeIn(tween(280))
                val exit  = slideOutHorizontally(tween(280)) { if (dir >= 0) -it / 5 else it / 5 } + fadeOut(tween(200))
                enter.togetherWith(exit)
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { _ ->
            // 6 rows × 7 columns
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 6) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val idx = row * 7 + col
                            val day = days.getOrNull(idx)
                            if (day != null) {
                                DesktopCalendarCell(
                                    day = day,
                                    onAnimeClick = onAnimeClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, BORDER),
                                )
                            } else {
                                Box(Modifier.weight(1f).fillMaxHeight().border(0.5.dp, BORDER))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopCalendarCell(
    day: CalendarDay,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inter = remember { MutableInteractionSource() }
    val hovered by inter.collectIsHoveredAsState()

    // Cycling carousel index for background image
    var bgIndex by remember(day.date) { mutableStateOf(0) }
    LaunchedEffect(day.events) {
        if (day.events.size > 1) {
            while (true) {
                delay(5000)
                bgIndex = (bgIndex + 1) % day.events.size
            }
        }
    }

    var hoveredEventId by remember { mutableStateOf<String?>(null) }
    val displayEvent = if (hoveredEventId != null)
        day.events.find { it.id == hoveredEventId } ?: day.events.getOrNull(bgIndex)
    else
        day.events.getOrNull(bgIndex)

    val alpha by animateFloatAsState(
        if (day.events.isNotEmpty()) (if (day.isToday) 0.8f else if (hovered) 0.3f else 0.2f) else 0f,
        tween(400)
    )

    Box(
        modifier
            .background(if (day.isCurrentMonth) SURFACE else BG)
            .hoverable(inter)
    ) {
        // ── Background image carousel ──────────────────────────────────
        if (displayEvent != null && alpha > 0f) {
            Crossfade(targetState = displayEvent.image, animationSpec = tween(600)) { img ->
                AsyncImage(
                    model = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha }
                )
            }
            // Gradient overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to BG.copy(alpha = 0.75f),
                            1f to BG.copy(alpha = 0.97f),
                        )
                        onDrawBehind { drawRect(gradient) }
                    }
            )
        }

        if (!day.isCurrentMonth) {
            Box(Modifier.fillMaxSize().background(BG.copy(alpha = 0.6f)))
        }

        // ── Cell content ──────────────────────────────────────────────
        Column(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            // Day number
            Box(
                Modifier
                    .then(
                        if (day.isToday)
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        else
                            Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${day.dayNumber}",
                    color = when {
                        day.isToday -> Color.Black
                        day.isCurrentMonth -> Color.White
                        else -> MUTED
                    },
                    fontSize = 13.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    modifier = if (!day.isToday) Modifier.padding(2.dp) else Modifier,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Event list
            val visible = day.events.take(MAX_VISIBLE)
            visible.forEachIndexed { i, event ->
                DesktopEventRow(
                    event = event,
                    isHovered = hoveredEventId == event.id,
                    onHoverEnter = { hoveredEventId = event.id },
                    onHoverExit  = { hoveredEventId = null },
                    onClick = { onAnimeClick(event.animeId) },
                )
                if (i < visible.lastIndex) Spacer(Modifier.height(2.dp))
            }

            if (day.events.size > MAX_VISIBLE) {
                var showMore by remember { mutableStateOf(false) }
                Box {
                    Text(
                        "+ ${day.events.size - MAX_VISIBLE} more",
                        color = MUTED,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable { showMore = true }
                    )
                    if (showMore) {
                        Popup(onDismissRequest = { showMore = false }) {
                            Column(
                                Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                                    .border(1.dp, BORDER, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                day.events.drop(MAX_VISIBLE).forEach { event ->
                                    MoreEventRow(event = event, onClick = { onAnimeClick(event.animeId); showMore = false })
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Hover tooltip ──────────────────────────────────────────────
        val tooltipEvent = hoveredEventId?.let { id -> day.events.find { it.id == id } }
        AnimatedVisibility(
            visible = tooltipEvent != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
        ) {
            tooltipEvent?.let { ev ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.92f))
                        .border(1.dp, BORDER, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${ev.name.take(26)}${if (ev.name.length > 26) "…" else ""} · Ep ${ev.episode}" +
                            if (ev.time.isNotBlank()) " · ${ev.time}" else "",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopEventRow(
    event: CalendarEvent,
    isHovered: Boolean,
    onHoverEnter: () -> Unit,
    onHoverExit: () -> Unit,
    onClick: () -> Unit,
) {
    val inter = remember { MutableInteractionSource() }
    val hovered by inter.collectIsHoveredAsState()

    LaunchedEffect(hovered) {
        if (hovered) onHoverEnter() else onHoverExit()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHovered) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .hoverable(inter)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            event.name,
            color = if (isHovered) Color.White else Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Ep.${event.episode}",
            color = MUTED,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MoreEventRow(event: CalendarEvent, onClick: () -> Unit) {
    val inter = remember { MutableInteractionSource() }
    val hovered by inter.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .hoverable(inter)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            event.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("Ep.${event.episode}", color = MUTED, fontSize = 11.sp)
    }
}

// ── Mobile Schedule Content ───────────────────────────────────────────────────

@Composable
private fun MobileScheduleContent(
    calendarDays: List<CalendarDay>,
    year: Int,
    month: Int,
    direction: Int,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = year to month,
        transitionSpec = {
            val enter = slideInHorizontally(tween(280)) { if (direction >= 0) it / 5 else -it / 5 } + fadeIn(tween(280))
            val exit  = slideOutHorizontally(tween(280)) { if (direction >= 0) -it / 5 else it / 5 } + fadeOut(tween(200))
            enter.togetherWith(exit)
        },
        modifier = modifier,
    ) { _ ->
        Column(Modifier.fillMaxSize()) {
            // Day list with events — only days that have events OR today
            val relevantDays = calendarDays.filter { it.events.isNotEmpty() || it.isToday }
            if (relevantDays.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No episodes scheduled this month", color = MUTED, fontSize = 14.sp)
                }
            } else {
                val listState = rememberLazyListState()
                // Auto-scroll to today
                val todayIdx = relevantDays.indexOfFirst { it.isToday }
                LaunchedEffect(relevantDays) {
                    if (todayIdx >= 0) listState.scrollToItem(todayIdx)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(relevantDays, key = { it.date }) { day ->
                        MobileDayItem(day = day, onAnimeClick = onAnimeClick)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(BORDER))
                    }
                }
            }
        }
    }
}


@Composable
private fun MobileDayItem(
    day: CalendarDay,
    onAnimeClick: (String) -> Unit,
) {
    // Day of week label
    val dayOfWeekLabel = run {
        val dow = dayOfWeek(
            day.date.split("-")[0].toInt(),
            day.date.split("-")[1].toInt(),
            day.date.split("-")[2].toInt(),
        )
        arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[dow]
    }
    val monthLabel = SHORT_MONTH_NAMES[day.date.split("-")[1].toInt() - 1]

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        // Header row
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Day number circle
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (day.isToday) Color.White else SURFACE2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${day.dayNumber}",
                        color = if (day.isToday) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        dayOfWeekLabel,
                        color = if (day.isToday) RED else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "$monthLabel ${day.dayNumber}",
                        color = MUTED,
                        fontSize = 12.sp,
                    )
                }
            }
            if (day.events.isNotEmpty()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(SURFACE2)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${day.events.size} ep${if (day.events.size > 1) "s" else ""}",
                        color = MUTED,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (day.events.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.events.forEach { event ->
                    MobileEventCard(event = event, onAnimeClick = onAnimeClick)
                }
            }
        } else if (day.isToday) {
            Text(
                "No episodes scheduled for today",
                color = MUTED,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun MobileEventCard(
    event: CalendarEvent,
    onAnimeClick: (String) -> Unit,
) {
    val inter = remember { MutableInteractionSource() }
    val hovered by inter.collectIsHoveredAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (hovered) Color.White.copy(alpha = 0.06f) else SURFACE.copy(alpha = 0.5f))
            .border(1.dp, BORDER, RoundedCornerShape(10.dp))
            .hoverable(inter)
            .clickable { onAnimeClick(event.animeId) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Poster thumbnail
        Box(
            Modifier
                .width(44.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SURFACE2)
        ) {
            if (event.image.isNotBlank()) {
                AsyncImage(
                    model = event.image,
                    contentDescription = event.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                event.name,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Episode ${event.episode}",
                    color = MUTED,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (event.time.isNotBlank()) {
                    Text("•", color = MUTED, fontSize = 12.sp)
                    Text(event.time, color = MUTED, fontSize = 12.sp)
                }
            }
        }
    }
}
