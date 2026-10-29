package com.nool.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.index.SearchScope
import com.nool.app.domain.model.VaultItem
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.SortOption
import com.nool.app.ui.sidebar.FolderTreeBuilder
import com.nool.app.ui.sidebar.TreeNode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Breadcrumb(
    val name: String,
    val relativePath: String
)

data class NotesUiState(
    val currentFolder: String = "",
    val breadcrumbs: List<Breadcrumb> = listOf(Breadcrumb("Vault", "")),
    val items: List<VaultItem> = emptyList(),
    val sortOption: SortOption = SortOption.MODIFIED_DESC,
    val showArchived: Boolean = false,
    val isLoading: Boolean = false,
    val activeDialog: NotesDialogState? = null
)

sealed interface NotesDialogState {
    data object CreateNote : NotesDialogState
    data object CreateFolder : NotesDialogState
    data class Rename(val item: VaultItem) : NotesDialogState
    data class Move(val item: VaultItem, val folders: List<String>) : NotesDialogState
    data class Delete(val item: VaultItem) : NotesDialogState
    data object SortFilter : NotesDialogState
}

sealed interface NotesUiEvent {
    data class ShowMessage(val message: String) : NotesUiEvent
    data object OpenSearch : NotesUiEvent
    data object OpenSettings : NotesUiEvent
    data class OpenNote(val relativePath: String) : NotesUiEvent
}

