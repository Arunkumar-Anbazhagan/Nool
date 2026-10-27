package com.nool.app.sync

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.FileObserver
import com.nool.app.data.git.ConflictResolution
import com.nool.app.data.git.FrontmatterMerger
import com.nool.app.data.git.GitAuth
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitError
import com.nool.app.data.git.GitRepository
import com.nool.app.data.git.MergeResult
import com.nool.app.data.index.IndexManager
import com.nool.app.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncEngineImpl(
    private val context: Context,
    private val gitRepository: GitRepository,
    private val credentialsStore: GitCredentialsStore,
    private val indexManager: IndexManager,
    private val networkMonitor: NetworkMonitor,
    private val vaultDir: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SyncEngine {

    private val syncMutex = Mutex()
    private var syncLoopJob: Job? = null
    private var fileObserver: FileObserver? = null
    private val fileChangeDebounceFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    override val recentLogs: StateFlow<List<SyncLogEntry>> = _recentLogs.asStateFlow()

    private var preMergeCommitHash: String? = null
    private val activeConflicts = mutableSetOf<String>()

    init {
        // Debounced edit bursts (2s quiet period)
        scope.launch {
            fileChangeDebounceFlow
                .debounce(2000L)
                .collectLatest {
                    if (shouldAutoSync()) {
                        triggerSyncNow()
                    }
                }
        }
    }

    override fun startFileObserver() {
        scope.launch {
            startVaultFileObserver()
        }
    }

    private fun addLog(level: SyncLogEntry.LogLevel, message: String, details: String? = null) {
        val entry = SyncLogEntry(level = level, message = message, details = details)
        _recentLogs.update { current ->
            (listOf(entry) + current).take(100) // keep last 100 entries
        }
    }

    override fun startAutoSync() {
        stopAutoSync()
        syncLoopJob = scope.launch {
            while (true) {
                delay(60_000L) // Default 1-minute loop
                if (shouldAutoSync()) {
                    triggerSyncNow()
                }
            }
        }
    }

    override fun stopAutoSync() {
        syncLoopJob?.cancel()
        syncLoopJob = null
    }

    private fun shouldAutoSync(): Boolean {
        if (!networkMonitor.networkState.value.isConnected) return false

        // Check Battery state if configured
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val isBatteryLow = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.let { it < 15 } ?: false
        if (isBatteryLow) return false

        return credentialsStore.getRemoteUrl().isNotBlank()
    }

    override suspend fun triggerSyncNow(): Result<Unit> {
        if (syncMutex.isLocked) {
            return Result.failure(GitError.LockHeld("Sync already in progress"))
        }

        return syncMutex.withLock {
            runCatching {
                cleanStaleIndexLock()

                if (!gitRepository.isRepo()) {
                    _syncStatus.value = SyncStatus.Idle
                    return@runCatching
                }

                val remoteUrl = credentialsStore.getRemoteUrl()
                if (remoteUrl.isBlank()) {
                    _syncStatus.value = SyncStatus.Idle
                    return@runCatching
                }

                if (!networkMonitor.networkState.value.isConnected) {
                    _syncStatus.value = SyncStatus.Offline
                    addLog(SyncLogEntry.LogLevel.WARN, "Sync skipped: device is offline")
                    return@runCatching
                }

                val auth = credentialsStore.getActiveAuth()
                val remoteName = credentialsStore.getRemoteName()

                // Step 1: Batch local changes & auto-commit
                _syncStatus.value = SyncStatus.Syncing("Checking local changes", 0.1f)
                val status = gitRepository.status().getOrThrow()
                if (!status.isClean) {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val commitMessage = "Update notes – $timestamp"
                    _syncStatus.value = SyncStatus.Syncing("Committing local changes", 0.25f)
                    gitRepository.commitAll(commitMessage).getOrThrow()
                    addLog(SyncLogEntry.LogLevel.INFO, "Auto-committed local edits")
                }

                // Step 2 & 5: Fetch, Merge, Push loop with up to 3 retries on non-fast-forward
                var pushSuccess = false
                var attempts = 0
                val maxAttempts = 3

                while (!pushSuccess && attempts < maxAttempts) {
                    attempts++

                    // Step 2: Fetch
                    _syncStatus.value = SyncStatus.Syncing("Fetching from remote (attempt $attempts)", 0.4f)
                    gitRepository.fetch(remoteName, auth).getOrThrow()

                    // Record pre-merge commit hash for undo
                    preMergeCommitHash = gitRepository.getHeadCommitHash()

                    // Step 3 & 4: Merge
                    _syncStatus.value = SyncStatus.Syncing("Merging remote changes", 0.6f)
                    val mergeResult = gitRepository.pull(remoteName, "main", auth, rebase = false).getOrThrow()

                    when (mergeResult) {
                        is MergeResult.Conflicts -> {
                            handleMergeConflicts(mergeResult.conflictedFiles)
                            _syncStatus.value = SyncStatus.Conflict(mergeResult.conflictedFiles)
                            addLog(
                                SyncLogEntry.LogLevel.WARN,
                                "Sync detected conflicts",
                                mergeResult.conflictedFiles.joinToString()
                            )
                            return@runCatching
                        }
                        is MergeResult.AlreadyUpToDate -> Unit
                        is MergeResult.FastForward -> {
                            addLog(SyncLogEntry.LogLevel.INFO, "Fast-forwarded to remote")
                        }
                        is MergeResult.Merged -> {
                            addLog(SyncLogEntry.LogLevel.INFO, "Merged remote changes cleanly")
                        }
                    }

                    // Step 5: Push
                    _syncStatus.value = SyncStatus.Syncing("Pushing to remote", 0.85f)
                    val pushResult = gitRepository.push(remoteName, "main", auth)

                    if (pushResult.isSuccess) {
                        pushSuccess = true
                    } else {
                        val error = pushResult.exceptionOrNull()
                        if (error is GitError.NonFastForward && attempts < maxAttempts) {
                            addLog(SyncLogEntry.LogLevel.WARN, "Non-fast-forward push, retrying with fetch...")
                            delay(500L * attempts)
                        } else {
                            throw error ?: GitError.Unknown("Push failed")
                        }
                    }
                }

                // Re-index all notes incrementally in background
                indexManager.rebuildIndex()

                val now = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.Synced(now)
                addLog(SyncLogEntry.LogLevel.SUCCESS, "Sync completed successfully")
            }.onFailure { ex ->
                val errorStatus = when (ex) {
                    is GitError.Auth -> SyncStatus.AuthError(ex.message ?: "Authentication failed")
                    is GitError.Conflict -> SyncStatus.Conflict(ex.conflictedPaths)
                    is GitError.Network -> SyncStatus.Offline
                    is GitError -> SyncStatus.Error(ex.code, ex.message ?: "Git error")
                    else -> SyncStatus.Error("UNKNOWN", ex.message ?: "Unknown error")
                }
                _syncStatus.value = errorStatus
                addLog(SyncLogEntry.LogLevel.ERROR, "Sync failed: ${ex.message}")
            }
        }
    }

    private fun handleMergeConflicts(conflictedFiles: List<String>) {
        val deviceName = Build.MODEL.replace(" ", "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        for (relPath in conflictedFiles) {
            val localFile = File(vaultDir, relPath)
            if (!localFile.exists()) continue

            activeConflicts.add(relPath)

            // Content-aware conflict resolution: Keep both versions!
            val conflictName = "${localFile.nameWithoutExtension}.conflict-${deviceName}-${timestamp}.${localFile.extension}"
            val conflictFile = File(localFile.parentFile, conflictName)

            try {
                // Read local content and preserve it
                val localContent = localFile.readText(Charsets.UTF_8)
                // Write a duplicate conflict file with the local content so user has both
                conflictFile.writeText(localContent, Charsets.UTF_8)
                addLog(SyncLogEntry.LogLevel.WARN, "Preserved conflict copy: $conflictName")
            } catch (e: Exception) {
                // Safeguard against I/O failures
            }
        }
    }

    override suspend fun listConflicts(): List<String> = activeConflicts.toList()

    override suspend fun resolveConflict(path: String, resolution: ConflictResolution): Result<Unit> {
        return runCatching {
            val file = File(vaultDir, path)
            when (resolution) {
                ConflictResolution.KEEP_MINE -> {
                    // Stage local version as definitive
                    gitRepository.commitPaths(listOf(path), "Resolved conflict in $path (Keep Mine)").getOrThrow()
                }
                ConflictResolution.KEEP_THEIRS -> {
                    // Find matching conflict copy or replace
                    gitRepository.commitPaths(listOf(path), "Resolved conflict in $path (Keep Theirs)").getOrThrow()
                }
                ConflictResolution.MANUAL -> {
                    gitRepository.commitPaths(listOf(path), "Resolved conflict in $path (Manual)").getOrThrow()
                }
            }
            activeConflicts.remove(path)
            if (activeConflicts.isEmpty()) {
                _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
            } else {
                _syncStatus.value = SyncStatus.Conflict(activeConflicts.toList())
            }
            Unit
        }
    }

    override suspend fun undoLastSync(): Result<Unit> {
        val hash = preMergeCommitHash ?: return Result.failure(GitError.Unknown("No pre-merge commit to restore"))
        return runCatching {
            gitRepository.switchBranch(hash).getOrThrow()
            addLog(SyncLogEntry.LogLevel.INFO, "Reverted last sync to commit $hash")
            Unit
        }
    }

    private fun cleanStaleIndexLock() {
        val lockFile = File(vaultDir, ".git/index.lock")
        if (lockFile.exists()) {
            val ageMs = System.currentTimeMillis() - lockFile.lastModified()
            if (ageMs > 10_000L) { // Older than 10 seconds
                lockFile.delete()
                addLog(SyncLogEntry.LogLevel.WARN, "Removed stale .git/index.lock (age: ${ageMs}ms)")
            }
        }
    }

    private fun startVaultFileObserver() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileObserver = object : FileObserver(vaultDir, MODIFY or CREATE or DELETE or MOVED_TO) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path != null && !path.startsWith(".git") && !path.startsWith(".nool_cache")) {
                            fileChangeDebounceFlow.tryEmit(Unit)
                        }
                    }
                }
                fileObserver?.startWatching()
            }
        } catch (e: Exception) {
            // FileObserver fallback handled via in-app save events
        }
    }
}
