package com.nool.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.tasks.TaskManager
import com.nool.app.data.tasks.VaultTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<VaultTask> = emptyList(),
    val filter: TaskFilter = TaskFilter.PENDING,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isCreateTaskDialogOpen: Boolean = false
)

class TasksViewModel(
    private val taskManager: TaskManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loaded = taskManager.getAllTasks()
            _uiState.update { it.copy(tasks = loaded, isLoading = false) }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openCreateTaskDialog() {
        _uiState.update { it.copy(isCreateTaskDialogOpen = true) }
    }

    fun dismissCreateTaskDialog() {
        _uiState.update { it.copy(isCreateTaskDialogOpen = false) }
    }

    fun toggleTask(task: VaultTask) {
        // Optimistic UI update
        val updated = _uiState.value.tasks.map {
            if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _uiState.update { it.copy(tasks = updated) }

        viewModelScope.launch {
            val success = taskManager.toggleTask(task)
            if (!success) {
                // Revert if write failed
                loadTasks()
            }
        }
    }

    fun addTask(text: String, notePath: String? = null) {
        if (text.isBlank()) return
        dismissCreateTaskDialog()
        viewModelScope.launch {
            taskManager.addTaskToNote(text.trim(), notePath)
            loadTasks()
        }
    }
}
