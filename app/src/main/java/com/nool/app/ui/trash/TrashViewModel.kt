package com.nool.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.domain.model.TrashedItem
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<TrashedItem> = emptyList(),
    val isLoading: Boolean = false,
    val showEmptyConfirm: Boolean = false,
    val itemToDelete: TrashedItem? = null
)

sealed interface TrashUiEvent {
    data class ShowMessage(val message: String) : TrashUiEvent
}

class TrashViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TrashUiEvent>()
    val events: SharedFlow<TrashUiEvent> = _events.asSharedFlow()

    init {
        loadTrash()
        viewModelScope.launch {
            repository.vaultUpdates.collect {
                loadTrash()
            }
        }
    }

    fun loadTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.listTrash()
                .onSuccess { list ->
                    _uiState.update { it.copy(items = list, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(TrashUiEvent.ShowMessage("Failed to load trash: ${error.message}"))
                }
        }
    }

    fun restoreItem(item: TrashedItem) {
        viewModelScope.launch {
            repository.restoreFromTrash(item.id)
                .onSuccess { restoredPath ->
                    _events.emit(TrashUiEvent.ShowMessage("Restored to $restoredPath"))
                    loadTrash()
                }
                .onFailure { error ->
                    _events.emit(TrashUiEvent.ShowMessage("Failed to restore: ${error.message}"))
                }
        }
    }

    fun showDeleteConfirm(item: TrashedItem) {
        _uiState.update { it.copy(itemToDelete = item) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(itemToDelete = null) }
    }

    fun deletePermanently() {
        val item = _uiState.value.itemToDelete ?: return
        dismissDeleteConfirm()
        viewModelScope.launch {
            repository.permanentlyDeleteFromTrash(item.id)
                .onSuccess {
                    _events.emit(TrashUiEvent.ShowMessage("Permanently deleted ${item.name}"))
                    loadTrash()
                }
                .onFailure { error ->
                    _events.emit(TrashUiEvent.ShowMessage("Failed to delete: ${error.message}"))
                }
        }
    }

    fun showEmptyTrashConfirm() {
        _uiState.update { it.copy(showEmptyConfirm = true) }
    }

    fun dismissEmptyTrashConfirm() {
        _uiState.update { it.copy(showEmptyConfirm = false) }
    }

    fun emptyTrash() {
        dismissEmptyTrashConfirm()
        viewModelScope.launch {
            repository.emptyTrash()
                .onSuccess {
                    _events.emit(TrashUiEvent.ShowMessage("Trash emptied"))
                    loadTrash()
                }
                .onFailure { error ->
                    _events.emit(TrashUiEvent.ShowMessage("Failed to empty trash: ${error.message}"))
                }
        }
    }
}
