package com.nool.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.settings.CalendarDefaultView
import com.nool.app.settings.NoteLayoutMode
import com.nool.app.settings.ThemeMode
import com.nool.app.ui.theme.Dimens
import java.util.Calendar
import java.util.Locale

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
    val pureBlack by viewModel.pureBlack.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val noteLayoutMode by viewModel.noteLayoutMode.collectAsState()
    val templatesFolder by viewModel.templatesFolder.collectAsState()
    val dailyNotesFolder by viewModel.dailyNotesFolder.collectAsState()
    val autoSave by viewModel.autoSave.collectAsState()
    val biometricLock by viewModel.biometricLock.collectAsState()
    val calendarDefaultView by viewModel.calendarDefaultView.collectAsState()
    val calendarFirstDayOfWeek by viewModel.calendarFirstDayOfWeek.collectAsState()
    val calendarDefaultReminder by viewModel.calendarDefaultReminderMinutes.collectAsState()
    val calendarDefaultCalendar by viewModel.calendarDefaultCalendar.collectAsState()
    val vaultStats by viewModel.vaultStats.collectAsState()
    val isRebuildingIndex by viewModel.isRebuildingIndex.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showDailyNotesDialog by remember { mutableStateOf(false) }
    var showCalendarViewDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showRebuildIndexConfirm by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    var tempTemplatesText by remember { mutableStateOf("") }
    var tempDailyNotesText by remember { mutableStateOf("") }
    var tempFontSize by remember { mutableFloatStateOf(fontSize.toFloat()) }

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
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                .padding(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing8),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16)
        ) {
            // Search field to find settings
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.Spacing4),
                shape = MaterialTheme.shapes.medium
            )

            // 1. APPEARANCE
            if (matchesSearch("Appearance Theme Dark Light Mode Dynamic Color Font Size Note Layout Pure Black", searchQuery)) {
                SettingsGroupCard(title = "Appearance", icon = Icons.Default.Palette) {
                    // Theme Mode
                    SettingsRowItem(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        subtitle = when (themeMode) {
                            ThemeMode.SYSTEM -> "Follow system setting"
                            ThemeMode.LIGHT -> "Light theme"
                            ThemeMode.DARK -> "Dark theme"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Dynamic Color
                    SettingsRowItem(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Color",
                        subtitle = "Harmonize with system Material You wallpaper palette",
                        trailing = {
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) }
                            )
                        },
                        onClick = { viewModel.setDynamicColor(!dynamicColor) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Pure Black (AMOLED)
                    SettingsRowItem(
                        icon = Icons.Default.DarkMode,
                        title = "Pure Black AMOLED",
                        subtitle = "High contrast deep blacks for OLED displays in dark mode",
                        trailing = {
                            Switch(
                                checked = pureBlack,
                                onCheckedChange = { viewModel.setPureBlack(it) }
                            )
                        },
                        onClick = { viewModel.setPureBlack(!pureBlack) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Font Size
                    SettingsRowItem(
                        icon = Icons.Default.FormatSize,
                        title = "Font Size",
                        subtitle = "$fontSize sp",
                        onClick = {
                            tempFontSize = fontSize.toFloat()
                            showFontSizeDialog = true
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Note Layout Mode
                    SettingsRowItem(
                        icon = if (noteLayoutMode == NoteLayoutMode.GRID) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        title = "Default Note Layout",
                        subtitle = noteLayoutMode.label,
                        onClick = { showLayoutDialog = true }
                    )
                }
            }

            // 2. NOTES & EDITOR
            if (matchesSearch("Notes Editor Templates Folder Daily Markdown Auto-Save", searchQuery)) {
                SettingsGroupCard(title = "Notes & Editor", icon = Icons.Outlined.Description) {
                    // Templates Folder
                    SettingsRowItem(
                        icon = Icons.Default.Folder,
                        title = "Templates Folder",
                        subtitle = templatesFolder,
                        onClick = {
                            tempTemplatesText = templatesFolder
                            showTemplatesDialog = true
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Daily Notes Folder
                    SettingsRowItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Daily Notes Folder",
                        subtitle = dailyNotesFolder,
                        onClick = {
                            tempDailyNotesText = dailyNotesFolder
                            showDailyNotesDialog = true
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Auto-Save
                    SettingsRowItem(
                        icon = Icons.Default.Save,
                        title = "Auto-save",
                        subtitle = "Automatically save changes continuously while editing",
                        trailing = {
                            Switch(
                                checked = autoSave,
                                onCheckedChange = { viewModel.setAutoSave(it) }
                            )
                        },
                        onClick = { viewModel.setAutoSave(!autoSave) }
                    )
                }
            }

            // 3. CALENDAR
            if (matchesSearch("Calendar Default View First Day Week Reminder Schedule", searchQuery)) {
                SettingsGroupCard(title = "Calendar", icon = Icons.Default.CalendarMonth) {
                    // Default View
                    SettingsRowItem(
                        icon = Icons.Default.CalendarMonth,
                        title = "Default View",
                        subtitle = calendarDefaultView.label,
                        onClick = { showCalendarViewDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // First Day of Week
                    SettingsRowItem(
                        icon = Icons.Default.CalendarToday,
                        title = "First Day of Week",
                        subtitle = if (calendarFirstDayOfWeek == Calendar.SUNDAY) "Sunday" else "Monday",
                        onClick = { showFirstDayDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Default Reminder
                    SettingsRowItem(
                        icon = Icons.Default.Notifications,
                        title = "Default Reminder",
                        subtitle = if (calendarDefaultReminder == 0) "None" else "$calendarDefaultReminder minutes before",
                        onClick = { showReminderDialog = true }
                    )
                }
            }

            // 4. SYNC & BACKUP
            if (matchesSearch("Sync Backup Git Repository Remote Status Clone", searchQuery)) {
                SettingsGroupCard(title = "Sync & Backup", icon = Icons.Default.Sync) {
                    SettingsRowItem(
                        icon = Icons.Default.Sync,
                        title = "Git Repository Status",
                        subtitle = "Manage branches, commit history, and sync state",
                        onClick = onNavigateToGitStatus
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Git Remote & Credentials",
                        subtitle = "SSH keys, personal access tokens, and remote URL",
                        onClick = onNavigateToGitSettings
                    )
                }
            }

            // 5. SECURITY & PRIVACY
            if (matchesSearch("Security Privacy Biometric Lock Fingerprint Passcode", searchQuery)) {
                SettingsGroupCard(title = "Security & Privacy", icon = Icons.Default.Lock) {
                    SettingsRowItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric App Lock",
                        subtitle = "Require fingerprint or face unlock when opening Nool",
                        trailing = {
                            Switch(
                                checked = biometricLock,
                                onCheckedChange = { viewModel.setBiometricLock(it) }
                            )
                        },
                        onClick = { viewModel.setBiometricLock(!biometricLock) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Zero Telemetry Guarantee",
                        subtitle = "100% offline-first. Notes remain purely on your device.",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }

            // 6. STORAGE & INDEX
            if (matchesSearch("Storage Usage Notes Count Cache Rebuild Index Size", searchQuery)) {
                SettingsGroupCard(title = "Storage", icon = Icons.Default.Storage) {
                    SettingsRowItem(
                        icon = Icons.Default.Storage,
                        title = "Vault Usage",
                        subtitle = "${vaultStats.noteCount} notes • ${formatBytes(vaultStats.totalSizeBytes)}"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Delete,
                        title = "Clear Cache",
                        subtitle = "Free temporary thumbnails and transient data",
                        onClick = { showClearCacheConfirm = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Refresh,
                        title = "Rebuild Search Index",
                        subtitle = "Cleanly rebuild SQLite FTS5 index directly from vault files",
                        trailing = if (isRebuildingIndex) {
                            { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        } else null,
                        onClick = {
                            if (!isRebuildingIndex) showRebuildIndexConfirm = true
                        }
                    )
                }
            }

            // 7. ABOUT
            if (matchesSearch("About Version Licenses Nool Open Source", searchQuery)) {
                SettingsGroupCard(title = "About", icon = Icons.Default.Info) {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "Nool v0.2.0 (Material 3 Release)"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Outlined.Description,
                        title = "Open Source Licenses",
                        subtitle = "Libraries, dependencies, and notices",
                        onClick = { showLicensesDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }

    // DIALOGS

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = Dimens.Spacing8)
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "System default"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = { Text("Editor Font Size") },
            text = {
                Column {
                    Text(
                        text = "Sample Note Preview",
                        fontSize = tempFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = Dimens.Spacing12)
                    )
                    Slider(
                        value = tempFontSize,
                        onValueChange = { tempFontSize = it },
                        valueRange = 12f..24f,
                        steps = 5
                    )
                    Text(
                        text = "${tempFontSize.toInt()} sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setFontSize(tempFontSize.toInt())
                    showFontSizeDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Note Layout Dialog
    if (showLayoutDialog) {
        AlertDialog(
            onDismissRequest = { showLayoutDialog = false },
            title = { Text("Default Note Layout") },
            text = {
                Column {
                    NoteLayoutMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setNoteLayoutMode(mode)
                                    showLayoutDialog = false
                                }
                                .padding(vertical = Dimens.Spacing8)
                        ) {
                            RadioButton(
                                selected = noteLayoutMode == mode,
                                onClick = {
                                    viewModel.setNoteLayoutMode(mode)
                                    showLayoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLayoutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Templates Folder Dialog
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = { Text("Templates Folder") },
            text = {
                OutlinedTextField(
                    value = tempTemplatesText,
                    onValueChange = { tempTemplatesText = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempTemplatesText.isNotBlank()) {
                        viewModel.setTemplatesFolder(tempTemplatesText)
                    }
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

    // Daily Notes Folder Dialog
    if (showDailyNotesDialog) {
        AlertDialog(
            onDismissRequest = { showDailyNotesDialog = false },
            title = { Text("Daily Notes Folder") },
            text = {
                OutlinedTextField(
                    value = tempDailyNotesText,
                    onValueChange = { tempDailyNotesText = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempDailyNotesText.isNotBlank()) {
                        viewModel.setDailyNotesFolder(tempDailyNotesText)
                    }
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

    // Calendar View Dialog
    if (showCalendarViewDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarViewDialog = false },
            title = { Text("Default Calendar View") },
            text = {
                Column {
                    CalendarDefaultView.values().forEach { view ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalendarDefaultView(view)
                                    showCalendarViewDialog = false
                                }
                                .padding(vertical = Dimens.Spacing8)
                        ) {
                            RadioButton(
                                selected = calendarDefaultView == view,
                                onClick = {
                                    viewModel.setCalendarDefaultView(view)
                                    showCalendarViewDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(view.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarViewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // First Day Dialog
    if (showFirstDayDialog) {
        AlertDialog(
            onDismissRequest = { showFirstDayDialog = false },
            title = { Text("First Day of Week") },
            text = {
                Column {
                    listOf(Calendar.MONDAY to "Monday", Calendar.SUNDAY to "Sunday").forEach { (day, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalendarFirstDayOfWeek(day)
                                    showFirstDayDialog = false
                                }
                                .padding(vertical = Dimens.Spacing8)
                        ) {
                            RadioButton(
                                selected = calendarFirstDayOfWeek == day,
                                onClick = {
                                    viewModel.setCalendarFirstDayOfWeek(day)
                                    showFirstDayDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFirstDayDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Reminder Dialog
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Default Reminder Time") },
            text = {
                Column {
                    listOf(
                        0 to "None",
                        5 to "5 minutes before",
                        15 to "15 minutes before",
                        30 to "30 minutes before",
                        60 to "1 hour before"
                    ).forEach { (mins, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalendarDefaultReminderMinutes(mins)
                                    showReminderDialog = false
                                }
                                .padding(vertical = Dimens.Spacing8)
                        ) {
                            RadioButton(
                                selected = calendarDefaultReminder == mins,
                                onClick = {
                                    viewModel.setCalendarDefaultReminderMinutes(mins)
                                    showReminderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear Cache Confirmation
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear Temporary Cache?") },
            text = { Text("This will clear temporary files and image caches. Your markdown notes and vault contents will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheConfirm = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rebuild Index Confirmation
    if (showRebuildIndexConfirm) {
        AlertDialog(
            onDismissRequest = { showRebuildIndexConfirm = false },
            title = { Text("Rebuild Search Index?") },
            text = { Text("This will re-parse all Markdown notes in your vault and re-populate the SQLite FTS5 search index. Your files remain completely intact.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rebuildIndex()
                    showRebuildIndexConfirm = false
                }) {
                    Text("Rebuild")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildIndexConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "• Jetpack Compose & Material 3 (Apache 2.0)\n" +
                                "• SQLDelight (Apache 2.0)\n" +
                                "• Eclipse JGit (EDL 1.0)\n" +
                                "• MWiede JSch (BSD)\n" +
                                "• CommonMark Java (BSD 2-Clause)\n" +
                                "• Coil (Apache 2.0)\n" +
                                "• Koin (Apache 2.0)\n" +
                                "• Kotlinx Coroutines & DateTime (Apache 2.0)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = Dimens.Spacing8)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Guarantee") },
            text = {
                Text(
                    text = "Nool is designed from the ground up as a privacy-first, local-first knowledge base.\n\n" +
                            "• Zero Telemetry: No analytics, tracking, or background pings.\n" +
                            "• Files on Device: Plain Markdown files stored in your device storage.\n" +
                            "• Git Encryption: Sync directly to your private git server using SSH keys with Android Keystore encryption.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Dimens.Spacing12)
            ) {
                Surface(
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Dimens.Spacing12))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Dimens.Spacing8, horizontal = Dimens.Spacing4)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.IconMedium)
        )
        Spacer(modifier = Modifier.width(Dimens.Spacing16))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun matchesSearch(content: String, query: String): Boolean {
    if (query.isBlank()) return true
    val terms = query.trim().lowercase(Locale.getDefault()).split(Regex("\\s+")).filter { it.isNotBlank() }
    val target = content.lowercase(Locale.getDefault())
    return terms.all { target.contains(it) }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt().coerceIn(1, 6)
    val pre = "KMGTPE"[exp - 1]
    val value = bytes / Math.pow(1024.0, exp.toDouble())
    return String.format(Locale.US, "%.1f %sB", value, pre)
}