class NotesViewModel(
    private val repository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    initialFolder: String = "",
    private val templateManager: com.nool.app.data.templates.TemplateManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(currentFolder = initialFolder, isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events: SharedFlow<NotesUiEvent> = _events.asSharedFlow()

    private val _allVaultNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val allVaultNotes: StateFlow<List<NoteEntity>> = _allVaultNotes.asStateFlow()

    private val _vaultTags = MutableStateFlow<Map<String, Int>>(emptyMap())
    val vaultTags: StateFlow<Map<String, Int>> = _vaultTags.asStateFlow()

    private val _folderTree = MutableStateFlow<TreeNode>(TreeNode("Vault", "", true))
    val folderTree: StateFlow<TreeNode> = _folderTree.asStateFlow()

    private val _savedSearches = MutableStateFlow<List<com.nool.app.settings.SavedSearch>>(emptyList())
    val savedSearches: StateFlow<List<com.nool.app.settings.SavedSearch>> = _savedSearches.asStateFlow()

    init {
        setFolder(initialFolder)
        loadSidebarData()
        viewModelScope.launch {
            repository.vaultUpdates.collect {
                loadCurrentFolder()
                loadSidebarData()
            }
        }
        viewModelScope.launch {
            settingsRepository.sortOption.collect { sort ->
                _uiState.update { it.copy(sortOption = sort) }
                loadCurrentFolder()
            }
        }
        viewModelScope.launch {
            settingsRepository.showArchived.collect { show ->
                _uiState.update { it.copy(showArchived = show) }
                loadCurrentFolder()
            }
        }
        viewModelScope.launch {
            settingsRepository.savedSearches.collect { searches ->
                _savedSearches.value = searches
            }
        }
    }

    fun deleteSavedSearch(id: String) {
        viewModelScope.launch {
            settingsRepository.deleteSavedSearch(id)
        }
    }

    fun openOrCreateDailyNote() {
        viewModelScope.launch {
            if (templateManager != null) {
                templateManager.getOrCreateDailyNote()
                    .onSuccess { path ->
                        _events.emit(NotesUiEvent.OpenNote(path))
                    }
                    .onFailure { error ->
                        _events.emit(NotesUiEvent.ShowMessage("Failed to open daily note: ${error.message}"))
                    }
            } else {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                val note = repository.createNote("", today, "# $today\n\n").getOrNull()
                if (note != null) {
                    _events.emit(NotesUiEvent.OpenNote(note.relativePath))
                }
            }
        }
    }

    fun setFolder(folderPath: String) {
        val cleanPath = folderPath.trim().removePrefix("/").removeSuffix("/")
        val crumbs = buildBreadcrumbs(cleanPath)
        _uiState.update { it.copy(currentFolder = cleanPath, breadcrumbs = crumbs) }
        loadCurrentFolder()
    }

    private fun buildBreadcrumbs(folderPath: String): List<Breadcrumb> {
        val list = mutableListOf(Breadcrumb("Vault", ""))
        if (folderPath.isEmpty()) return list

        val segments = folderPath.split("/")
        var accumulated = ""
        for (segment in segments) {
            accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
            list.add(Breadcrumb(segment, accumulated))
        }
        return list
    }

    fun loadCurrentFolder() {
        val folder = _uiState.value.currentFolder
        val sort = _uiState.value.sortOption
        val showArchived = _uiState.value.showArchived

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.listItems(folder)
                .onSuccess { rawItems ->
                    val filtered = rawItems.filter { item ->
                        if (item is VaultItem.NoteItem) {
                            showArchived || !item.isArchived
                        } else {
                            true
                        }
                    }

                    // Partition folders vs notes
                    val folders = filtered.filterIsInstance<VaultItem.FolderItem>().sortedBy { it.name.lowercase() }
                    val notes = filtered.filterIsInstance<VaultItem.NoteItem>()

                    // Sort notes based on SortOption
                    val sortedNotes = when (sort) {
                        SortOption.MODIFIED_DESC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenByDescending { it.lastModified })
                        SortOption.MODIFIED_ASC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenBy { it.lastModified })
                        SortOption.CREATED_DESC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenByDescending { it.createdTimestamp })
                        SortOption.CREATED_ASC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenBy { it.createdTimestamp })
                        SortOption.TITLE_ASC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenBy { it.title.lowercase() })
                        SortOption.TITLE_DESC -> notes.sortedWith(compareByDescending<VaultItem.NoteItem> { it.isPinned }.thenByDescending { it.title.lowercase() })
                    }

                    _uiState.update { it.copy(items = folders + sortedNotes, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(NotesUiEvent.ShowMessage("Failed to list items: ${error.message}"))
                }
        }
    }

    fun loadSidebarData() {
        viewModelScope.launch {
            val notesResult = repository.searchNotes("", scope = SearchScope.ALL)
            val foldersResult = repository.getAllFolders()
            val tagsResult = repository.getAllTags()

            val notes = notesResult.getOrDefault(emptyList())
            val folders = foldersResult.getOrDefault(emptyList())
            val tags = tagsResult.getOrDefault(emptyMap())

            _allVaultNotes.value = notes
            _vaultTags.value = tags
            _folderTree.value = FolderTreeBuilder.buildTree(notes, folders)
        }
    }

    fun togglePin(item: VaultItem.NoteItem) {
        viewModelScope.launch {
            val next = !item.isPinned
            repository.setNoteFlag(item.relativePath, "pinned", next)
                .onSuccess {
                    _events.emit(NotesUiEvent.ShowMessage(if (next) "Pinned note" else "Unpinned note"))
                    loadCurrentFolder()
                }
        }
    }

    fun toggleFavorite(item: VaultItem.NoteItem) {
        viewModelScope.launch {
            val next = !item.isFavorite
            repository.setNoteFlag(item.relativePath, "favorite", next)
                .onSuccess {
                    _events.emit(NotesUiEvent.ShowMessage(if (next) "Added to Favorites" else "Removed from Favorites"))
                    loadCurrentFolder()
                }
        }
    }

    fun toggleArchive(item: VaultItem.NoteItem) {
        viewModelScope.launch {
            val next = !item.isArchived
            repository.setNoteFlag(item.relativePath, "archived", next)
                .onSuccess {
                    _events.emit(NotesUiEvent.ShowMessage(if (next) "Archived note" else "Unarchived note"))
                    loadCurrentFolder()
                }
        }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch {
            settingsRepository.setSortOption(option)
        }
    }

    fun setShowArchived(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowArchived(show)
        }
    }

    fun showSortFilterDialog() {
        _uiState.update { it.copy(activeDialog = NotesDialogState.SortFilter) }
    }

    fun showCreateNoteDialog() {
        _uiState.update { it.copy(activeDialog = NotesDialogState.CreateNote) }
    }

    fun showCreateFolderDialog() {
        _uiState.update { it.copy(activeDialog = NotesDialogState.CreateFolder) }
    }

    fun showRenameDialog(item: VaultItem) {
        _uiState.update { it.copy(activeDialog = NotesDialogState.Rename(item)) }
    }

    fun showMoveDialog(item: VaultItem) {
        viewModelScope.launch {
            repository.getAllFolders().onSuccess { folders ->
                _uiState.update {
                    it.copy(activeDialog = NotesDialogState.Move(item, folders))
                }
            }.onFailure {
                _events.emit(NotesUiEvent.ShowMessage("Failed to load folders: ${it.message}"))
            }
        }
    }

    fun showDeleteDialog(item: VaultItem) {
        _uiState.update { it.copy(activeDialog = NotesDialogState.Delete(item)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun createNote(title: String) {
        dismissDialog()
        viewModelScope.launch {
            repository.createNote(_uiState.value.currentFolder, title)
                .onSuccess { note ->
                    _events.emit(NotesUiEvent.OpenNote(note.relativePath))
                }
                .onFailure { error ->
                    _events.emit(NotesUiEvent.ShowMessage("Failed to create note: ${error.message}"))
                }
        }
    }

    fun createFolder(name: String) {
        dismissDialog()
        viewModelScope.launch {
            repository.createFolder(_uiState.value.currentFolder, name)
                .onSuccess {
                    loadCurrentFolder()
                }
                .onFailure { error ->
                    _events.emit(NotesUiEvent.ShowMessage("Failed to create folder: ${error.message}"))
                }
        }
    }

    fun renameItem(item: VaultItem, newName: String) {
        dismissDialog()
        viewModelScope.launch {
            repository.renameItem(item.relativePath, newName)
                .onSuccess {
                    loadCurrentFolder()
                }
                .onFailure { error ->
                    _events.emit(NotesUiEvent.ShowMessage("Failed to rename: ${error.message}"))
                }
        }
    }

    fun moveItem(item: VaultItem, targetFolder: String) {
        dismissDialog()
        viewModelScope.launch {
            repository.moveItem(item.relativePath, targetFolder)
                .onSuccess {
                    loadCurrentFolder()
                }
                .onFailure { error ->
                    _events.emit(NotesUiEvent.ShowMessage("Failed to move: ${error.message}"))
                }
        }
    }

    fun deleteItem(item: VaultItem) {
        dismissDialog()
        viewModelScope.launch {
            repository.moveToTrash(item.relativePath)
                .onSuccess {
                    loadCurrentFolder()
                    _events.emit(NotesUiEvent.ShowMessage("Moved '${item.name}' to Trash"))
                }
                .onFailure { error ->
                    _events.emit(NotesUiEvent.ShowMessage("Failed to move to trash: ${error.message}"))
                }
        }
    }
}
