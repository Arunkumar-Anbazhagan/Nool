package com.nool.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nool.app.ui.components.expressive.GroupedCardContainer
import com.nool.app.ui.components.expressive.PillListItem
import com.nool.app.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGitSettings: () -> Unit = {},
    onNavigateToGitStatus: () -> Unit = {},
    onNavigateToGitSetup: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val templatesFolder by viewModel.templatesFolder.collectAsState()
    val dailyNotesFolder by viewModel.dailyNotesFolder.collectAsState()
    val isRebuildingIndex by viewModel.isRebuildingIndex.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showDailyNotesDialog by remember { mutableStateOf(false) }
    var tempTemplatesText by remember { mutableStateOf("") }
    var tempDailyNotesText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Setting Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ThemeMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "Follow system default"
                                    ThemeMode.LIGHT -> "Light theme"
                                    ThemeMode.DARK -> "Dark theme (Minimal / Obsidian)"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "Use system wallpaper palette (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                }
            }

            // Typography Setting Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Editor Font Size (${fontSize} sp)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { viewModel.setFontSize(it.toInt()) },
                        valueRange = 12f..24f,
                        steps = 5
                    )
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.",
                        fontSize = fontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Index Maintenance Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Search & Derived Index", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All databases and indexes are derived caches. Your plain Markdown files are the source of truth. You can rebuild the full search index at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ElevatedButton(
                        onClick = { viewModel.rebuildIndex() },
                        enabled = !isRebuildingIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRebuildingIndex) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rebuilding Index...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rebuild Search Index")
                        }
                    }
                }
            }

            // Git & Synchronization Section
            GroupedCardContainer(headerTitle = "Git & Synchronization") {
                PillListItem(
                    title = "Git Configuration",
                    subtitle = "Manage remotes, SSH keys, and auto-sync",
                    leadingIcon = Icons.Default.Sync,
                    onClick = onNavigateToGitSettings
                )
                Spacer(modifier = Modifier.height(8.dp))
                PillListItem(
                    title = "Working Tree Changes",
                    subtitle = "View modified files, branches, and commit",
                    leadingIcon = Icons.Default.Commit,
                    onClick = onNavigateToGitStatus
                )
                Spacer(modifier = Modifier.height(8.dp))
                PillListItem(
                    title = "Clone or Initialize Repository",
                    subtitle = "Clone remote repo or initialize local Git",
                    leadingIcon = Icons.Default.CloudDownload,
                    onClick = onNavigateToGitSetup
                )
            }

            // Templates & Daily Notes Section
            GroupedCardContainer(headerTitle = "Templates & Daily Notes") {
                PillListItem(
                    title = "Templates Folder",
                    subtitle = "Location: ${if (templatesFolder.isEmpty()) "Vault Root" else templatesFolder} (supports {{title}}, {{date}}, {{time}})",
                    leadingIcon = Icons.Default.NoteAlt,
                    onClick = {
                        tempTemplatesText = templatesFolder
                        showTemplatesDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PillListItem(
                    title = "Daily Notes Folder",
                    subtitle = "Location: ${if (dailyNotesFolder.isEmpty()) "Vault Root" else dailyNotesFolder} (opens/creates YYYY-MM-DD.md)",
                    leadingIcon = Icons.Default.CalendarToday,
                    onClick = {
                        tempDailyNotesText = dailyNotesFolder
                        showDailyNotesDialog = true
                    }
                )
            }

            // Storage & Vault Information Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Storage & Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vault Path:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Text(viewModel.getVaultPath(), style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }

    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = { Text("Templates Folder") },
            text = {
                Column {
                    Text(
                        "Specify the folder where your template markdown files are stored. Variables like {{title}}, {{date}}, and {{time}} will be resolved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempTemplatesText,
                        onValueChange = { tempTemplatesText = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setTemplatesFolder(tempTemplatesText)
                    showTemplatesDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplatesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDailyNotesDialog) {
        AlertDialog(
            onDismissRequest = { showDailyNotesDialog = false },
            title = { Text("Daily Notes Folder") },
            text = {
                Column {
                    Text(
                        "Specify the folder where daily notes will be created or opened (named YYYY-MM-DD.md).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempDailyNotesText,
                        onValueChange = { tempDailyNotesText = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setDailyNotesFolder(tempDailyNotesText)
                    showDailyNotesDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyNotesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
