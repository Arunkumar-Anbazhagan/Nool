package com.nool.app.ui.notes

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nool.core.model.VaultItem
import com.nool.app.ui.components.expressive.FloatingPillNavDock
import com.nool.app.ui.components.expressive.NavDockItem
import com.nool.app.ui.components.expressive.ScallopedFlowerButton
import com.nool.app.ui.navigation.quickswitcher.QuickSwitcherDialog
import com.nool.app.ui.sidebar.LeftSidebarDrawer
import com.nool.app.ui.notes.components.BreadcrumbsBar
import com.nool.app.ui.notes.components.CreateFolderDialog
import com.nool.app.ui.notes.components.CreateNoteDialog
import com.nool.app.ui.notes.components.DeleteConfirmationDialog
import com.nool.app.ui.notes.components.MoveDialog
import com.nool.app.ui.notes.components.RenameDialog
import com.nool.app.ui.notes.components.SortFilterDialog
import com.nool.app.ui.notes.components.VaultItemRow
import com.nool.app.ui.git.GitStatusChip
import com.nool.app.ui.git.GitSyncLogSheet
import com.nool.app.ui.git.GitViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToNote: (relativePath: String) -> Unit,
    onNavigateToFolder: (folderPath: String) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSearch: (query: String) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToGraph: () -> Unit = {},
    onNavigateToGitSettings: () -> Unit = {},
    onNavigateToGitStatus: () -> Unit = {},
    onNavigateToGitConflict: () -> Unit = {},
    onNavigateToGitSetup: () -> Unit = {},
    gitViewModel: GitViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allVaultNotes by viewModel.allVaultNotes.collectAsState()
    val vaultTags by viewModel.vaultTags.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()
    val savedSearches by viewModel.savedSearches.collectAsState()
    val gitUiState by gitViewModel.uiState.collectAsState()
    val gitSyncStatus by gitViewModel.syncStatus.collectAsState()
    val gitSyncLogs by gitViewModel.syncLogs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateBottomSheet by remember { mutableStateOf(false) }
    var showQuickSwitcher by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is NotesUiEvent.OpenNote -> {
                    onNavigateToNote(event.relativePath)
                }
                is NotesUiEvent.OpenSearch -> {
                    onNavigateToSearch("")
                }
                is NotesUiEvent.OpenSettings -> {
                    onNavigateToSettings()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LeftSidebarDrawer(
                    rootNode = folderTree,
                    activeNotePath = null,
                    allNotes = allVaultNotes,
                    tagsWithCounts = vaultTags,
                    onSelectNote = { notePath ->
                        scope.launch { drawerState.close() }
                        onNavigateToNote(notePath)
                    },
                    onSelectTag = { tag ->
                        scope.launch { drawerState.close() }
                        onNavigateToSearch("tag:$tag")
                    },
                    onCreateNote = {
                        scope.launch { drawerState.close() }
                        viewModel.showCreateNoteDialog()
                    },
                    onCreateFolder = {
                        scope.launch { drawerState.close() }
                        viewModel.showCreateFolderDialog()
                    },
                    onOpenDailyNote = {
                        scope.launch { drawerState.close() }
                        viewModel.showCalendar()
                    },
                    savedSearches = savedSearches,
                    onSelectSavedSearch = { query ->
                        scope.launch { drawerState.close() }
                        onNavigateToSearch(query)
                    },
                    onDeleteSavedSearch = { id ->
                        viewModel.deleteSavedSearch(id)
                    },
                    onNavigateToTrash = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrash()
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onNavigateToGit = {
                        scope.launch { drawerState.close() }
                        onNavigateToGitStatus()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            val titleText = if (uiState.currentFolder.isEmpty()) "Nool" else uiState.currentFolder.substringAfterLast("/")
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge
                            )
                            val noteCount = uiState.items.count { !it.isDirectory }
                            val folderCount = uiState.items.count { it.isDirectory }
                            val subtitle = buildList {
                                if (folderCount > 0) add("$folderCount ${if (folderCount == 1) "folder" else "folders"}")
                                if (noteCount > 0) add("$noteCount ${if (noteCount == 1) "note" else "notes"}")
                            }.joinToString(", ").ifEmpty { "Empty folder" }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (uiState.currentFolder.isNotEmpty()) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Up to parent folder"
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Sidebar"
                                )
                            }
                        }
                    },
                    actions = {
                        if (gitUiState.isRepo) {
                            GitStatusChip(
                                status = gitSyncStatus,
                                onClick = { gitViewModel.toggleSyncLogSheet() }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = { showQuickSwitcher = true }) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Quick Switcher",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.openOrCreateDailyNote() }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Daily Note"
                            )
                        }
                        IconButton(onClick = onNavigateToGraph) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Graph View",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onNavigateToSearch("") }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { viewModel.showTasks() }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tasks & Due Dates"
                            )
                        }
                        IconButton(onClick = { viewModel.showFlashcards() }) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Flashcard Review"
                            )
                        }
                        IconButton(onClick = { viewModel.showSortFilterDialog() }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort and filter"
                            )
                        }
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Trash"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                FloatingPillNavDock(
                    items = listOf(
                        NavDockItem("notes", "Notes", Icons.Default.Description),
                        NavDockItem("switcher", "Switch", Icons.Default.Bolt),
                        NavDockItem("search", "Search", Icons.Default.Search),
                        NavDockItem("graph", "Graph", Icons.Default.Hub),
                        NavDockItem("settings", "Settings", Icons.Default.Settings)
                    ),
                    selectedItemId = "notes",
                    onItemSelected = { item ->
                        when (item.id) {
                            "switcher" -> showQuickSwitcher = true
                            "search" -> onNavigateToSearch("")
                            "graph" -> onNavigateToGraph()
                            "settings" -> onNavigateToSettings()
                        }
                    },
                    trailingActionButton = {
                        ScallopedFlowerButton(
                            icon = Icons.Default.Add,
                            contentDescription = "New Note",
                            onClick = { showCreateBottomSheet = true },
                            lobes = 10,
                            size = 56.dp,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumbs Navigation
            BreadcrumbsBar(
                breadcrumbs = uiState.breadcrumbs,
                onCrumbClick = { crumb ->
                    if (crumb.relativePath != uiState.currentFolder) {
                        onNavigateToFolder(crumb.relativePath)
                    }
                }
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (uiState.items.isEmpty()) {
                    EmptyVaultFolderState(
                        folderName = if (uiState.currentFolder.isEmpty()) "Vault" else uiState.currentFolder.substringAfterLast("/"),
                        onCreateNote = { viewModel.showCreateNoteDialog() },
                        onCreateFolder = { viewModel.showCreateFolderDialog() }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.relativePath }
                        ) { item ->
                            VaultItemRow(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        onNavigateToFolder(item.relativePath)
                                    } else {
                                        onNavigateToNote(item.relativePath)
                                    }
                                },
                                onRename = { viewModel.showRenameDialog(item) },
                                onMove = { viewModel.showMoveDialog(item) },
                                onDelete = { viewModel.showDeleteDialog(item) },
                                onTogglePin = if (item is VaultItem.NoteItem) { { viewModel.togglePin(item) } } else null,
                                onToggleFavorite = if (item is VaultItem.NoteItem) { { viewModel.toggleFavorite(item) } } else null,
                                onToggleArchive = if (item is VaultItem.NoteItem) { { viewModel.toggleArchive(item) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Create Bottom Sheet
    if (showCreateBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Create in this directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedButton(
                    onClick = {
                        showCreateBottomSheet = false
                        viewModel.showCreateNoteDialog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Markdown Note (.md)")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        showCreateBottomSheet = false
                        viewModel.showCreateFolderDialog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Folder")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showQuickSwitcher) {
        QuickSwitcherDialog(
            notes = allVaultNotes,
            onDismiss = { showQuickSwitcher = false },
            onSelectNote = { path ->
                showQuickSwitcher = false
                onNavigateToNote(path)
            }
        )
    }

    // Dialogs
    when (val dialog = uiState.activeDialog) {
        is NotesDialogState.CreateNote -> {
            CreateNoteDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { title -> viewModel.createNote(title) }
            )
        }
        is NotesDialogState.CreateFolder -> {
            CreateFolderDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.createFolder(name) }
            )
        }
        is NotesDialogState.Rename -> {
            RenameDialog(
                item = dialog.item,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.renameItem(dialog.item, newName) }
            )
        }
        is NotesDialogState.Move -> {
            MoveDialog(
                item = dialog.item,
                availableFolders = dialog.folders,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { target -> viewModel.moveItem(dialog.item, target) }
            )
        }
        is NotesDialogState.Delete -> {
            DeleteConfirmationDialog(
                item = dialog.item,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteItem(dialog.item) }
            )
        }
        is NotesDialogState.SortFilter -> {
            SortFilterDialog(
                currentSort = uiState.sortOption,
                showArchived = uiState.showArchived,
                onSortSelected = { viewModel.setSortOption(it) },
                onToggleShowArchived = { viewModel.setShowArchived(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is NotesDialogState.Calendar -> {
            com.nool.app.ui.calendar.CalendarViewDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                onSelectDate = { date -> viewModel.openOrCreateDailyNote(date) },
                onOpenWeeklyNote = { viewModel.openOrCreateWeeklyNote() },
                onOpenNote = { path -> onNavigateToNote(path) },
                existingNoteDates = uiState.existingNoteDates,
                notesOnThisDay = uiState.notesOnThisDay
            )
        }
        is NotesDialogState.Tasks -> {
            com.nool.app.ui.tasks.TasksDashboardDialog(
                tasks = uiState.tasks,
                onToggleTask = { task, isChecked -> viewModel.toggleTaskFromDashboard(task, isChecked) },
                onOpenNote = { path -> onNavigateToNote(path) },
                onDismissRequest = { viewModel.dismissDialog() }
            )
        }
        is NotesDialogState.Flashcards -> {
            com.nool.app.ui.flashcards.FlashcardReviewDialog(
                dueCards = uiState.dueFlashcards,
                onRecordGrade = { card, grade, state ->
                    viewModel.recordFlashcardReview(card, grade, state)
                },
                onDismissRequest = { viewModel.dismissDialog() }
            )
        }
        null -> Unit
    }

    if (gitUiState.isSyncLogSheetOpen) {
        GitSyncLogSheet(
            status = gitSyncStatus,
            logs = gitSyncLogs,
            onDismiss = { gitViewModel.dismissSyncLogSheet() },
            onSyncNow = { gitViewModel.triggerSyncNow() },
            onOpenConflicts = onNavigateToGitConflict,
            onOpenGitSettings = onNavigateToGitSettings
        )
    }
}

@Composable
fun EmptyVaultFolderState(
    folderName: String,
    onCreateNote: () -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes in $folderName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create markdown notes and organize them with nested folders. Everything is stored as plain files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedButton(
                onClick = onCreateNote,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note")
            }
            OutlinedButton(
                onClick = onCreateFolder,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Folder")
            }
        }
    }
}
