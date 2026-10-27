package com.nool.app.ui.git

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.data.git.CloneStrategy
import com.nool.app.data.git.GitAuth
import com.nool.app.ui.components.expressive.GroupedCardContainer
import com.nool.app.ui.components.expressive.SegmentedButtonRow
import com.nool.app.ui.components.expressive.SegmentedItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GitSetupScreen(
    viewModel: GitViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Clone, 1: Initialize
    var remoteUrl by remember { mutableStateOf(uiState.remoteUrl) }
    var targetFolder by remember { mutableStateOf(uiState.targetSubfolder) }
    var authType by remember { mutableStateOf(uiState.authType) }
    var token by remember { mutableStateOf("") }
    var tokenUser by remember { mutableStateOf("token") }

    // Dialog state for non-empty folders
    var showNonEmptyDialog by remember { mutableStateOf(false) }
    var pendingAuth by remember { mutableStateOf<GitAuth>(GitAuth.None) }
    var showFolderPicker by remember { mutableStateOf(false) }

    fun extractRepoName(url: String): String {
        val clean = url.trim().removeSuffix("/").removeSuffix(".git")
        return clean.substringAfterLast("/").trim().ifEmpty { "notes" }
    }

    val suggestedRepoName = remember(remoteUrl) {
        if (remoteUrl.isNotBlank() && remoteUrl.contains("/")) extractRepoName(remoteUrl) else "notes"
    }

    LaunchedEffect(Unit) {
        viewModel.loadExistingFolders()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GitEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is GitEvent.RepoInitialized, is GitEvent.RepoCloned -> onSuccess()
            }
        }
    }

    fun executeClone(strategy: CloneStrategy) {
        viewModel.cloneRepository(
            remoteUrl = remoteUrl.trim(),
            targetSubfolder = targetFolder.trim(),
            auth = pendingAuth,
            strategy = strategy
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Git Repository", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Mode Selector: Clone vs Init
            SegmentedButtonRow(
                items = listOf(
                    SegmentedItem(0, "Clone Remote", Icons.Default.CloudDownload),
                    SegmentedItem(1, "Initialize Local", Icons.Default.Folder)
                ),
                selectedItem = selectedMode,
                onItemSelected = { selectedMode = it }
            )

            if (selectedMode == 0) {
                // Clone from remote
                GroupedCardContainer(headerTitle = "Remote Repository") {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = {
                            remoteUrl = it
                            if (targetFolder.isBlank() && it.contains("/")) {
                                targetFolder = extractRepoName(it)
                            }
                        },
                        label = { Text("Repository URL") },
                        placeholder = { Text("https://github.com/user/notes.git") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Folder Selection
                    Text(
                        text = "Destination Directory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = targetFolder,
                        onValueChange = { targetFolder = it },
                        label = { Text("Directory / Subfolder") },
                        placeholder = { Text("Leave empty for Root Vault (/)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = { showFolderPicker = true }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Pick Folder")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick suggestion chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (suggestedRepoName.isNotBlank()) {
                            FilterChip(
                                selected = targetFolder == suggestedRepoName,
                                onClick = { targetFolder = suggestedRepoName },
                                label = { Text(suggestedRepoName) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        FilterChip(
                            selected = targetFolder.isEmpty(),
                            onClick = { targetFolder = "" },
                            label = { Text("Root Vault (/)") }
                        )

                        if (uiState.existingFolders.isNotEmpty()) {
                            FilterChip(
                                selected = false,
                                onClick = { showFolderPicker = true },
                                label = { Text("Existing Folders (${uiState.existingFolders.size})") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Authentication Selection
                    Text(
                        text = "Authentication Method",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SegmentedButtonRow(
                        items = listOf(
                            SegmentedItem("NONE", "Public", null),
                            SegmentedItem("HTTPS", "Token", Icons.Default.Lock),
                            SegmentedItem("SSH", "SSH Key", Icons.Default.Key)
                        ),
                        selectedItem = authType,
                        onItemSelected = { authType = it }
                    )

                    if (authType == "HTTPS") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Personal Access Token (PAT)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else if (authType == "SSH") {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.sshPublicKey.isBlank()) {
                            Button(
                                onClick = { viewModel.generateSshKey() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Ed25519 SSH Keypair")
                            }
                        } else {
                            Text(
                                text = "Public Key: ${uiState.sshPublicKey.take(35)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val auth = when (authType) {
                            "HTTPS" -> {
                                viewModel.setHttpsToken(token, tokenUser)
                                GitAuth.Token(token = token, username = tokenUser)
                            }
                            "SSH" -> GitAuth.Ssh(
                                privateKey = uiState.sshPublicKey,
                                knownHosts = null
                            )
                            else -> GitAuth.None
                        }
                        pendingAuth = auth

                        // Check if the target directory is empty
                        scope.launch {
                            val isEmpty = viewModel.isDirectoryEmpty(targetFolder.trim())
                            if (isEmpty) {
                                executeClone(CloneStrategy.FAIL_IF_NOT_EMPTY)
                            } else {
                                // Folder contains files: show resolution dialog
                                showNonEmptyDialog = true
                            }
                        }
                    },
                    enabled = remoteUrl.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Clone Repository", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // Initialize local repo
                GroupedCardContainer(headerTitle = "Local Repository") {
                    Text(
                        text = "Initialize a Git repository directly on your device storage. You can specify a folder or initialize the root vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetFolder,
                        onValueChange = { targetFolder = it },
                        label = { Text("Directory / Subfolder (optional)") },
                        placeholder = { Text("Leave empty for Root Vault") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.initRepository(targetFolder.trim()) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Initialize Repository", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // Existing Folder Picker Dialog
    if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Select Destination Folder") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                targetFolder = ""
                                showFolderPicker = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Root Vault (/)", fontWeight = FontWeight.Bold)
                    }

                    uiState.existingFolders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    targetFolder = folder
                                    showFolderPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(folder)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Non-Empty Folder Resolution Dialog
    if (showNonEmptyDialog) {
        val folderLabel = if (targetFolder.isBlank()) "Root Vault (/)" else targetFolder
        AlertDialog(
            onDismissRequest = { showNonEmptyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Destination Not Empty", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "The folder \"$folderLabel\" already contains notes or files. Please choose how you want to proceed:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Option 1: Clone into new clean subfolder (Recommended)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNonEmptyDialog = false
                                targetFolder = suggestedRepoName
                                executeClone(CloneStrategy.FAIL_IF_NOT_EMPTY)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Clone into new folder \"$suggestedRepoName/\" (Recommended)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Creates an isolated clean directory for this repository.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Option 2: Connect and fetch without deleting
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNonEmptyDialog = false
                                executeClone(CloneStrategy.CONNECT_AND_FETCH)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Connect & Fetch (Keep existing notes)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Links this folder to the remote repository without discarding local files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 3: Backup to trash and overwrite
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNonEmptyDialog = false
                                executeClone(CloneStrategy.BACKUP_AND_CLEAN)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Backup to Trash & Overwrite",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Safely moves existing files to .trash and clones cleanly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNonEmptyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
