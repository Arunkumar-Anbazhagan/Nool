package com.nool.app.ui.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.domain.model.VaultItem
import com.nool.app.ui.editor.MarkdownFormattingHelper
import com.nool.app.ui.editor.MarkdownRenderer
import com.nool.app.ui.editor.MarkdownToolbar
import com.nool.app.ui.notes.components.BacklinksSheet
import com.nool.app.ui.notes.components.DeleteConfirmationDialog
import com.nool.app.ui.notes.components.RenameDialog
import com.nool.app.ui.notes.components.WikiLinkAutocompletePopup
import com.nool.app.ui.properties.FilePropertiesView
import com.nool.app.ui.sidepanel.RightSidePanelDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteEditViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (String) -> Unit = {},
    onNavigateToGraph: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRightSidePanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val fileName = "image_${System.currentTimeMillis()}.png"
                viewModel.attachFile(fileName, inputStream)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteEditEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is NoteEditEvent.NoteDeleted -> onNavigateBack()
                is NoteEditEvent.PathChanged -> Unit
                is NoteEditEvent.OpenNote -> onNavigateToNote(event.relativePath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.saveStatus == SaveStatus.Unsaved) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                            }
                        }
                        Text(
                            text = when (uiState.saveStatus) {
                                SaveStatus.Saved -> "Saved"
                                SaveStatus.Saving -> "Saving..."
                                SaveStatus.Unsaved -> "Unsaved changes"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = when (uiState.saveStatus) {
                                SaveStatus.Saved -> MaterialTheme.colorScheme.outline
                                SaveStatus.Saving -> MaterialTheme.colorScheme.primary
                                SaveStatus.Unsaved -> MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.flushSave()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Backlinks & Mentions sheet trigger
                    val totalBacklinks = uiState.backlinks.size + uiState.unlinkedMentions.size
                    IconButton(onClick = { viewModel.toggleBacklinksSheet() }) {
                        BadgedBox(
                            badge = {
                                if (totalBacklinks > 0) {
                                    Badge {
                                        Text("$totalBacklinks")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Backlinks and mentions",
                                tint = if (totalBacklinks > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Local Graph Button
                    IconButton(onClick = { onNavigateToGraph(uiState.relativePath) }) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Local Graph",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Right Side Panel Context Drawer Toggle
                    IconButton(onClick = { showRightSidePanel = !showRightSidePanel }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Note Context & Properties",
                            tint = if (showRightSidePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Preview Toggle Button
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (uiState.isPreviewMode) "Edit Mode" else "Preview Mode",
                            tint = if (uiState.isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Manual Save button
                    if (uiState.saveStatus == SaveStatus.Saving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.manualSave() },
                            enabled = uiState.isDirty
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Note",
                                tint = if (uiState.isDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Insert Template") },
                                leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.loadTemplates()
                                    showTemplateDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Note") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.showRenameDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.showDeleteDialog()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!uiState.isPreviewMode) {
                MarkdownToolbar(
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onHeading = { viewModel.formatHeading() },
                    onBold = { viewModel.formatBold() },
                    onItalic = { viewModel.formatItalic() },
                    onStrikethrough = { viewModel.formatStrikethrough() },
                    onChecklist = { viewModel.formatChecklist() },
                    onBulletList = { viewModel.formatBulletList() },
                    onNumberedList = { viewModel.formatNumberedList() },
                    onInlineCode = { viewModel.formatInlineCode() },
                    onCodeBlock = { viewModel.formatCodeBlock() },
                    onBlockquote = { viewModel.formatBlockquote() },
                    onTable = { viewModel.formatTable() },
                    onLink = { viewModel.formatLink() },
                    onWikiLink = {
                        viewModel.applyFormat { MarkdownFormattingHelper.insertWikiLink(it) }
                    },
                    onAttachImage = { imagePickerLauncher.launch("image/*") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isPreviewMode) {
                // Preview Mode with Properties Header
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    FilePropertiesView(
                        properties = uiState.properties,
                        onPropertiesChanged = { viewModel.updateProperties(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MarkdownRenderer(
                        markdown = uiState.content,
                        vaultRoot = viewModel.getVaultRoot(),
                        onToggleTask = { lineIndex, isChecked ->
                            viewModel.toggleTask(lineIndex, isChecked)
                        },
                        onWikiLinkClick = { target ->
                            viewModel.handleWikiLinkClick(target)
                        }
                    )
                }
            } else {
                // Edit Mode with Properties Header and floating Wiki-link autocomplete
                Column(modifier = Modifier.fillMaxSize()) {
                    FilePropertiesView(
                        properties = uiState.properties,
                        onPropertiesChanged = { viewModel.updateProperties(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.textFieldValue,
                            onValueChange = { viewModel.onTextFieldValueChange(it) },
                            placeholder = {
                                Text(
                                    text = "Start typing markdown...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 22.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Autocomplete suggestion popup card
                        WikiLinkAutocompletePopup(
                            visible = uiState.showWikiLinkSuggestions,
                            query = uiState.wikiLinkQuery,
                            suggestions = uiState.wikiLinkSuggestions,
                            onSelectSuggestion = { viewModel.insertWikiLinkSuggestion(it) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            // Right Side Panel Context Drawer with backdrop scrim
            AnimatedVisibility(
                visible = showRightSidePanel,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showRightSidePanel = false }
                )
            }

            AnimatedVisibility(
                visible = showRightSidePanel,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            ) {
                RightSidePanelDrawer(
                    noteTitle = uiState.title,
                    content = uiState.content,
                    backlinks = uiState.backlinks,
                    unlinkedMentions = uiState.unlinkedMentions,
                    properties = uiState.properties,
                    onPropertiesChanged = { viewModel.updateProperties(it) },
                    onHeadingClick = { heading ->
                        showRightSidePanel = false
                        val lines = uiState.content.lines()
                        if (heading.lineIndex in lines.indices) {
                            var charOffset = 0
                            for (i in 0 until heading.lineIndex) {
                                charOffset += lines[i].length + 1
                            }
                            viewModel.onTextFieldValueChange(
                                uiState.textFieldValue.copy(
                                    selection = androidx.compose.ui.text.TextRange(charOffset)
                                )
                            )
                        }
                    },
                    onOpenNote = {
                        showRightSidePanel = false
                        onNavigateToNote(it)
                    },
                    onConvertUnlinkedMention = { viewModel.convertUnlinkedMention(it) },
                    onCreateMissingNote = { viewModel.createMissingNote(it) },
                    onOpenLocalGraph = {
                        showRightSidePanel = false
                        onNavigateToGraph(uiState.relativePath)
                    }
                )
            }
        }
    }

    // Backlinks & Mentions Modal Bottom Sheet
    if (uiState.isBacklinksSheetOpen) {
        BacklinksSheet(
            noteTitle = uiState.title,
            backlinks = uiState.backlinks,
            unlinkedMentions = uiState.unlinkedMentions,
            onDismiss = { viewModel.dismissBacklinksSheet() },
            onNavigateToNote = { targetPath ->
                viewModel.dismissBacklinksSheet()
                onNavigateToNote(targetPath)
            },
            onConvertMention = { mention ->
                viewModel.convertUnlinkedMention(mention)
            }
        )
    }

    // Broken Link: Missing Note Creation Dialog
    uiState.missingNoteToCreate?.let { missingTitle ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMissingNoteDialog() },
            icon = { Icon(Icons.Default.AddLink, contentDescription = null) },
            title = { Text("Note does not exist") },
            text = { Text("The note \"$missingTitle\" does not exist yet. Would you like to create it now?") },
            confirmButton = {
                Button(onClick = { viewModel.createMissingNote(missingTitle) }) {
                    Text("Create Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMissingNoteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (uiState.showRenameDialog) {
        RenameDialog(
            item = VaultItem.NoteItem(
                name = "${uiState.title}.md",
                relativePath = uiState.relativePath,
                title = uiState.title,
                lastModified = 0L,
                size = 0L
            ),
            onDismiss = { viewModel.dismissRenameDialog() },
            onConfirm = { newTitle -> viewModel.rename(newTitle) }
        )
    }

    // Move to Trash Confirmation Dialog
    if (uiState.showDeleteDialog) {
        DeleteConfirmationDialog(
            item = VaultItem.NoteItem(
                name = "${uiState.title}.md",
                relativePath = uiState.relativePath,
                title = uiState.title,
                lastModified = 0L,
                size = 0L
            ),
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = { viewModel.moveToTrash() }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            icon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
            title = { Text("Insert Template") },
            text = {
                if (templates.isEmpty()) {
                    Text(
                        "No templates found in the templates folder. Create a note in your templates folder to use it as a template.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(templates) { template ->
                            ListItem(
                                headlineContent = { Text(template.title) },
                                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.insertTemplate(template)
                                        showTemplateDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
