package com.nool.app.sync

import com.nool.app.data.git.ConflictResolution
import kotlinx.coroutines.flow.StateFlow

interface SyncEngine {
    val syncStatus: StateFlow<SyncStatus>
    val recentLogs: StateFlow<List<SyncLogEntry>>

    suspend fun triggerSyncNow(): Result<Unit>
    suspend fun resolveConflict(path: String, resolution: ConflictResolution): Result<Unit>
    suspend fun undoLastSync(): Result<Unit>
    suspend fun listConflicts(): List<String>
    fun startAutoSync()
    fun stopAutoSync()
}
