package com.nool.app.ui.notes

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.core.model.VaultItem
import com.nool.app.data.export.PdfExporter
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.TableChart
import com.nool.app.ui.editor.table.VisualTableEditorDialog
import com.nool.app.ui.editor.MarkdownFormattingHelper
import com.nool.app.ui.editor.MarkdownRenderer
import com.nool.app.ui.editor.MarkdownToolbar
import com.nool.app.ui.editor.audio.AudioRecorderDialog
import com.nool.app.ui.navigation.quickswitcher.QuickSwitcherDialog
import com.nool.app.ui.notes.components.BacklinksSheet
import com.nool.app.ui.notes.components.DeleteConfirmationDialog
import com.nool.app.ui.notes.components.RenameDialog
import com.nool.app.ui.notes.components.SlashCommandPopup
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
    val allVaultNotes by viewModel.allVaultNotes.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showRightSidePanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showQuickSwitcher by remember { mutableStateOf(false) }
    var confirmRestoreCommit by remember { mutableStateOf<com.nool.app.data.git.GitCommit?>(null) }
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

    var showAudioRecorder by remember { mutableStateOf(false) }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAudioRecorder = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission required for voice notes")
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
                    if (uiState.isFocusMode) {
                        IconButton(onClick = { viewModel.toggleFocusMode() }) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Exit Focus Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
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
                                    text = { Text(if (uiState.isFocusMode) "Exit Focus Mode" else "Focus Mode") },
                                    leadingIcon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.toggleFocusMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Insert Table (Visual)") },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.showTableEditor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Version History") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.openVersionHistory()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        val result = PdfExporter.exportNoteToPdf(
                                            context = context,
                                            title = uiState.title,
                                            content = uiState.content
                                        )
                                        result.onSuccess { file ->
                                            PdfExporter.sharePdf(context, file)
                                        }.onFailure { error ->
                                            Toast.makeText(context, "PDF export failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Quick Switcher") },
                                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        showQuickSwitcher = true
                                    }
                                )
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!uiState.isPreviewMode) {
                val wordCount = remember(uiState.content) {
                    uiState.content.split(Regex("\\s+")).count { it.isNotBlank() }
                }
                val charCount = uiState.content.length
                val readingTimeMin = maxOf(1, Math.ceil(wordCount / 200.0).toInt())

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words • $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$readingTimeMin min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

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
                        onCallout = {
                            viewModel.applyFormat { MarkdownFormattingHelper.insertCallout(it) }
                        },
                        onTable = { viewModel.showTableEditor() },
                        onLink = { viewModel.formatLink() },
                        onWikiLink = {
                            viewModel.applyFormat { MarkdownFormattingHelper.insertWikiLink(it) }
                        },
                        onAttachImage = { imagePickerLauncher.launch("image/*") },
                        onRecordAudio = {
                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
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
                        currentNotePath = uiState.relativePath,
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

                        // Slash command popup
                        SlashCommandPopup(
                            visible = uiState.showSlashCommands,
                            query = uiState.slashCommandQuery,
                            commands = uiState.slashCommandSuggestions,
                            onSelectCommand = { viewModel.insertSlashCommand(it) },
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

    if (showAudioRecorder) {
        val attachmentsDir = java.io.File(viewModel.getVaultRoot(), "attachments")
        AudioRecorderDialog(
            targetDir = attachmentsDir,
            onAudioRecorded = { file ->
                val snippet = "![[attachments/${file.name}]]\n"
                val current = uiState.textFieldValue
                val newText = current.text.replaceRange(
                    current.selection.start,
                    current.selection.end,
                    snippet
                )
                val cursor = current.selection.start + snippet.length
                viewModel.onTextFieldValueChange(
                    androidx.compose.ui.text.input.TextFieldValue(
                        newText,
                        androidx.compose.ui.text.TextRange(cursor)
                    )
                )
                viewModel.saveImmediately()
            },
            onDismiss = { showAudioRecorder = false }
        )
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

    if (uiState.showVersionHistory) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeVersionHistory() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Version History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.closeVersionHistory() }) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isVersionHistoryLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.noteHistoryCommits.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Git revisions found",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commit changes to this file to view past revisions here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.noteHistoryCommits, key = { it.hash }) { commit ->
                            val formattedDate = remember(commit.timestamp) {
                                val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(commit.timestamp * 1000L))
                            }

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = commit.shortHash,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = commit.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "By ${commit.authorName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.viewCommitContent(commit) },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("View")
                                        }
                                        Button(
                                            onClick = { confirmRestoreCommit = commit }
                                        ) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (confirmRestoreCommit != null) {
        val commitToRestore = confirmRestoreCommit!!
        AlertDialog(
            onDismissRequest = { confirmRestoreCommit = null },
            icon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Note Version") },
            text = {
                Text(
                    "Restore '${uiState.title}' to commit ${commitToRestore.shortHash} (\"${commitToRestore.message}\")?\n\nThis will replace the current file contents and create a new commit."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreVersion(commitToRestore)
                        confirmRestoreCommit = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreCommit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.previewCommit != null && uiState.previewCommitContent != null) {
        val commit = uiState.previewCommit!!
        AlertDialog(
            onDismissRequest = { viewModel.closeCommitPreview() },
            title = { Text("Revision ${commit.shortHash}") },
            text = {
                Column {
                    Text(
                        text = commit.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = uiState.previewCommitContent!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreVersion(commit)
                    }
                ) {
                    Text("Restore this revision")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeCommitPreview() }) {
                    Text("Close")
                }
            }
        )
    }

    VisualTableEditorDialog(
        visible = uiState.showTableEditorDialog,
        onDismiss = { viewModel.dismissTableEditor() },
        onInsertTable = { viewModel.insertTable(it) }
    )
}
