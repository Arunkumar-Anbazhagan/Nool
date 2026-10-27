package com.nool.app.data.git

sealed interface GitAuth {
    data object None : GitAuth
    data class Token(val token: String, val username: String = "token") : GitAuth
    data class Ssh(
        val privateKey: String,
        val passphrase: String? = null,
        val knownHosts: String? = null
    ) : GitAuth
}

data class GitStatus(
    val modified: List<String> = emptyList(),
    val added: List<String> = emptyList(),
    val deleted: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
    val conflicted: List<String> = emptyList()
) {
    val isClean: Boolean
        get() = modified.isEmpty() && added.isEmpty() && deleted.isEmpty() && untracked.isEmpty() && conflicted.isEmpty()

    val totalChanges: Int
        get() = modified.size + added.size + deleted.size + untracked.size + conflicted.size
}

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val authorName: String,
    val authorEmail: String,
    val timestamp: Long,
    val message: String
)

data class GitDiffEntry(
    val path: String,
    val oldPath: String? = null,
    val changeType: ChangeType,
    val patch: String
) {
    enum class ChangeType { ADD, MODIFY, DELETE, RENAME }
}

data class GitBranch(
    val name: String,
    val isCurrent: Boolean,
    val upstreamName: String? = null,
    val aheadCount: Int = 0,
    val behindCount: Int = 0
)

data class GitRemote(
    val name: String,
    val url: String
)

sealed interface MergeResult {
    data object FastForward : MergeResult
    data object AlreadyUpToDate : MergeResult
    data class Merged(val mergeCommitHash: String) : MergeResult
    data class Conflicts(val conflictedFiles: List<String>) : MergeResult
}

enum class ConflictResolution {
    KEEP_MINE,
    KEEP_THEIRS,
    MANUAL
}

enum class CloneStrategy {
    FAIL_IF_NOT_EMPTY,
    BACKUP_AND_CLEAN,
    CONNECT_AND_FETCH
}
