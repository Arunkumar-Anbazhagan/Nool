package com.nool.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiEvent {
    data class ShowMessage(val message: String) : SettingsUiEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settingsRepository.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fontSize: StateFlow<Int> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    val templatesFolder: StateFlow<String> = settingsRepository.templatesFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Templates")

    val dailyNotesFolder: StateFlow<String> = settingsRepository.dailyNotesFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Daily Notes")

    private val _isRebuildingIndex = MutableStateFlow(false)
    val isRebuildingIndex: StateFlow<Boolean> = _isRebuildingIndex.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

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

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
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

    fun rebuildIndex() {
        viewModelScope.launch {
            _isRebuildingIndex.value = true
            vaultRepository.rebuildIndex()
                .onSuccess { count ->
                    _isRebuildingIndex.value = false
                    _events.emit(SettingsUiEvent.ShowMessage("Rebuilt index for $count notes."))
                }
                .onFailure { error ->
                    _isRebuildingIndex.value = false
                    _events.emit(SettingsUiEvent.ShowMessage("Index rebuild failed: ${error.message}"))
                }
        }
    }

    fun getVaultPath(): String = vaultRepository.getVaultRootDir()
}
