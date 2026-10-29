package com.nool.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.index.SearchScope
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.SavedSearch
import com.nool.app.settings.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedTag: String? = null,
    val selectedFolder: String? = null,
    val selectedType: String? = null,
    val selectedScope: SearchScope = SearchScope.ALL,
    val results: List<NoteEntity> = emptyList(),
    val availableTags: Map<String, Int> = emptyMap(),
    val availableFolders: List<String> = emptyList(),
    val savedSearches: List<SavedSearch> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: VaultRepository,
    private val settingsRepository: SettingsRepository? = null,
    initialTag: String? = null,
    initialQuery: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            query = initialQuery ?: "",
            selectedTag = initialTag
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryInputFlow = MutableStateFlow(initialQuery ?: "")

    init {
        loadMetadata()
        executeSearch()

        viewModelScope.launch {
            queryInputFlow
                .debounce(250L)
                .collectLatest {
                    executeSearch()
                }
        }

        viewModelScope.launch {
            repository.vaultUpdates.collect {
                loadMetadata()
                executeSearch()
            }
        }

        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.savedSearches.collect { saved ->
                    _uiState.update { it.copy(savedSearches = saved) }
                }
            }
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            repository.getAllTags().onSuccess { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
            repository.getAllFolders().onSuccess { folders ->
                _uiState.update { it.copy(availableFolders = folders) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryInputFlow.value = newQuery
    }

    fun selectTag(tag: String?) {
        val next = if (_uiState.value.selectedTag == tag) null else tag
        _uiState.update { it.copy(selectedTag = next) }
        executeSearch()
    }

    fun selectFolder(folder: String?) {
        val next = if (_uiState.value.selectedFolder == folder) null else folder
        _uiState.update { it.copy(selectedFolder = next) }
        executeSearch()
    }

    fun selectScope(scope: SearchScope) {
        _uiState.update { it.copy(selectedScope = scope) }
        executeSearch()
    }

    fun saveSearch(name: String) {
        val q = _uiState.value.query
        if (q.isBlank() || settingsRepository == null) return
        viewModelScope.launch {
            settingsRepository.saveSearch(name, q)
        }
    }

    fun deleteSavedSearch(id: String) {
        if (settingsRepository == null) return
        viewModelScope.launch {
            settingsRepository.deleteSavedSearch(id)
        }
    }

    fun applySavedSearch(saved: SavedSearch) {
        onQueryChange(saved.query)
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                query = "",
                selectedTag = null,
                selectedFolder = null,
                selectedType = null,
                selectedScope = SearchScope.ALL
            )
        }
        executeSearch()
    }

    private fun executeSearch() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.searchNotes(
                query = state.query,
                folder = state.selectedFolder,
                tag = state.selectedTag,
                type = state.selectedType,
                scope = state.selectedScope
            ).onSuccess { res ->
                _uiState.update { it.copy(results = res, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
