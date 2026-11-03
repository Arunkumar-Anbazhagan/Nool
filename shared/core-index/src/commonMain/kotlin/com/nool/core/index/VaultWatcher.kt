package com.nool.core.index

/**
 * Interface for incremental vault filesystem watching.
 * Implementations observe changes to the vault directory and emit events for modified/created
 * or deleted files without requiring a full vault scan.
 */
interface VaultWatcher {
    fun startWatching(
        vaultPath: String,
        onFileChanged: (relativePath: String) -> Unit,
        onFileDeleted: (relativePath: String) -> Unit
    )

    fun stopWatching()
}
