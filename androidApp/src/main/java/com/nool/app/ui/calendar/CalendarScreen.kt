package com.nool.app.ui.calendar

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.ui.theme.Dimens
import com.nool.core.model.CalendarEvent
import com.nool.core.model.CalendarEventInstance
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterMenu by remember { mutableStateOf(false) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.CornerSmall))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val picked = Calendar.getInstance().apply {
                                            set(y, m, d)
                                        }
                                        viewModel.selectDate(picked.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4)
                    ) {
                        Text(
                            text = monthYearFormat.format(Date(uiState.selectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Today button
                    IconButton(
                        onClick = { viewModel.goToToday() },
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = "Go to Today")
                    }

                    // Previous Period
                    IconButton(
                        onClick = { viewModel.navigatePeriod(-1) },
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }

                    // Next Period
                    IconButton(
                        onClick = { viewModel.navigatePeriod(1) },
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }

                    // Filter Menu
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(Dimens.MinTouchTarget)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter Calendars")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            listOf("Personal", "Work", "Ideas").forEach { cat ->
                                val isChecked = uiState.activeCalendars.contains(cat)
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    leadingIcon = {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleCalendarFilter(cat)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateEvent() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Event")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Mode Switcher
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing8)
            ) {
                CalendarViewMode.values().forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CalendarViewMode.values().size),
                        onClick = { viewModel.setViewMode(mode) },
                        selected = uiState.viewMode == mode,
                        label = { Text(mode.label) }
                    )
                }
            }

            // Swipeable Container
            var dragDistance by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(uiState.viewMode) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragDistance > 80f) {
                                    viewModel.navigatePeriod(-1)
                                } else if (dragDistance < -80f) {
                                    viewModel.navigatePeriod(1)
                                }
                                dragDistance = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragDistance += dragAmount
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = uiState.viewMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "CalendarViewTransition"
                ) { targetMode ->
                    when (targetMode) {
                        CalendarViewMode.MONTH -> MonthGridView(
                            selectedDate = uiState.selectedDate,
                            events = uiState.events,
                            existingNoteDates = uiState.existingNoteDates,
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            onDayClick = { dayMillis -> viewModel.openAgendaSheetForDay(dayMillis) },
                            onDayLongClick = { dayMillis -> viewModel.openCreateEvent(dayMillis) }
                        )
                        CalendarViewMode.WEEK -> WeekTimelineView(
                            selectedDate = uiState.selectedDate,
                            events = uiState.events,
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            onEventClick = { event -> viewModel.openEditEvent(event) },
                            onTimeSlotLongClick = { slotMillis -> viewModel.openCreateEvent(slotMillis) }
                        )
                        CalendarViewMode.DAY -> DayTimelineView(
                            selectedDate = uiState.selectedDate,
                            events = uiState.events,
                            onEventClick = { event -> viewModel.openEditEvent(event) },
                            onTimeSlotLongClick = { slotMillis -> viewModel.openCreateEvent(slotMillis) }
                        )
                        CalendarViewMode.AGENDA -> AgendaListView(
                            events = uiState.events,
                            onEventClick = { event -> viewModel.openEditEvent(event) },
                            onOpenNote = onOpenNote,
                            onCreateEvent = { viewModel.openCreateEvent() }
                        )
                    }
                }
            }
        }
    }

    // Edit Event Bottom Sheet
    if (uiState.isEditSheetOpen) {
        EventEditSheet(
            event = uiState.editingEvent,
            initialDate = Date(uiState.selectedDate),
            onDismiss = { viewModel.dismissEditSheet() },
            onSave = { event -> viewModel.saveEvent(event) },
            onDelete = { id -> viewModel.deleteEvent(id) },
            onCreateNoteForEvent = { title ->
                viewModel.createNoteForEvent(title) { path ->
                    onOpenNote(path)
                }
            }
        )
    }

    // Selected Day Agenda Bottom Sheet (Shown when tapping a day cell in Month View)
    if (uiState.isAgendaSheetOpen) {
        val selectedDayCal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
        val dateHeaderFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
        val dayKey = dayKeyFormat.format(selectedDayCal.time)
        val hasNote = uiState.existingNoteDates.contains(dayKey)

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissAgendaSheet() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontalMargin)
                    .padding(bottom = Dimens.Spacing32)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = dateHeaderFormat.format(selectedDayCal.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.selectedDayEvents.size} scheduled ${if (uiState.selectedDayEvents.size == 1) "event" else "events"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            viewModel.dismissAgendaSheet()
                            viewModel.openCreateEvent(uiState.selectedDate)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Event", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing12))

                // Daily Note quick button
                OutlinedButton(
                    onClick = {
                        viewModel.dismissAgendaSheet()
                        onOpenNote("Daily Notes/$dayKey.md")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = if (hasNote) Icons.Default.Description else Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Text(if (hasNote) "Open Daily Note ($dayKey)" else "Create Daily Note ($dayKey)")
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing16))

                if (uiState.selectedDayEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.Spacing24),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No events scheduled for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Dimens.CardItemSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.selectedDayEvents) { instance ->
                            EventCardRow(
                                instance = instance,
                                onClick = {
                                    viewModel.dismissAgendaSheet()
                                    viewModel.openEditEvent(instance.event)
                                },
                                onOpenNote = onOpenNote
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridView(
    selectedDate: Long,
    events: List<CalendarEventInstance>,
    existingNoteDates: Set<String>,
    firstDayOfWeek: Int,
    onDayClick: (Long) -> Unit,
    onDayLongClick: (Long) -> Unit
) {
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayCal = remember { Calendar.getInstance() }
    val selectedCal = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }

    val daysInMonth = remember(selectedDate, firstDayOfWeek) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val currentMonth = cal.get(Calendar.MONTH)

        // Find how many days to pad before the 1st
        val firstDayOfMonthWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDayOfMonthWeek - firstDayOfWeek + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -offset)

        val list = mutableListOf<CalendarDayInfo>()
        for (i in 0 until 42) {
            val isCurrentMonth = cal.get(Calendar.MONTH) == currentMonth
            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            val isSelected = cal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

            val dayKey = dayKeyFormat.format(cal.time)
            val hasNote = existingNoteDates.contains(dayKey)

            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + 86_400_000L - 1
            val dayEvents = events.filter { it.instanceEndTime >= startOfDay && it.instanceStartTime <= endOfDay }

            list.add(
                CalendarDayInfo(
                    dateMillis = startOfDay,
                    dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday,
                    isSelected = isSelected,
                    hasNote = hasNote,
                    events = dayEvents
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val weekDays = remember(firstDayOfWeek) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        (0 until 7).map {
            val name = format.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            name
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.Spacing8)) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Spacing4)) {
            weekDays.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // 6 rows of 7 days
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 7) {
                        val info = daysInMonth[row * 7 + col]
                        MonthDayCell(
                            dayInfo = info,
                            onClick = { onDayClick(info.dateMillis) },
                            onLongClick = { onDayLongClick(info.dateMillis) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

private data class CalendarDayInfo(
    val dateMillis: Long,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasNote: Boolean,
    val events: List<CalendarEventInstance>
)

@Composable
private fun MonthDayCell(
    dayInfo: CalendarDayInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        dayInfo.isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(Dimens.CornerSmall))
            .background(bgColor)
            .pointerInput(dayInfo.dateMillis) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (dayInfo.isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${dayInfo.dayNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (dayInfo.isToday || dayInfo.isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        dayInfo.isToday -> MaterialTheme.colorScheme.onPrimary
                        !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Note Indicator
            if (dayInfo.hasNote) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Has note",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Event dots
            if (dayInfo.events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    dayInfo.events.take(3).forEach { ev ->
                        val dotColor = try {
                            Color(android.graphics.Color.parseColor(ev.event.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTimelineView(
    selectedDate: Long,
    events: List<CalendarEventInstance>,
    firstDayOfWeek: Int,
    onEventClick: (CalendarEvent) -> Unit,
    onTimeSlotLongClick: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val cal = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }
    val todayCal = remember { Calendar.getInstance() }

    val daysOfWeek = remember(selectedDate, firstDayOfWeek) {
        val weekCal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        val dayNameFormat = SimpleDateFormat("EEE\nd", Locale.getDefault())
        (0 until 7).map {
            val millis = weekCal.timeInMillis
            val isToday = weekCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    weekCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            val label = dayNameFormat.format(weekCal.time)
            weekCal.add(Calendar.DAY_OF_MONTH, 1)
            Triple(millis, label, isToday)
        }
    }

    // Auto scroll to current hour on load
    LaunchedEffect(Unit) {
        val curHour = todayCal.get(Calendar.HOUR_OF_DAY)
        scrollState.scrollTo((curHour * 60).coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week days header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = Dimens.Spacing8, top = Dimens.Spacing4, bottom = Dimens.Spacing4)
        ) {
            daysOfWeek.forEach { (_, label, isToday) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(Dimens.CornerXSmall))
                        .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider()

        // 24 hour grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column {
                for (hour in 0..23) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:00", hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(44.dp).padding(start = 4.dp, top = 2.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f).padding(top = 8.dp)
                        )
                    }
                }
            }

            // Current Time Red Indicator Line
            val nowCal = Calendar.getInstance()
            val minutesIntoDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val currentLineY = (minutesIntoDay * 1.0f).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = currentLineY)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.error)
            )

            // Render events
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp)
            ) {
                daysOfWeek.forEach { (dayMillis, _, _) ->
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = dayMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = dayStart + 86_400_000L - 1

                    val dayEvents = events.filter { it.instanceEndTime >= dayStart && it.instanceStartTime <= dayEnd }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1440.dp) // 24 hours * 60dp
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .pointerInput(dayMillis) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        val clickedMinute = offset.y.toInt()
                                        val slotMillis = dayStart + (clickedMinute * 60_000L)
                                        onTimeSlotLongClick(slotMillis)
                                    }
                                )
                            }
                    ) {
                        dayEvents.forEach { inst ->
                            val startCal = Calendar.getInstance().apply { timeInMillis = inst.instanceStartTime }
                            val endCal = Calendar.getInstance().apply { timeInMillis = inst.instanceEndTime }

                            val startMinutes = (startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)).coerceIn(0, 1439)
                            val endMinutes = (endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)).coerceIn(startMinutes + 15, 1440)
                            val duration = (endMinutes - startMinutes).coerceAtLeast(20)

                            val eventColor = try {
                                Color(android.graphics.Color.parseColor(inst.event.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Card(
                                shape = RoundedCornerShape(Dimens.CornerXSmall),
                                colors = CardDefaults.cardColors(containerColor = eventColor.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = startMinutes.dp)
                                    .height(duration.dp)
                                    .padding(horizontal = 1.dp)
                                    .clickable { onEventClick(inst.event) }
                            ) {
                                Text(
                                    text = inst.event.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTimelineView(
    selectedDate: Long,
    events: List<CalendarEventInstance>,
    onEventClick: (CalendarEvent) -> Unit,
    onTimeSlotLongClick: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val todayCal = remember { Calendar.getInstance() }
    val selectedCal = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }

    val startOfDay = remember(selectedDate) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfDay = startOfDay + 86_400_000L - 1
    val dayEvents = events.filter { it.instanceEndTime >= startOfDay && it.instanceStartTime <= endOfDay }

    LaunchedEffect(Unit) {
        val curHour = todayCal.get(Calendar.HOUR_OF_DAY)
        scrollState.scrollTo((curHour * 60).coerceAtLeast(0))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Dimens.Spacing8)
    ) {
        // Grid lines
        Column {
            for (hour in 0..23) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:00", hour),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(56.dp).padding(top = 2.dp)
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).padding(top = 10.dp)
                    )
                }
            }
        }

        // Current time line
        val isToday = selectedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            val minutesIntoDay = todayCal.get(Calendar.HOUR_OF_DAY) * 60 + todayCal.get(Calendar.MINUTE)
            val currentLineY = (minutesIntoDay * 1.0f).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
                    .offset(y = currentLineY)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.error)
            )
        }

        // Events on this day
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp)
                .height(1440.dp)
                .pointerInput(startOfDay) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val clickedMinute = offset.y.toInt()
                            val slotMillis = startOfDay + (clickedMinute * 60_000L)
                            onTimeSlotLongClick(slotMillis)
                        }
                    )
                }
        ) {
            dayEvents.forEach { inst ->
                val startCal = Calendar.getInstance().apply { timeInMillis = inst.instanceStartTime }
                val endCal = Calendar.getInstance().apply { timeInMillis = inst.instanceEndTime }

                val startMinutes = (startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)).coerceIn(0, 1439)
                val endMinutes = (endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)).coerceIn(startMinutes + 15, 1440)
                val duration = (endMinutes - startMinutes).coerceAtLeast(30)

                val eventColor = try {
                    Color(android.graphics.Color.parseColor(inst.event.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = eventColor.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .offset(y = startMinutes.dp)
                        .height(duration.dp)
                        .clickable { onEventClick(inst.event) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4)) {
                        Text(
                            text = inst.event.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (inst.event.description.isNotBlank()) {
                            Text(
                                text = inst.event.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaListView(
    events: List<CalendarEventInstance>,
    onEventClick: (CalendarEvent) -> Unit,
    onOpenNote: (String) -> Unit,
    onCreateEvent: () -> Unit
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.Spacing32),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Dimens.Spacing16))
                Text("No upcoming events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(Dimens.Spacing8))
                Text(
                    "Your schedule is clear. Create events and link them with your notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.Spacing16))
                OutlinedButton(onClick = onCreateEvent, shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Text("Add Event")
                }
            }
        }
        return
    }

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    // Group events by date
    val grouped = remember(events) {
        events.groupBy { dayFormat.format(Date(it.instanceStartTime)) }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
        modifier = Modifier.fillMaxSize()
    ) {
        grouped.forEach { (dateKey, dayInstances) ->
            item(key = "header_$dateKey") {
                val date = dayFormat.parse(dateKey) ?: Date()
                Text(
                    text = dayHeaderFormat.format(date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimens.Spacing8, bottom = Dimens.Spacing4)
                )
            }

            items(dayInstances, key = { "${it.event.id}_${it.instanceStartTime}" }) { instance ->
                EventCardRow(
                    instance = instance,
                    onClick = { onEventClick(instance.event) },
                    onOpenNote = onOpenNote
                )
            }
        }
    }
}

@Composable
private fun EventCardRow(
    instance: CalendarEventInstance,
    onClick: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val eventColor = try {
        Color(android.graphics.Color.parseColor(instance.event.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Dimens.CornerXSmall))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(Dimens.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = instance.event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Surface(
                        shape = RoundedCornerShape(Dimens.CornerXSmall),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = instance.event.calendarId,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.Spacing4, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing4))

                // Time info
                val timeText = if (instance.event.isAllDay) {
                    "All Day"
                } else {
                    "${timeFormat.format(Date(instance.instanceStartTime))} – ${timeFormat.format(Date(instance.instanceEndTime))}"
                }

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Linked note chip
                val notePath = instance.event.linkedNotePath
                if (!notePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.Spacing4))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.CornerXSmall))
                            .clickable { onOpenNote(notePath) }
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = Dimens.Spacing8, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Spacing4))
                        Text(
                            text = notePath,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
