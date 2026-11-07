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
            try {
                val notes = withContext(Dispatchers.IO) {
                    vaultRepository.getIndexManager().getAllNotes()
                }
                val count = notes.size
                val size = notes.sumOf { it.size.coerceAtLeast(0L) }
                _vaultStats.value = VaultStats(noteCount = count, totalSizeBytes = size)
            } catch (e: Exception) {
                e.printStackTrace()
                _vaultStats.value = VaultStats()
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(mode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setDynamicColor(enabled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setPureBlack(enabled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setFontSize(size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setNoteLayoutMode(mode: NoteLayoutMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setNoteLayoutMode(mode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTemplatesFolder(folder: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setTemplatesFolder(folder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDailyNotesFolder(folder: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setDailyNotesFolder(folder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setAutoSave(enabled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setBiometricLock(enabled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCalendarDefaultView(view: CalendarDefaultView) {
        viewModelScope.launch {
            try {
                settingsRepository.setCalendarDefaultView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCalendarFirstDayOfWeek(firstDay: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setCalendarFirstDayOfWeek(firstDay)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCalendarDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setCalendarDefaultReminderMinutes(minutes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCalendarDefaultCalendar(name: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setCalendarDefaultCalendar(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
