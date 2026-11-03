package com.nool.app.sync

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Syncing(val phase: String, val progress: Float = 0f) : SyncStatus
    data class Synced(val timestamp: Long) : SyncStatus
    data object Offline : SyncStatus
    data class Conflict(val conflictedFiles: List<String>) : SyncStatus
    data class AuthError(val message: String) : SyncStatus
    data class Error(val code: String, val message: String) : SyncStatus
}

data class SyncLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val details: String? = null
) {
    enum class LogLevel { INFO, SUCCESS, WARN, ERROR }
}
