package com.nool.app.ui.git

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.app.data.git.CloneJobState
import com.nool.app.data.git.CloneJobStatus
import com.nool.app.data.git.CloneStrategy
import com.nool.app.data.git.ConflictResolution
import com.nool.app.data.git.GitAuth
import com.nool.app.data.git.GitBranch
import com.nool.app.data.git.GitCloneNotificationHelper
import com.nool.app.data.git.GitCommit
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitDiffEntry
import com.nool.app.data.git.GitRepository
import com.nool.app.data.git.GitStatus
import com.nool.app.sync.GitCloneManager
import com.nool.app.sync.SyncEngine
import com.nool.app.sync.SyncLogEntry
import com.nool.app.sync.SyncStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GitUiState(
    val isRepo: Boolean = false,
    val isLoading: Boolean = false,
    val clonePhase: String = "",
    val cloneProgress: Float = 0f,
    val cloneError: String? = null,
    val status: GitStatus = GitStatus(),
    val commits: List<GitCommit> = emptyList(),
    val branches: List<GitBranch> = emptyList(),
    val workingDiff: List<GitDiffEntry> = emptyList(),
    val selectedCommitDiff: List<GitDiffEntry> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val existingFolders: List<String> = emptyList(),
    val targetSubfolder: String = "",
    val remoteUrl: String = "",
    val remoteName: String = "origin",
    val authorName: String = "",
    val authorEmail: String = "",
    val authType: String = "NONE",
    val sshPublicKey: String = "",
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isSyncLogSheetOpen: Boolean = false
)

sealed interface GitEvent {
    data class ShowMessage(val message: String) : GitEvent
    data object RepoInitialized : GitEvent
    data object RepoCloned : GitEvent
}

