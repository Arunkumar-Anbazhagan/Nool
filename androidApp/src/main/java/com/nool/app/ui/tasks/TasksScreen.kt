package com.nool.app.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nool.app.data.tasks.VaultTask
import com.nool.app.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onNavigateBack: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }

    val pendingCount = remember(uiState.tasks) { uiState.tasks.count { !it.isCompleted } }
    val completedCount = remember(uiState.tasks) { uiState.tasks.count { it.isCompleted } }

    val filteredTasks = remember(uiState.tasks, uiState.filter, uiState.searchQuery) {
        uiState.tasks.filter { task ->
            val matchesFilter = when (uiState.filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING, TaskFilter.DUE_TODAY, TaskFilter.OVERDUE -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            val matchesQuery = uiState.searchQuery.isBlank() ||
                    task.taskText.contains(uiState.searchQuery, ignoreCase = true) ||
                    task.noteTitle.contains(uiState.searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    val groupedTasks = remember(filteredTasks) {
        filteredTasks.groupBy { it.notePath }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$pendingCount pending • $completedCount completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "Search tasks")
                    }
                    IconButton(onClick = { viewModel.loadTasks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh tasks")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateTaskDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Selector Row
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing8)
            ) {
                TaskFilter.entries.forEachIndexed { index, filter ->
                    val badgeCount = when (filter) {
                        TaskFilter.ALL -> uiState.tasks.size
                        TaskFilter.PENDING, TaskFilter.DUE_TODAY, TaskFilter.OVERDUE -> pendingCount
                        TaskFilter.COMPLETED -> completedCount
                    }
                    SegmentedButton(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskFilter.entries.size),
                        label = {
                            Text("${filter.label} ($badgeCount)", style = MaterialTheme.typography.labelMedium)
                        }
                    )
                }
            }

            // Search Bar (if expanded)
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Filter tasks by keyword or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing4),
                    shape = MaterialTheme.shapes.medium
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.Spacing32),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(Dimens.Spacing16))
                        Text(
                            text = if (uiState.tasks.isEmpty()) "No tasks in your vault" else "No matching tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Dimens.Spacing8))
                        Text(
                            text = if (uiState.tasks.isEmpty()) {
                                "Add `- [ ] task` in any Markdown note or tap the + button to create a task."
                            } else {
                                "Try changing the filter or search term."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Dimens.ScreenHorizontalMargin,
                        vertical = Dimens.Spacing8
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedTasks.forEach { (notePath, tasksInNote) ->
                        val noteTitle = tasksInNote.firstOrNull()?.noteTitle ?: notePath.substringAfterLast("/")
                        item(key = "header_$notePath") {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onOpenNote(notePath) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                                    Text(
                                        text = noteTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.Spacing4))
                                    Text(
                                        text = "• ${tasksInNote.size} tasks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        items(tasksInNote, key = { it.id }) { task ->
                            TaskItemRow(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onOpenNote = { onOpenNote(task.notePath) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Task Dialog
    if (uiState.isCreateTaskDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateTaskDialog() },
            title = { Text("New Task") },
            text = {
                Column {
                    Text(
                        text = "Appends a new `- [ ]` task to today's daily note.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        placeholder = { Text("What needs to be done?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTask(newTaskText)
                        newTaskText = ""
                    },
                    enabled = newTaskText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateTaskDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TaskItemRow(
    task: VaultTask,
    onToggle: () -> Unit,
    onOpenNote: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(Dimens.MinTouchTarget)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Dimens.Spacing8)
            ) {
                Text(
                    text = task.taskText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "in ${task.noteTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onOpenNote() }
                )
            }
        }
    }
}
