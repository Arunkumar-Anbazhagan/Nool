package com.nool.app.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.share.ShareManager
import com.nool.app.data.share.ShareTarget
import com.nool.app.data.share.SharedContent
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShareUiState(
    val sharedContent: SharedContent? = null,
    val isAppendMode: Boolean = false,
    val noteTitle: String = "",
    val selectedFolder: String = "",
    val availableFolders: List<String> = emptyList(),
    val availableNotes: List<NoteEntity> = emptyList(),
    val selectedNotePath: String = "",
    val isSaving: Boolean = false
)

sealed interface ShareEvent {
    data class Success(val notePath: String) : ShareEvent
    data class Error(val message: String) : ShareEvent
}

class ShareViewModel(
    private val shareManager: ShareManager,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShareEvent>()
    val events: SharedFlow<ShareEvent> = _events.asSharedFlow()

    fun setSharedContent(content: SharedContent) {
        val defaultTitle = content.subject?.takeIf { it.isNotBlank() }
            ?: "Shared ${SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault()).format(Date())}"
        _uiState.update {
            it.copy(
                sharedContent = content,
                noteTitle = defaultTitle,
                isAppendMode = false
            )
        }
        loadFoldersAndNotes()
    }

    private fun loadFoldersAndNotes() {
        viewModelScope.launch {
            val folders = vaultRepository.getAllFolders().getOrDefault(listOf(""))
            val notes = vaultRepository.searchNotes("").getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    availableFolders = folders,
                    availableNotes = notes,
                    selectedNotePath = notes.firstOrNull()?.relativePath.orEmpty()
                )
            }
        }
    }

    fun setAppendMode(append: Boolean) {
        _uiState.update { it.copy(isAppendMode = append) }
    }

    fun setNoteTitle(title: String) {
        _uiState.update { it.copy(noteTitle = title) }
    }

    fun setSelectedFolder(folder: String) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun setSelectedNotePath(path: String) {
        _uiState.update { it.copy(selectedNotePath = path) }
    }

    fun dismiss() {
        _uiState.update { it.copy(sharedContent = null) }
    }

    fun save() {
        val content = _uiState.value.sharedContent ?: return
        val target = if (_uiState.value.isAppendMode) {
            val path = _uiState.value.selectedNotePath
            if (path.isBlank()) {
                viewModelScope.launch { _events.emit(ShareEvent.Error("Please select a note to append to")) }
                return
            }
            ShareTarget.AppendToNote(path)
        } else {
            ShareTarget.NewNote(
                title = _uiState.value.noteTitle,
                folder = _uiState.value.selectedFolder
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            shareManager.saveSharedContent(content, target)
                .onSuccess { notePath ->
                    _uiState.update { it.copy(isSaving = false, sharedContent = null) }
                    _events.emit(ShareEvent.Success(notePath))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(ShareEvent.Error(error.message ?: "Failed to save shared content"))
                }
        }
    }
}
