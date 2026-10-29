package com.nool.app.ui.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.index.BacklinkItem
import com.nool.app.data.index.NoteTitleAndPath
import com.nool.app.data.index.UnlinkedMentionItem
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.ui.editor.MarkdownFormattingHelper
import com.nool.app.ui.editor.UndoRedoManager
import com.nool.app.ui.properties.FilePropertiesHelper
import com.nool.app.ui.properties.PropertyItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

enum class SaveStatus {
    Saved,
    Saving,
    Unsaved
}

data class NoteEditUiState(
    val relativePath: String = "",
    val title: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val initialContent: String = "",
    val isLoading: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Saved,
    val isPreviewMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val backlinks: List<BacklinkItem> = emptyList(),
    val unlinkedMentions: List<UnlinkedMentionItem> = emptyList(),
    val isBacklinksSheetOpen: Boolean = false,
    val showWikiLinkSuggestions: Boolean = false,
    val wikiLinkQuery: String = "",
    val wikiLinkSuggestions: List<NoteTitleAndPath> = emptyList(),
    val missingNoteToCreate: String? = null,
    val properties: List<PropertyItem> = emptyList(),
    val showVersionHistory: Boolean = false,
    val isVersionHistoryLoading: Boolean = false,
    val noteHistoryCommits: List<com.nool.app.data.git.GitCommit> = emptyList(),
    val previewCommit: com.nool.app.data.git.GitCommit? = null,
    val previewCommitContent: String? = null
) {
    val content: String get() = textFieldValue.text
    val isDirty: Boolean get() = textFieldValue.text != initialContent
}

sealed interface NoteEditEvent {
    data class ShowMessage(val message: String) : NoteEditEvent
    data object NoteDeleted : NoteEditEvent
    data class PathChanged(val newPath: String) : NoteEditEvent
    data class OpenNote(val relativePath: String) : NoteEditEvent
}

