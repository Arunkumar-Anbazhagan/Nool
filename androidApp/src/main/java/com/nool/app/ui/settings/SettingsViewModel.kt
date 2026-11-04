package com.nool.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.CalendarDefaultView
import com.nool.app.settings.NoteLayoutMode
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed interface SettingsUiEvent {
    data class ShowMessage(val message: String) : SettingsUiEvent
}

data class VaultStats(
    val noteCount: Int = 0,
    val totalSizeBytes: Long = 0L
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settingsRepository.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pureBlack: StateFlow<Boolean> = settingsRepository.pureBlack
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fontSize: StateFlow<Int> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    val noteLayoutMode: StateFlow<NoteLayoutMode> = settingsRepository.noteLayoutMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NoteLayoutMode.LIST)

    val templatesFolder: StateFlow<String> = settingsRepository.templatesFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Templates")

    val dailyNotesFolder: StateFlow<String> = settingsRepository.dailyNotesFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Daily Notes")

    val autoSave: StateFlow<Boolean> = settingsRepository.autoSave
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val biometricLock: StateFlow<Boolean> = settingsRepository.biometricLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val calendarDefaultView: StateFlow<CalendarDefaultView> = settingsRepository.calendarDefaultView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarDefaultView.MONTH)

    val calendarFirstDayOfWeek: StateFlow<Int> = settingsRepository.calendarFirstDayOfWeek
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.MONDAY)

    val calendarDefaultReminderMinutes: StateFlow<Int> = settingsRepository.calendarDefaultReminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val calendarDefaultCalendar: StateFlow<String> = settingsRepository.calendarDefaultCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Personal")

    private val _vaultStats = MutableStateFlow(VaultStats())
    val vaultStats: StateFlow<VaultStats> = _vaultStats.asStateFlow()

    private val _isRebuildingIndex = MutableStateFlow(false)
    val isRebuildingIndex: StateFlow<Boolean> = _isRebuildingIndex.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    init {
        loadVaultStats()
    }

    fun loadVaultStats() {
        viewModelScope.launch {
            val notes = withContext(Dispatchers.IO) {
                vaultRepository.getIndexManager().getAllNotes()
            }
            val count = notes.size
            val size = notes.sumOf { it.size }
            _vaultStats.value = VaultStats(noteCount = count, totalSizeBytes = size)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPureBlack(enabled)
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }

    fun setNoteLayoutMode(mode: NoteLayoutMode) {
        viewModelScope.launch {
            settingsRepository.setNoteLayoutMode(mode)
        }
    }

    fun setTemplatesFolder(folder: String) {
        viewModelScope.launch {
            settingsRepository.setTemplatesFolder(folder)
        }
    }

    fun setDailyNotesFolder(folder: String) {
        viewModelScope.launch {
            settingsRepository.setDailyNotesFolder(folder)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSave(enabled)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLock(enabled)
        }
    }

    fun setCalendarDefaultView(view: CalendarDefaultView) {
        viewModelScope.launch {
            settingsRepository.setCalendarDefaultView(view)
        }
    }

    fun setCalendarFirstDayOfWeek(firstDay: Int) {
        viewModelScope.launch {
            settingsRepository.setCalendarFirstDayOfWeek(firstDay)
        }
    }

    fun setCalendarDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setCalendarDefaultReminderMinutes(minutes)
        }
    }

    fun setCalendarDefaultCalendar(name: String) {
        viewModelScope.launch {
            settingsRepository.setCalendarDefaultCalendar(name)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _events.emit(SettingsUiEvent.ShowMessage("Temporary cache cleared."))
            loadVaultStats()
        }
    }

    fun rebuildIndex() {
        viewModelScope.launch {
            _isRebuildingIndex.value = true
            vaultRepository.rebuildIndex()
                .onSuccess { count ->
                    _isRebuildingIndex.value = false
                    _events.emit(SettingsUiEvent.ShowMessage("Rebuilt index for $count notes."))
                    loadVaultStats()
                }
                .onFailure { error ->
                    _isRebuildingIndex.value = false
                    _events.emit(SettingsUiEvent.ShowMessage("Index rebuild failed: ${error.message}"))
                }
        }
    }
}
