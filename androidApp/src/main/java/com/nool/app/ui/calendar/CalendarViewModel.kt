package com.nool.app.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.index.IndexManager
import com.nool.app.data.index.NoteEntity
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.CalendarDefaultView
import com.nool.app.settings.SettingsRepository
import com.nool.core.model.CalendarEvent
import com.nool.core.model.CalendarEventInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val label: String) {
    MONTH("Month"),
    WEEK("Week"),
    DAY("Day"),
    AGENDA("Agenda")
}

data class CalendarUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val events: List<CalendarEventInstance> = emptyList(),
    val activeCalendars: Set<String> = setOf("Personal", "Work", "Ideas"),
    val existingNoteDates: Set<String> = emptySet(),
    val editingEvent: CalendarEvent? = null,
    val isEditSheetOpen: Boolean = false,
    val isAgendaSheetOpen: Boolean = false,
    val selectedDayEvents: List<CalendarEventInstance> = emptyList(),
    val allVaultNotes: List<NoteEntity> = emptyList(),
    val firstDayOfWeek: Int = Calendar.MONDAY
)

class CalendarViewModel(
    private val indexManager: IndexManager,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        viewModelScope.launch {
            settingsRepository.calendarDefaultView.collect { defaultView ->
                val mode = when (defaultView) {
                    CalendarDefaultView.MONTH -> CalendarViewMode.MONTH
                    CalendarDefaultView.WEEK -> CalendarViewMode.WEEK
                    CalendarDefaultView.DAY -> CalendarViewMode.DAY
                    CalendarDefaultView.AGENDA -> CalendarViewMode.AGENDA
                }
                _uiState.update { it.copy(viewMode = mode) }
            }
        }

        viewModelScope.launch {
            settingsRepository.calendarFirstDayOfWeek.collect { firstDay ->
                _uiState.update { it.copy(firstDayOfWeek = firstDay) }
            }
        }

        loadNoteDates()
        loadEventsForCurrentRange()
    }

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        loadEventsForCurrentRange()
    }

    fun selectDate(dateMillis: Long) {
        _uiState.update { it.copy(selectedDate = dateMillis) }
        loadEventsForCurrentRange()
        updateSelectedDayEvents(dateMillis)
    }

    fun goToToday() {
        selectDate(System.currentTimeMillis())
    }

    fun navigatePeriod(delta: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _uiState.value.selectedDate }
        when (_uiState.value.viewMode) {
            CalendarViewMode.MONTH -> cal.add(Calendar.MONTH, delta)
            CalendarViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, delta)
            CalendarViewMode.DAY, CalendarViewMode.AGENDA -> cal.add(Calendar.DAY_OF_YEAR, delta)
        }
        selectDate(cal.timeInMillis)
    }

    fun openCreateEvent(initialDate: Long = _uiState.value.selectedDate) {
        _uiState.update {
            it.copy(
                editingEvent = null,
                selectedDate = initialDate,
                isEditSheetOpen = true
            )
        }
    }

    fun openEditEvent(event: CalendarEvent) {
        _uiState.update {
            it.copy(
                editingEvent = event,
                isEditSheetOpen = true
            )
        }
    }

    fun dismissEditSheet() {
        _uiState.update { it.copy(isEditSheetOpen = false, editingEvent = null) }
    }

    fun openAgendaSheetForDay(dateMillis: Long) {
        selectDate(dateMillis)
        updateSelectedDayEvents(dateMillis)
        _uiState.update { it.copy(isAgendaSheetOpen = true) }
    }

    fun dismissAgendaSheet() {
        _uiState.update { it.copy(isAgendaSheetOpen = false) }
    }

    fun toggleCalendarFilter(calendarId: String) {
        _uiState.update { state ->
            val updated = if (state.activeCalendars.contains(calendarId)) {
                if (state.activeCalendars.size > 1) state.activeCalendars - calendarId else state.activeCalendars
            } else {
                state.activeCalendars + calendarId
            }
            state.copy(activeCalendars = updated)
        }
        loadEventsForCurrentRange()
    }

    fun saveEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    indexManager.upsertCalendarEvent(event)
                    CalendarReminderScheduler.scheduleReminder(context, event)
                }
                loadEventsForCurrentRange()
                updateSelectedDayEvents(_uiState.value.selectedDate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    indexManager.deleteCalendarEvent(eventId)
                    CalendarReminderScheduler.cancelReminder(context, eventId)
                }
                loadEventsForCurrentRange()
                updateSelectedDayEvents(_uiState.value.selectedDate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createNoteForEvent(title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val noteTitle = "Event - ${title.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "_")}"
                val initialContent = buildString {
                    appendLine("---")
                    appendLine("title: \"$title\"")
                    appendLine("tags: [event, calendar]")
                    appendLine("created: ${dayKeyFormat.format(Date())}")
                    appendLine("---")
                    appendLine()
                    appendLine("# $title")
                    appendLine()
                    appendLine("## Agenda & Notes")
                    appendLine("- ")
                }
                val result = vaultRepository.createNote("Notes", noteTitle, initialContent)
                val createdPath = result.getOrNull()?.relativePath ?: "Notes/$noteTitle.md"
                loadNoteDates()
                onCreated(createdPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadEventsForCurrentRange() {
        viewModelScope.launch {
            try {
                val (startRange, endRange) = calculateRangeForMode(_uiState.value.selectedDate, _uiState.value.viewMode)
                val instances = withContext(Dispatchers.IO) {
                    indexManager.getCalendarEventsInRange(startRange, endRange)
                }
                val filtered = instances.filter { _uiState.value.activeCalendars.contains(it.event.calendarId) }
                _uiState.update { it.copy(events = filtered) }
                updateSelectedDayEvents(_uiState.value.selectedDate)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(events = emptyList(), selectedDayEvents = emptyList()) }
            }
        }
    }

    private fun updateSelectedDayEvents(dateMillis: Long) {
        try {
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val startOfDay = Calendar.getInstance().apply {
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + 86_400_000L - 1

            val matching = _uiState.value.events.filter { instance ->
                instance.instanceEndTime >= startOfDay && instance.instanceStartTime <= endOfDay
            }
            _uiState.update { it.copy(selectedDayEvents = matching) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadNoteDates() {
        viewModelScope.launch {
            try {
                val allNotes = withContext(Dispatchers.IO) { indexManager.getAllNotes() }
                val dates = allNotes.mapNotNull { note ->
                    // Check note relative path or lastModified
                    val match = Regex("""\b(\d{4}-\d{2}-\d{2})\b""").find(note.relativePath)
                    match?.value ?: dayKeyFormat.format(Date(note.createdTimestamp.takeIf { it > 0 } ?: note.lastModified))
                }.toSet()
                _uiState.update { it.copy(existingNoteDates = dates) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateRangeForMode(selectedDate: Long, mode: CalendarViewMode): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        return when (mode) {
            CalendarViewMode.MONTH -> {
                // Buffer by 2 weeks before and after for smooth swipe
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.WEEK_OF_YEAR, -2)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 8)
                val end = cal.timeInMillis
                start to end
            }
            CalendarViewMode.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, _uiState.value.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val end = cal.timeInMillis
                start to end
            }
            CalendarViewMode.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                val end = start + 86_400_000L
                start to end
            }
            CalendarViewMode.AGENDA -> {
                // Upcoming 60 days
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 60)
                val end = cal.timeInMillis
                start to end
            }
        }
    }
}
