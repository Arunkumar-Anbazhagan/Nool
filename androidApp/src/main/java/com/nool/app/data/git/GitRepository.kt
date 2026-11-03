package com.nool.app.data.git

import java.io.File

interface GitRepository {
    suspend fun isRepo(): Boolean
    suspend fun init(targetSubfolder: String = ""): Result<Unit>
    suspend fun clone(
        remoteUrl: String,
        targetSubfolder: String = "",
        auth: GitAuth,
        strategy: CloneStrategy = CloneStrategy.FAIL_IF_NOT_EMPTY,
        onProgress: (phase: String, progress: Float) -> Unit = { _, _ -> }
    ): Result<Unit>

    suspend fun isDirectoryEmpty(targetSubfolder: String = ""): Boolean
    suspend fun listExistingFolders(): List<String>
    suspend fun createFolder(subfolder: String): Result<Unit>
    fun getActiveRepoDir(): File
    suspend fun setRepoSubfolder(subfolder: String): Unit

    suspend fun status(): Result<GitStatus>
    suspend fun commitAll(
        message: String,
        authorName: String? = null,
        authorEmail: String? = null
    ): Result<GitCommit>
    suspend fun commitPaths(paths: List<String>, message: String): Result<GitCommit>

    suspend fun fetch(
        remote: String = "origin",
        auth: GitAuth,
        onProgress: (phase: String, progress: Float) -> Unit = { _, _ -> }
    ): Result<Unit>

    suspend fun pull(
        remote: String = "origin",
        branch: String = "main",
        auth: GitAuth,
        rebase: Boolean = false
    ): Result<MergeResult>

    suspend fun push(
        remote: String = "origin",
        branch: String = "main",
        auth: GitAuth,
        onProgress: (phase: String, progress: Float) -> Unit = { _, _ -> }
    ): Result<Unit>

    suspend fun listRemotes(): Result<List<GitRemote>>
    suspend fun addRemote(name: String, url: String): Result<Unit>
    suspend fun removeRemote(name: String): Result<Unit>

    suspend fun listBranches(): Result<List<GitBranch>>
    suspend fun createBranch(branchName: String, startPoint: String? = null): Result<Unit>
    suspend fun switchBranch(branchName: String): Result<Unit>
    suspend fun deleteBranch(branchName: String): Result<Unit>

    suspend fun getLog(maxCount: Int = 50, skip: Int = 0): Result<List<GitCommit>>
    suspend fun getFileHistory(path: String, maxCount: Int = 30): Result<List<GitCommit>>
    suspend fun getDiffBetweenCommits(oldCommit: String, newCommit: String): Result<List<GitDiffEntry>>
    suspend fun getWorkingTreeDiff(): Result<List<GitDiffEntry>>
    suspend fun getFileContentAtCommit(path: String, commitHash: String): Result<String>
    suspend fun restoreFileFromCommit(path: String, commitHash: String): Result<Unit>

    suspend fun ensureGitIgnore(): Result<Unit>
    suspend fun getHeadCommitHash(): String?
}
