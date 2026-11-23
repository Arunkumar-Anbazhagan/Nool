package com.nool.app.ui.git

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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

enum class GitProvider(
    val label: String,
    val defaultUrlPrefix: String,
    val defaultUser: String,
    val tokenHint: String,
    val sshHint: String
) {
    GITHUB(
        label = "GitHub",
        defaultUrlPrefix = "https://github.com/",
        defaultUser = "token",
        tokenHint = "Create a Personal Access Token in GitHub > Settings > Developer settings > Personal access tokens with 'repo' scope.",
        sshHint = "Add your SSH key to GitHub > Settings > SSH and GPG keys > New SSH key."
    ),
    GITLAB(
        label = "GitLab",
        defaultUrlPrefix = "https://gitlab.com/",
        defaultUser = "oauth2",
        tokenHint = "Create a Personal Access Token in GitLab > User Settings > Access Tokens with 'read_repository' and 'write_repository' scopes.",
        sshHint = "Add your SSH key to GitLab > User Settings / Preferences > SSH Keys."
    ),
    CODEBERG(
        label = "Codeberg",
        defaultUrlPrefix = "https://codeberg.org/",
        defaultUser = "token",
        tokenHint = "Create a Personal Access Token in Codeberg > Settings > Applications with 'repo' scope.",
        sshHint = "Add your SSH key to Codeberg > Settings > SSH / GPG Keys."
    ),
    GITEA(
        label = "Gitea / Forgejo",
        defaultUrlPrefix = "https://",
        defaultUser = "token",
        tokenHint = "Create an Access Token in your Gitea/Forgejo User Settings > Applications with repository scope.",
        sshHint = "Add your SSH key to your Gitea/Forgejo Settings > SSH / GPG Keys."
    ),
    BITBUCKET(
        label = "Bitbucket",
        defaultUrlPrefix = "https://bitbucket.org/",
        defaultUser = "",
        tokenHint = "Enter your Bitbucket username and generate an App Password in Personal Settings > App Passwords with 'repository:read' and 'repository:write'.",
        sshHint = "Add your SSH key to Bitbucket > Personal Settings > SSH Keys > Add key."
    ),
    CUSTOM(
        label = "Custom Server",
        defaultUrlPrefix = "",
        defaultUser = "token",
        tokenHint = "Enter your username and personal access token, deploy token, or password for your self-hosted Git server.",
        sshHint = "Add your SSH key to your Git server or ~/.ssh/authorized_keys."
    )
}

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
    val clipboardManager = LocalClipboardManager.current

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Clone, 1: Initialize
    var remoteUrl by remember { mutableStateOf(uiState.remoteUrl) }
    var targetFolder by remember { mutableStateOf(uiState.targetSubfolder) }
    var authType by remember { mutableStateOf(uiState.authType) }
    var token by remember { mutableStateOf("") }
    var tokenUser by remember { mutableStateOf("token") }
    var selectedProvider by remember {
        val detected = when {
            uiState.remoteUrl.contains("gitlab.com") -> GitProvider.GITLAB
            uiState.remoteUrl.contains("codeberg.org") -> GitProvider.CODEBERG
            uiState.remoteUrl.contains("bitbucket.org") -> GitProvider.BITBUCKET
            uiState.remoteUrl.isNotBlank() && !uiState.remoteUrl.contains("github.com") -> GitProvider.CUSTOM
            else -> GitProvider.GITHUB
        }
        mutableStateOf(detected)
    }

    // Dialog state for non-empty folders
    var showNonEmptyDialog by remember { mutableStateOf(false) }
    var pendingAuth by remember { mutableStateOf<GitAuth>(GitAuth.None) }
    var showFolderPicker by remember { mutableStateOf(false) }

    fun extractRepoName(url: String): String {
        val clean = url.trim().removeSuffix("/").removeSuffix(".git")
        val candidate = clean.substringAfterLast("/").substringAfterLast(":")
        return candidate.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "notes" }
    }

    val suggestedRepoName = remember(remoteUrl) {
        if (remoteUrl.isNotBlank() && (remoteUrl.contains("/") || remoteUrl.contains(":"))) extractRepoName(remoteUrl) else "notes"
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val uriPath = uri.path ?: ""
            val rawName = uriPath.substringAfterLast(":").substringAfterLast("/")
            val cleanName = rawName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { suggestedRepoName }
            viewModel.createFolder(cleanName) { created ->
                targetFolder = created
                showFolderPicker = false
                scope.launch {
                    snackbarHostState.showSnackbar("Target directory set to '$created' in vault storage")
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        viewModel.loadExistingFolders()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(remoteUrl) {
        val trimmed = remoteUrl.trim()
        if (trimmed.startsWith("git@") || trimmed.startsWith("ssh://")) {
            if (authType != "SSH") authType = "SSH"
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (authType == "SSH") {
                authType = if (token.isNotBlank()) "HTTPS" else "NONE"
            }
        }
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
                    Text(
                        text = "Git Hosting Provider",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GitProvider.entries.forEach { provider ->
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    if (provider.defaultUrlPrefix.isNotBlank() && (remoteUrl.isBlank() || GitProvider.entries.any { it.defaultUrlPrefix.isNotBlank() && remoteUrl == it.defaultUrlPrefix })) {
                                        remoteUrl = provider.defaultUrlPrefix
                                    }
                                    if (provider.defaultUser.isNotBlank() || provider == GitProvider.BITBUCKET) {
                                        tokenUser = provider.defaultUser
                                    }
                                },
                                label = { Text(provider.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = {
                            remoteUrl = it
                            if (targetFolder.isBlank() && it.contains("/")) {
                                targetFolder = extractRepoName(it)
                            }
                        },
                        label = { Text("Repository URL") },
                        placeholder = {
                            Text(
                                when (selectedProvider) {
                                    GitProvider.GITHUB -> "https://github.com/user/notes.git"
                                    GitProvider.GITLAB -> "https://gitlab.com/user/notes.git"
                                    GitProvider.CODEBERG -> "https://codeberg.org/user/notes.git"
                                    GitProvider.GITEA -> "https://gitea.example.com/user/notes.git"
                                    GitProvider.BITBUCKET -> "https://bitbucket.org/user/notes.git"
                                    GitProvider.CUSTOM -> "https://git.server.com/user/notes.git or git@host:repo.git"
                                }
                            )
                        },
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
                            value = tokenUser,
                            onValueChange = { tokenUser = it },
                            label = { Text("Username") },
                            placeholder = {
                                Text(
                                    when (selectedProvider) {
                                        GitProvider.GITLAB -> "oauth2 or GitLab username"
                                        GitProvider.BITBUCKET -> "Bitbucket Username"
                                        else -> "token or username"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text(if (selectedProvider == GitProvider.BITBUCKET) "App Password" else "Personal Access Token (PAT)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedProvider.tokenHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (authType == "SSH") {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.sshPublicKey.isBlank()) {
                            Button(
                                onClick = { viewModel.generateSshKey() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Ed25519 SSH Keypair")
                            }
                        } else {
                            Text(
                                text = "Ed25519 Public Key:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.sshPublicKey,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(uiState.sshPublicKey))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("SSH Public Key copied to clipboard")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Key")
                                }
                                Button(
                                    onClick = { viewModel.generateSshKey() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Regenerate")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedProvider.sshHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.clonePhase.isNotBlank()) uiState.clonePhase else "Cloning repository...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val pct = (uiState.cloneProgress * 100).toInt()
                                if (pct > 0) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (uiState.cloneProgress > 0f) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { uiState.cloneProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                androidx.compose.material3.LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!uiState.cloneError.isNullOrBlank() && !uiState.isLoading) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Clone Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Clone Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = uiState.cloneError ?: "Unknown error occurred during clone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.retryClone() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Clone")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val auth = when (authType) {
                            "HTTPS" -> {
                                if (token.isNotBlank()) {
                                    viewModel.setHttpsToken(token, tokenUser)
                                    GitAuth.Token(token = token, username = tokenUser)
                                } else {
                                    GitAuth.None
                                }
                            }
                            "SSH" -> GitAuth.Ssh(
                                privateKey = uiState.sshPublicKey,
                                knownHosts = null
                            )
                            "NONE" -> GitAuth.None
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

    // Folder Picker Dialog
    if (showFolderPicker) {
        var newFolderNameInput by remember { mutableStateOf(suggestedRepoName) }
        var folderFilter by remember { mutableStateOf("") }

        val filteredFolders = remember(uiState.existingFolders, folderFilter) {
            if (folderFilter.isBlank()) uiState.existingFolders
            else uiState.existingFolders.filter { it.contains(folderFilter, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Destination Directory", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Git repositories clone into local app vault storage for high performance and native SSH locking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option A: Browse Device / External folder via SAF
                    OutlinedButton(
                        onClick = { safLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Device Storage (SAF)")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Option B: Create New Folder right here
                    Text(
                        text = "Create New Directory",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFolderNameInput,
                            onValueChange = { newFolderNameInput = it },
                            placeholder = { Text("folder_name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val clean = newFolderNameInput.trim().trim('/')
                                if (clean.isNotBlank()) {
                                    viewModel.createFolder(clean) { created ->
                                        targetFolder = created
                                        showFolderPicker = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Created & selected folder '$created'")
                                        }
                                    }
                                }
                            },
                            enabled = newFolderNameInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Option C: Vault Folders List
                    Text(
                        text = "Vault Folders",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (uiState.existingFolders.size > 4) {
                        OutlinedTextField(
                            value = folderFilter,
                            onValueChange = { folderFilter = it },
                            placeholder = { Text("Search folders...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Root Vault item
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (targetFolder.isEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                targetFolder = ""
                                showFolderPicker = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Root Vault (/)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Clones directly into top-level notes directory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (targetFolder.isEmpty()) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    filteredFolders.forEach { folder ->
                        val isSelected = targetFolder == folder
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    targetFolder = folder
                                    showFolderPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
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