@OptIn(FlowPreview::class)
class NoteEditViewModel(
    private val repository: VaultRepository,
    private val initialPath: String,
    private val templateManager: com.nool.app.data.templates.TemplateManager? = null,
    private val gitRepository: com.nool.app.data.git.GitRepository? = null
) : ViewModel() {

    private val _templates = MutableStateFlow<List<com.nool.app.domain.model.VaultItem.NoteItem>>(emptyList())
    val templates: StateFlow<List<com.nool.app.domain.model.VaultItem.NoteItem>> = _templates.asStateFlow()

    fun openVersionHistory() {
        _uiState.update { it.copy(showVersionHistory = true, isVersionHistoryLoading = true) }
        viewModelScope.launch {
            if (gitRepository == null || !gitRepository.isRepo()) {
                _uiState.update { it.copy(isVersionHistoryLoading = false, noteHistoryCommits = emptyList()) }
                return@launch
            }
            gitRepository.getFileHistory(_uiState.value.relativePath)
                .onSuccess { commits ->
                    _uiState.update { it.copy(noteHistoryCommits = commits, isVersionHistoryLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isVersionHistoryLoading = false) }
                    _events.emit(NoteEditEvent.ShowMessage("Failed to load history: ${it.message}"))
                }
        }
    }

    fun closeVersionHistory() {
        _uiState.update {
            it.copy(
                showVersionHistory = false,
                previewCommit = null,
                previewCommitContent = null
            )
        }
    }

    fun viewCommitContent(commit: com.nool.app.data.git.GitCommit) {
        viewModelScope.launch {
            gitRepository?.getFileContentAtCommit(_uiState.value.relativePath, commit.hash)
                ?.onSuccess { content ->
                    _uiState.update {
                        it.copy(
                            previewCommit = commit,
                            previewCommitContent = content
                        )
                    }
                }
                ?.onFailure {
                    _events.emit(NoteEditEvent.ShowMessage("Failed to read commit content: ${it.message}"))
                }
        }
    }

    fun closeCommitPreview() {
        _uiState.update { it.copy(previewCommit = null, previewCommitContent = null) }
    }

    fun restoreVersion(commit: com.nool.app.data.git.GitCommit) {
        viewModelScope.launch {
            gitRepository?.restoreFileFromCommit(_uiState.value.relativePath, commit.hash)
                ?.onSuccess {
                    loadNote(_uiState.value.relativePath)
                    closeVersionHistory()
                    _events.emit(NoteEditEvent.ShowMessage("Restored note to commit ${commit.shortHash}"))
                }
                ?.onFailure {
                    _events.emit(NoteEditEvent.ShowMessage("Failed to restore: ${it.message}"))
                }
        }
    }

    fun loadTemplates() {
        viewModelScope.launch {
            templateManager?.let { tm ->
                _templates.value = tm.getTemplates()
            }
        }
    }

    fun insertTemplate(template: com.nool.app.domain.model.VaultItem.NoteItem) {
        viewModelScope.launch {
            templateManager?.applyTemplate(template.relativePath, _uiState.value.title)
                ?.onSuccess { text ->
                    insertTextAtCursor(if (_uiState.value.textFieldValue.text.isEmpty()) text else "\n$text\n")
                }
        }
    }

    fun insertTextAtCursor(textToInsert: String) {
        val current = _uiState.value.textFieldValue
        val start = current.selection.min
        val end = current.selection.max
        val newText = current.text.replaceRange(start, end, textToInsert)
        val newCursor = start + textToInsert.length
        val newTfv = TextFieldValue(newText, TextRange(newCursor))
        onTextFieldValueChange(newTfv)
    }

    private val undoRedoManager = UndoRedoManager()
    private val contentChangeFlow = MutableStateFlow("")
    private var cachedCatalog: List<NoteTitleAndPath> = emptyList()
    private val _allVaultNotes = MutableStateFlow<List<com.nool.app.data.index.NoteEntity>>(emptyList())
    val allVaultNotes: StateFlow<List<com.nool.app.data.index.NoteEntity>> = _allVaultNotes.asStateFlow()

    private val _uiState = MutableStateFlow(
        NoteEditUiState(
            relativePath = initialPath,
            title = initialPath.substringAfterLast("/").removeSuffix(".md")
        )
    )
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NoteEditEvent>()
    val events: SharedFlow<NoteEditEvent> = _events.asSharedFlow()

    init {
        loadNote(initialPath)
        loadCatalog()

        // Setup debounced auto-save (~500 ms)
        viewModelScope.launch {
            contentChangeFlow
                .debounce(500L)
                .distinctUntilChanged()
                .collect { changedText ->
                    val current = _uiState.value
                    if (current.isDirty && !current.isLoading) {
                        saveInternal(changedText)
                    }
                }
        }
    }

    fun getVaultRoot(): File {
        return File(repository.getVaultRootDir())
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            repository.getNoteCatalog().onSuccess { catalog ->
                cachedCatalog = catalog
            }
            repository.searchNotes("", scope = com.nool.app.data.index.SearchScope.ALL).onSuccess { notes ->
                _allVaultNotes.value = notes
            }
        }
    }

    fun loadNote(path: String) {
        val cleanPath = path.trim().removePrefix("/")
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    relativePath = cleanPath,
                    title = cleanPath.substringAfterLast("/").removeSuffix(".md"),
                    isLoading = true
                )
            }
            repository.readNoteContent(cleanPath)
                .onSuccess { content ->
                    val tfv = TextFieldValue(content, TextRange(content.length))
                    undoRedoManager.reset(tfv)
                    val (parsedProps, _) = FilePropertiesHelper.parseProperties(content)
                    _uiState.update {
                        it.copy(
                            textFieldValue = tfv,
                            initialContent = content,
                            properties = parsedProps,
                            isLoading = false,
                            saveStatus = SaveStatus.Saved,
                            canUndo = undoRedoManager.canUndo,
                            canRedo = undoRedoManager.canRedo
                        )
                    }
                    loadBacklinksAndMentions(cleanPath)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(NoteEditEvent.ShowMessage("Failed to load note: ${error.message}"))
                }
        }
    }

    fun loadBacklinksAndMentions(notePath: String = _uiState.value.relativePath) {
        viewModelScope.launch {
            repository.getBacklinks(notePath).onSuccess { backlinks ->
                _uiState.update { it.copy(backlinks = backlinks) }
            }
            repository.getUnlinkedMentions(notePath).onSuccess { mentions ->
                _uiState.update { it.copy(unlinkedMentions = mentions) }
            }
        }
    }

    fun onTextFieldValueChange(newVal: TextFieldValue) {
        val oldVal = _uiState.value.textFieldValue
        val isTextChanged = newVal.text != oldVal.text

        if (isTextChanged) {
            undoRedoManager.push(newVal)
            contentChangeFlow.value = newVal.text
        }

        // Detect [[ autocomplete trigger
        val cursor = newVal.selection.start
        val textUpToCursor = newVal.text.substring(0, cursor)
        val lastOpen = textUpToCursor.lastIndexOf("[[")

        var showSuggestions = false
        var query = ""
        var suggestions = emptyList<NoteTitleAndPath>()

        if (lastOpen != -1) {
            val textBetween = textUpToCursor.substring(lastOpen + 2)
            // Autocomplete active if no newline and no closing bracket in between
            if (!textBetween.contains("\n") && !textBetween.contains("]]")) {
                query = textBetween.trim()
                showSuggestions = true
                suggestions = cachedCatalog.filter { item ->
                    item.relativePath != _uiState.value.relativePath && (
                        item.title.contains(query, ignoreCase = true) ||
                        item.aliases.split(",").any { it.trim().contains(query, ignoreCase = true) } ||
                        item.relativePath.contains(query, ignoreCase = true)
                    )
                }.take(8)
            }
        }

        val (parsedProps, _) = if (isTextChanged) FilePropertiesHelper.parseProperties(newVal.text) else (_uiState.value.properties to "")

        _uiState.update {
            it.copy(
                textFieldValue = newVal,
                properties = parsedProps,
                saveStatus = if (isTextChanged) SaveStatus.Unsaved else it.saveStatus,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo,
                showWikiLinkSuggestions = showSuggestions,
                wikiLinkQuery = query,
                wikiLinkSuggestions = suggestions
            )
        }
    }

    fun updateProperties(newProps: List<PropertyItem>) {
        val currentText = _uiState.value.textFieldValue.text
        val newContent = FilePropertiesHelper.updateProperties(currentText, newProps)
        val newTfv = TextFieldValue(newContent, TextRange(newContent.length))
        onTextFieldValueChange(newTfv)
    }

    fun insertWikiLinkSuggestion(note: NoteTitleAndPath) {
        val current = _uiState.value.textFieldValue
        val cursor = current.selection.start
        val textUpToCursor = current.text.substring(0, cursor)
        val lastOpen = textUpToCursor.lastIndexOf("[[")
        if (lastOpen == -1) return

        val textAfterCursor = current.text.substring(cursor)
        val hasClosingBrackets = textAfterCursor.startsWith("]]")

        val before = current.text.substring(0, lastOpen)
        val insertion = "[[${note.title}]]"
        val after = if (hasClosingBrackets) textAfterCursor.removePrefix("]]") else textAfterCursor

        val newText = before + insertion + after
        val newCursor = before.length + insertion.length
        val newTfv = TextFieldValue(newText, TextRange(newCursor))

        undoRedoManager.push(newTfv)
        contentChangeFlow.value = newText
        _uiState.update {
            it.copy(
                textFieldValue = newTfv,
                saveStatus = SaveStatus.Unsaved,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo,
                showWikiLinkSuggestions = false
            )
        }
    }

    private suspend fun saveInternal(contentToSave: String) {
        val current = _uiState.value
        _uiState.update { it.copy(saveStatus = SaveStatus.Saving) }
        repository.saveNoteContent(current.relativePath, contentToSave)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        initialContent = contentToSave,
                        saveStatus = SaveStatus.Saved
                    )
                }
                loadBacklinksAndMentions(current.relativePath)
            }
            .onFailure { error ->
                _uiState.update { it.copy(saveStatus = SaveStatus.Unsaved) }
                _events.emit(NoteEditEvent.ShowMessage("Auto-save failed: ${error.message}"))
            }
    }

    fun manualSave() {
        viewModelScope.launch {
            val content = _uiState.value.textFieldValue.text
            saveInternal(content)
            _events.emit(NoteEditEvent.ShowMessage("Note saved"))
        }
    }

    fun saveImmediately() {
        viewModelScope.launch {
            val content = _uiState.value.textFieldValue.text
            saveInternal(content)
        }
    }

    fun flushSave() {
        val current = _uiState.value
        if (current.isDirty) {
            viewModelScope.launch {
                repository.saveNoteContent(current.relativePath, current.textFieldValue.text)
            }
        }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun toggleBacklinksSheet() {
        val next = !_uiState.value.isBacklinksSheetOpen
        if (next) {
            loadBacklinksAndMentions()
        }
        _uiState.update { it.copy(isBacklinksSheetOpen = next) }
    }

    fun dismissBacklinksSheet() {
        _uiState.update { it.copy(isBacklinksSheetOpen = false) }
    }

    fun convertUnlinkedMention(mention: UnlinkedMentionItem) {
        viewModelScope.launch {
            repository.convertUnlinkedMention(mention.sourcePath, mention.matchedText, _uiState.value.title)
                .onSuccess {
                    _events.emit(NoteEditEvent.ShowMessage("Converted mention in '${mention.sourceTitle}' to link"))
                    loadBacklinksAndMentions()
                }
                .onFailure {
                    _events.emit(NoteEditEvent.ShowMessage("Failed to convert: ${it.message}"))
                }
        }
    }

    fun handleWikiLinkClick(rawTarget: String) {
        viewModelScope.launch {
            val currentPath = _uiState.value.relativePath
            repository.resolveWikiLink(rawTarget, currentPath)
                .onSuccess { resolvedPath ->
                    if (resolvedPath != null) {
                        _events.emit(NoteEditEvent.OpenNote(resolvedPath))
                    } else {
                        // Broken link: ask to create
                        val targetTitle = rawTarget.substringBefore("#").substringBefore("|").trim()
                        _uiState.update { it.copy(missingNoteToCreate = targetTitle) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(missingNoteToCreate = rawTarget) }
                }
        }
    }

    fun dismissMissingNoteDialog() {
        _uiState.update { it.copy(missingNoteToCreate = null) }
    }

    fun createMissingNote(title: String) {
        dismissMissingNoteDialog()
        val currentFolder = File(_uiState.value.relativePath).parent?.replace('\\', '/') ?: ""
        viewModelScope.launch {
            repository.createNote(currentFolder, title)
                .onSuccess { newNote ->
                    loadCatalog()
                    _events.emit(NoteEditEvent.ShowMessage("Created note '${newNote.name}'"))
                    _events.emit(NoteEditEvent.OpenNote(newNote.relativePath))
                }
                .onFailure {
                    _events.emit(NoteEditEvent.ShowMessage("Failed to create note: ${it.message}"))
                }
        }
    }

    // Markdown formatting helper actions
    fun applyFormat(formatter: (TextFieldValue) -> TextFieldValue) {
        val newVal = formatter(_uiState.value.textFieldValue)
        onTextFieldValueChange(newVal)
    }

    fun formatHeading() = applyFormat { MarkdownFormattingHelper.cycleHeading(it) }
    fun formatBold() = applyFormat { MarkdownFormattingHelper.toggleBold(it) }
    fun formatItalic() = applyFormat { MarkdownFormattingHelper.toggleItalic(it) }
    fun formatStrikethrough() = applyFormat { MarkdownFormattingHelper.toggleStrikethrough(it) }
    fun formatChecklist() = applyFormat { MarkdownFormattingHelper.toggleChecklist(it) }
    fun formatBulletList() = applyFormat { MarkdownFormattingHelper.toggleBulletList(it) }
    fun formatNumberedList() = applyFormat { MarkdownFormattingHelper.toggleNumberedList(it) }
    fun formatInlineCode() = applyFormat { MarkdownFormattingHelper.toggleInlineCode(it) }
    fun formatCodeBlock() = applyFormat { MarkdownFormattingHelper.insertCodeBlock(it) }
    fun formatBlockquote() = applyFormat { MarkdownFormattingHelper.toggleBlockquote(it) }
    fun formatTable() = applyFormat { MarkdownFormattingHelper.insertTable(it) }
    fun formatLink() = applyFormat { MarkdownFormattingHelper.insertLink(it) }

    fun undo() {
        undoRedoManager.undo()?.let { previous ->
            _uiState.update {
                it.copy(
                    textFieldValue = previous,
                    saveStatus = SaveStatus.Unsaved,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo
                )
            }
            contentChangeFlow.value = previous.text
        }
    }

    fun redo() {
        undoRedoManager.redo()?.let { next ->
            _uiState.update {
                it.copy(
                    textFieldValue = next,
                    saveStatus = SaveStatus.Unsaved,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo
                )
            }
            contentChangeFlow.value = next.text
        }
    }

    fun toggleTask(lineIndex: Int, isChecked: Boolean) {
        val currentText = _uiState.value.textFieldValue.text.ifEmpty { _uiState.value.content }
        val lines = currentText.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val updatedLine = if (isChecked) {
                line.replaceFirst("- [ ] ", "- [x] ").replaceFirst("* [ ] ", "* [x] ")
            } else {
                line.replaceFirst("- [x] ", "- [ ] ")
                    .replaceFirst("- [X] ", "- [ ] ")
                    .replaceFirst("* [x] ", "* [ ] ")
                    .replaceFirst("* [X] ", "* [ ] ")
            }
            lines[lineIndex] = updatedLine
            val newText = lines.joinToString("\n")
            val newTfv = TextFieldValue(newText, _uiState.value.textFieldValue.selection)
            onTextFieldValueChange(newTfv)
            saveImmediately()
        }
    }

    fun attachFile(fileName: String, inputStream: InputStream) {
        viewModelScope.launch {
            repository.saveAttachment(fileName, inputStream)
                .onSuccess { relativeAttachmentPath ->
                    val isImage = fileName.endsWith(".png", true) ||
                                  fileName.endsWith(".jpg", true) ||
                                  fileName.endsWith(".jpeg", true) ||
                                  fileName.endsWith(".webp", true) ||
                                  fileName.endsWith(".gif", true)

                    val isAudio = fileName.endsWith(".m4a", true) ||
                                  fileName.endsWith(".mp3", true) ||
                                  fileName.endsWith(".wav", true) ||
                                  fileName.endsWith(".aac", true) ||
                                  fileName.endsWith(".ogg", true)

                    val snippet = if (isImage || isAudio) {
                        "![[attachments/$fileName]]\n"
                    } else {
                        "[[attachments/$fileName]]\n"
                    }

                    val current = _uiState.value.textFieldValue
                    val newText = current.text.replaceRange(
                        current.selection.start,
                        current.selection.end,
                        snippet
                    )
                    val cursor = current.selection.start + snippet.length
                    onTextFieldValueChange(TextFieldValue(newText, TextRange(cursor)))
                    _events.emit(NoteEditEvent.ShowMessage("Attached $fileName"))
                }
                .onFailure {
                    _events.emit(NoteEditEvent.ShowMessage("Failed to attach file: ${it.message}"))
                }
        }
    }

    fun showRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = true) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false) }
    }

    fun renameNote(newName: String) {
        dismissRenameDialog()
        viewModelScope.launch {
            repository.renameItem(_uiState.value.relativePath, newName)
                .onSuccess { newPath ->
                    _uiState.update {
                        it.copy(
                            relativePath = newPath,
                            title = newPath.substringAfterLast("/").removeSuffix(".md")
                        )
                    }
                    loadCatalog()
                    _events.emit(NoteEditEvent.PathChanged(newPath))
                    _events.emit(NoteEditEvent.ShowMessage("Note renamed"))
                }
                .onFailure { error ->
                    _events.emit(NoteEditEvent.ShowMessage("Failed to rename: ${error.message}"))
                }
        }
    }

    fun rename(newName: String) = renameNote(newName)

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteNote() {
        dismissDeleteDialog()
        viewModelScope.launch {
            repository.moveToTrash(_uiState.value.relativePath)
                .onSuccess {
                    _events.emit(NoteEditEvent.ShowMessage("Note moved to Trash"))
                    _events.emit(NoteEditEvent.NoteDeleted)
                }
                .onFailure { error ->
                    _events.emit(NoteEditEvent.ShowMessage("Failed to delete: ${error.message}"))
                }
        }
    }

    fun moveToTrash() = deleteNote()
}