class GitViewModel(
    private val gitRepository: GitRepository,
    private val syncEngine: SyncEngine,
    private val credentialsStore: GitCredentialsStore,
    private val cloneNotificationHelper: GitCloneNotificationHelper? = null,
    private val cloneManager: GitCloneManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GitEvent>()
    val events: SharedFlow<GitEvent> = _events.asSharedFlow()

    val syncStatus: StateFlow<SyncStatus> = syncEngine.syncStatus
    val syncLogs: StateFlow<List<SyncLogEntry>> = syncEngine.recentLogs

    init {
        loadSettings()
        checkRepo()
        loadExistingFolders()
        observeCloneJobState()
    }

    private fun observeCloneJobState() {
        val persisted = cloneManager?.cloneJobState?.value ?: credentialsStore.getCloneJobState()
        if (persisted.status == CloneJobStatus.FAILED) {
            _uiState.update { it.copy(cloneError = persisted.errorMessage) }
        }

        cloneManager?.let { manager ->
            viewModelScope.launch {
                manager.cloneJobState.collect { job ->
                    when (job.status) {
                        CloneJobStatus.RUNNING -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = true,
                                    clonePhase = job.phase,
                                    cloneProgress = job.progress,
                                    cloneError = null
                                )
                            }
                        }
                        CloneJobStatus.SUCCESS -> {
                            _uiState.update {
                                it.copy(
                                    isRepo = true,
                                    isLoading = false,
                                    clonePhase = "Completed",
                                    cloneProgress = 1f,
                                    cloneError = null,
                                    remoteUrl = job.url,
                                    targetSubfolder = job.subfolder
                                )
                            }
                            _events.emit(GitEvent.ShowMessage("Repository cloned successfully!"))
                            _events.emit(GitEvent.RepoCloned)
                            checkRepo()
                        }
                        CloneJobStatus.FAILED -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    clonePhase = "",
                                    cloneProgress = 0f,
                                    cloneError = job.errorMessage
                                )
                            }
                        }
                        CloneJobStatus.IDLE -> Unit
                    }
                }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val remoteUrl = credentialsStore.getRemoteUrl()
            val remoteName = credentialsStore.getRemoteName()
            val targetSubfolder = credentialsStore.getRepoSubfolder()
            val authorName = credentialsStore.getAuthorName()
            val authorEmail = credentialsStore.getAuthorEmail()
            val authType = credentialsStore.getAuthType()
            val sshPublicKey = credentialsStore.getSshPublicKey().orEmpty()

            _uiState.update {
                it.copy(
                    remoteUrl = remoteUrl,
                    remoteName = remoteName,
                    targetSubfolder = targetSubfolder,
                    authorName = authorName,
                    authorEmail = authorEmail,
                    authType = authType,
                    sshPublicKey = sshPublicKey
                )
            }
        }
    }

    fun loadExistingFolders() {
        viewModelScope.launch {
            val folders = gitRepository.listExistingFolders()
            _uiState.update { it.copy(existingFolders = folders) }
        }
    }

    fun createFolder(folderName: String, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clean = folderName.trim().trim('/')
            if (clean.isNotBlank()) {
                gitRepository.createFolder(clean).onSuccess {
                    loadExistingFolders()
                    onCreated(clean)
                }
            }
        }
    }

    suspend fun isDirectoryEmpty(targetSubfolder: String): Boolean {
        return gitRepository.isDirectoryEmpty(targetSubfolder)
    }

    fun checkRepo() {
        viewModelScope.launch {
            val isRepo = gitRepository.isRepo()
            _uiState.update { it.copy(isRepo = isRepo) }
            if (isRepo) {
                loadStatus()
                loadHistory()
                loadBranches()
            }
        }
    }

    fun loadStatus() {
        viewModelScope.launch {
            gitRepository.status()
                .onSuccess { status ->
                    _uiState.update { it.copy(status = status) }
                }
                .onFailure {
                    _events.emit(GitEvent.ShowMessage("Failed to load status: ${it.message}"))
                }
        }
    }

    fun loadHistory(maxCount: Int = 50) {
        viewModelScope.launch {
            gitRepository.getLog(maxCount = maxCount)
                .onSuccess { commits ->
                    _uiState.update { it.copy(commits = commits) }
                }
        }
    }

    fun loadBranches() {
        viewModelScope.launch {
            gitRepository.listBranches()
                .onSuccess { branches ->
                    _uiState.update { it.copy(branches = branches) }
                }
        }
    }

    fun loadWorkingDiff() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.getWorkingTreeDiff()
                .onSuccess { diff ->
                    _uiState.update { it.copy(workingDiff = diff, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Failed to load diff: ${it.message}"))
                }
        }
    }

    fun loadCommitDiff(oldCommit: String, newCommit: String) {
        viewModelScope.launch {
            gitRepository.getDiffBetweenCommits(oldCommit, newCommit)
                .onSuccess { diff ->
                    _uiState.update { it.copy(selectedCommitDiff = diff) }
                }
        }
    }

    fun initRepository(targetSubfolder: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.init(targetSubfolder)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRepo = true,
                            isLoading = false,
                            targetSubfolder = targetSubfolder
                        )
                    }
                    _events.emit(GitEvent.ShowMessage("Git repository initialized!"))
                    _events.emit(GitEvent.RepoInitialized)
                    loadStatus()
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Init failed: ${it.message}"))
                }
        }
    }

    fun cloneRepository(
        remoteUrl: String,
        targetSubfolder: String = "",
        auth: GitAuth,
        strategy: CloneStrategy = CloneStrategy.FAIL_IF_NOT_EMPTY
    ) {
        val authTypeStr = when (auth) {
            is GitAuth.Token -> "HTTPS"
            is GitAuth.Ssh -> "SSH"
            GitAuth.None -> "NONE"
        }
        if (cloneManager != null) {
            _uiState.update { it.copy(isLoading = true, cloneError = null) }
            cloneManager.startClone(remoteUrl, targetSubfolder, authTypeStr, strategy)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    clonePhase = "Connecting to remote...",
                    cloneProgress = 0f,
                    cloneError = null
                )
            }
            cloneNotificationHelper?.showProgress("Connecting to remote...", 0f)

            val repoDisplayName = targetSubfolder.ifBlank {
                remoteUrl.trim().removeSuffix("/").removeSuffix(".git").substringAfterLast("/").ifBlank { "repository" }
            }

            gitRepository.clone(
                remoteUrl = remoteUrl,
                targetSubfolder = targetSubfolder,
                auth = auth,
                strategy = strategy,
                onProgress = { phase, progress ->
                    _uiState.update {
                        it.copy(
                            clonePhase = phase,
                            cloneProgress = progress
                        )
                    }
                    cloneNotificationHelper?.showProgress(phase, progress)
                }
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isRepo = true,
                        isLoading = false,
                        clonePhase = "Completed",
                        cloneProgress = 1f,
                        cloneError = null,
                        remoteUrl = remoteUrl,
                        targetSubfolder = targetSubfolder
                    )
                }
                cloneNotificationHelper?.showSuccess(repoDisplayName)
                _events.emit(GitEvent.ShowMessage("Repository cloned successfully!"))
                _events.emit(GitEvent.RepoCloned)
                checkRepo()
            }.onFailure { error ->
                val errorMessage = error.message ?: "Clone failed"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        clonePhase = "",
                        cloneProgress = 0f,
                        cloneError = errorMessage
                    )
                }
                cloneNotificationHelper?.showError(errorMessage)
                _events.emit(GitEvent.ShowMessage("Clone failed: $errorMessage"))
            }
        }
    }

    fun retryClone() {
        val persisted = cloneManager?.cloneJobState?.value ?: credentialsStore.getCloneJobState()
        if (persisted.url.isNotBlank()) {
            val auth = when (persisted.authType) {
                "HTTPS" -> {
                    val token = credentialsStore.getHttpsToken()
                    val user = credentialsStore.getHttpsUser()
                    if (!token.isNullOrBlank()) GitAuth.Token(token, user) else GitAuth.None
                }
                "SSH" -> {
                    val priv = credentialsStore.getSshPrivateKey() ?: ""
                    val pass = credentialsStore.getSshPassphrase()
                    GitAuth.Ssh(priv, pass)
                }
                else -> GitAuth.None
            }
            cloneRepository(
                remoteUrl = persisted.url,
                targetSubfolder = persisted.subfolder,
                auth = auth,
                strategy = persisted.strategy
            )
        }
    }

    fun commitAll(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.commitAll(message)
                .onSuccess { commit ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Committed: ${commit.shortHash}"))
                    loadStatus()
                    loadHistory()
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Commit failed: ${it.message}"))
                }
        }
    }

    fun switchBranch(branchName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.switchBranch(branchName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Switched to branch $branchName"))
                    loadBranches()
                    loadHistory()
                    loadStatus()
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(GitEvent.ShowMessage("Branch switch failed: ${it.message}"))
                }
        }
    }

    fun createBranch(branchName: String) {
        if (branchName.isBlank()) return
        viewModelScope.launch {
            gitRepository.createBranch(branchName)
                .onSuccess {
                    _events.emit(GitEvent.ShowMessage("Created branch $branchName"))
                    switchBranch(branchName)
                }
                .onFailure {
                    _events.emit(GitEvent.ShowMessage("Failed to create branch: ${it.message}"))
                }
        }
    }

    fun restoreFile(path: String, commitHash: String) {
        viewModelScope.launch {
            gitRepository.restoreFileFromCommit(path, commitHash)
                .onSuccess {
                    _events.emit(GitEvent.ShowMessage("Restored $path from $commitHash"))
                    loadStatus()
                }
                .onFailure {
                    _events.emit(GitEvent.ShowMessage("Restore failed: ${it.message}"))
                }
        }
    }

    fun resolveConflict(path: String, resolution: ConflictResolution) {
        viewModelScope.launch {
            syncEngine.resolveConflict(path, resolution)
                .onSuccess {
                    _events.emit(GitEvent.ShowMessage("Conflict resolved for $path"))
                    loadConflicts()
                    loadStatus()
                }
                .onFailure {
                    _events.emit(GitEvent.ShowMessage("Failed to resolve: ${it.message}"))
                }
        }
    }

    fun loadConflicts() {
        viewModelScope.launch {
            val conflicts = syncEngine.listConflicts()
            _uiState.update { it.copy(conflicts = conflicts) }
        }
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            syncEngine.triggerSyncNow()
                .onSuccess {
                    loadStatus()
                    loadHistory()
                }
        }
    }

    fun generateSshKey() {
        try {
            val (_, pub) = credentialsStore.generateEd25519Keypair()
            _uiState.update { it.copy(sshPublicKey = pub, authType = "SSH") }
            viewModelScope.launch {
                _events.emit(GitEvent.ShowMessage("Ed25519 SSH keypair generated!"))
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _events.emit(GitEvent.ShowMessage("Key generation failed: ${e.message}"))
            }
        }
    }

    fun saveAuthorSettings(name: String, email: String) {
        credentialsStore.setAuthorName(name)
        credentialsStore.setAuthorEmail(email)
        _uiState.update { it.copy(authorName = name, authorEmail = email) }
    }

    fun saveRemoteSettings(name: String, url: String) {
        credentialsStore.setRemoteName(name)
        credentialsStore.setRemoteUrl(url)
        _uiState.update { it.copy(remoteName = name, remoteUrl = url) }
    }

    fun setHttpsToken(token: String, username: String) {
        credentialsStore.setHttpsCredentials(token, username)
        _uiState.update { it.copy(authType = "HTTPS") }
    }

    fun toggleSyncLogSheet() {
        _uiState.update { it.copy(isSyncLogSheetOpen = !it.isSyncLogSheetOpen) }
    }

    fun dismissSyncLogSheet() {
        _uiState.update { it.copy(isSyncLogSheetOpen = false) }
    }
}
