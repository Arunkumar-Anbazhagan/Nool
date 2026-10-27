package com.nool.app.data.git

sealed class GitError(
    val code: String,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    data class Network(val detail: String, val statusCode: Int? = null) :
        GitError("NETWORK_ERROR", "Network failure: $detail")

    data class Auth(val detail: String) :
        GitError("AUTH_ERROR", "Authentication failed: $detail")

    data class Conflict(val conflictedPaths: List<String>) :
        GitError("MERGE_CONFLICT", "Merge conflicts detected in: ${conflictedPaths.joinToString()}")

    data object NotARepo :
        GitError("NOT_A_REPO", "Directory is not a valid Git repository")

    data object DirtyTree :
        GitError("DIRTY_TREE", "Working tree contains uncommitted changes")

    data object NonFastForward :
        GitError("NON_FAST_FORWARD", "Push rejected: remote has diverged (non-fast-forward)")

    data class LockHeld(val lockPath: String) :
        GitError("LOCK_HELD", "Repository index lock is held: $lockPath")

    data object Cancelled :
        GitError("CANCELLED", "Git operation was cancelled")

    data class Unknown(val detail: String, val originalException: Throwable? = null) :
        GitError("UNKNOWN", detail, originalException)
}
