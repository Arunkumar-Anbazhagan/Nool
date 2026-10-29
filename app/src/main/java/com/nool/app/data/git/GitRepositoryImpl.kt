package com.nool.app.data.git

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult as JGitMergeResult
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class GitRepositoryImpl(
    private val vaultRoot: File,
    private val credentialsStore: GitCredentialsStore,
    private val gitDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : GitRepository {

    init {
        // Register custom SSH Session Factory with mwiede JSch
        SshSessionFactory.setInstance(SshSessionFactoryImpl(credentialsStore))
    }

    private fun resolveDir(targetSubfolder: String): File {
        val clean = targetSubfolder.trim().removePrefix("/").removePrefix("\\").replace('\\', '/')
        return if (clean.isEmpty()) vaultRoot else File(vaultRoot, clean)
    }

    override fun getActiveRepoDir(): File {
        val subfolder = credentialsStore.getRepoSubfolder()
        val dir = resolveDir(subfolder)
        if (File(dir, ".git").exists()) return dir
        if (File(vaultRoot, ".git").exists()) return vaultRoot
        return dir
    }

    override suspend fun setRepoSubfolder(subfolder: String): Unit = withContext(gitDispatcher) {
        credentialsStore.setRepoSubfolder(subfolder)
    }

    override suspend fun isDirectoryEmpty(targetSubfolder: String): Boolean = withContext(gitDispatcher) {
        val dir = resolveDir(targetSubfolder)
        if (!dir.exists()) return@withContext true
        val nonHidden = dir.listFiles()?.filter { !it.name.startsWith(".") && it.name != ".trash" } ?: emptyList()
        nonHidden.isEmpty()
    }

    override suspend fun listExistingFolders(): List<String> = withContext(gitDispatcher) {
        val list = mutableListOf<String>()
        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    val rel = child.relativeTo(vaultRoot).path.replace('\\', '/')
                    list.add(rel)
                    scan(child)
                }
            }
        }
        scan(vaultRoot)
        list.sorted()
    }

    override suspend fun createFolder(subfolder: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            val dir = resolveDir(subfolder)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            Unit
        }
    }

    private fun getGit(): Git {
        val dir = getActiveRepoDir()
        val gitDir = File(dir, ".git")
        if (!gitDir.exists() || !gitDir.isDirectory) {
            throw GitError.NotARepo
        }
        return Git.open(dir)
    }

    override suspend fun isRepo(): Boolean = withContext(gitDispatcher) {
        val dir = getActiveRepoDir()
        val gitDir = File(dir, ".git")
        gitDir.exists() && gitDir.isDirectory
    }

    override suspend fun init(targetSubfolder: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            val targetDir = resolveDir(targetSubfolder)
            if (!targetDir.exists()) targetDir.mkdirs()
            val git = Git.init().setDirectory(targetDir).call()
            git.close()
            credentialsStore.setRepoSubfolder(targetSubfolder)
            ensureGitIgnore()
            Unit
        }.recoverCatching { mapException(it) }
    }

    override suspend fun clone(
        remoteUrl: String,
        targetSubfolder: String,
        auth: GitAuth,
        strategy: CloneStrategy,
        onProgress: (phase: String, progress: Float) -> Unit
    ): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            val targetDir = resolveDir(targetSubfolder)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val existingFiles = targetDir.listFiles()?.filter { !it.name.startsWith(".") && it.name != ".trash" } ?: emptyList()

            if (existingFiles.isNotEmpty()) {
                when (strategy) {
                    CloneStrategy.FAIL_IF_NOT_EMPTY -> {
                        throw GitError.Unknown(
                            "The destination folder '${if (targetSubfolder.isBlank()) "Root Vault" else targetSubfolder}' is not empty (${existingFiles.size} existing items). Select another folder or choose 'Backup & Overwrite' / 'Connect & Fetch'."
                        )
                    }
                    CloneStrategy.BACKUP_AND_CLEAN -> {
                        // Move existing files safely to .trash/backup_<timestamp>
                        val backupDir = File(vaultRoot, ".trash/backup_${System.currentTimeMillis()}")
                        backupDir.mkdirs()
                        for (file in existingFiles) {
                            file.renameTo(File(backupDir, file.name))
                        }
                    }
                    CloneStrategy.CONNECT_AND_FETCH -> {
                        // Connect without deleting existing files
                        val git = Git.init().setDirectory(targetDir).call()
                        git.remoteAdd()
                            .setName("origin")
                            .setUri(URIish(remoteUrl))
                            .call()

                        val fetchCmd = git.fetch().setRemote("origin")
                            .setProgressMonitor(createProgressMonitor(onProgress))
                        configureAuth(fetchCmd, auth)
                        fetchCmd.call()

                        // Try checkout main or master from origin
                        val branchList = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
                        val remoteMain = branchList.firstOrNull { it.name.endsWith("/main") }
                            ?: branchList.firstOrNull { it.name.endsWith("/master") }
                            ?: branchList.firstOrNull()

                        if (remoteMain != null) {
                            val branchName = remoteMain.name.substringAfterLast("/")
                            try {
                                git.checkout()
                                    .setCreateBranch(true)
                                    .setName(branchName)
                                    .setStartPoint(remoteMain.name)
                                    .call()
                            } catch (e: Exception) {
                                // If already checked out
                            }
                        }
                        git.close()

                        credentialsStore.setRemoteUrl(remoteUrl)
                        credentialsStore.setRepoSubfolder(targetSubfolder)
                        ensureGitIgnore()
                        return@runCatching Unit
                    }
                }
            }

            // Normal clone into empty directory
            val cloneCommand = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(targetDir)
                .setProgressMonitor(createProgressMonitor(onProgress))

            configureAuth(cloneCommand, auth)
            val git = cloneCommand.call()
            git.close()

            credentialsStore.setRemoteUrl(remoteUrl)
            credentialsStore.setRepoSubfolder(targetSubfolder)
            ensureGitIgnore()
            Unit
        }.recoverCatching { mapException(it) }
    }

    override suspend fun status(): Result<GitStatus> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val jgitStatus = git.status().call()
                GitStatus(
                    modified = (jgitStatus.modified + jgitStatus.changed).distinct(),
                    added = jgitStatus.added.toList(),
                    deleted = (jgitStatus.removed + jgitStatus.missing).distinct(),
                    untracked = jgitStatus.untracked.toList(),
                    conflicted = jgitStatus.conflicting.toList()
                )
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun commitAll(
        message: String,
        authorName: String?,
        authorEmail: String?
    ): Result<GitCommit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                // Stage all changes and deletions
                git.add().addFilepattern(".").call()
                git.add().setUpdate(true).addFilepattern(".").call()

                val name = authorName ?: credentialsStore.getAuthorName()
                val email = authorEmail ?: credentialsStore.getAuthorEmail()
                val author = PersonIdent(name, email)

                val revCommit = git.commit()
                    .setMessage(message)
                    .setAuthor(author)
                    .setCommitter(author)
                    .call()

                revCommit.toGitCommit()
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun commitPaths(paths: List<String>, message: String): Result<GitCommit> = withContext(gitDispatcher) {
        runCatching {
            val activeDir = getActiveRepoDir()
            getGit().use { git ->
                for (path in paths) {
                    val file = File(activeDir, path)
                    if (file.exists()) {
                        git.add().addFilepattern(path).call()
                    } else {
                        git.rm().addFilepattern(path).call()
                    }
                }

                val author = PersonIdent(credentialsStore.getAuthorName(), credentialsStore.getAuthorEmail())
                val revCommit = git.commit()
                    .setMessage(message)
                    .setAuthor(author)
                    .setCommitter(author)
                    .call()

                revCommit.toGitCommit()
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun fetch(
        remote: String,
        auth: GitAuth,
        onProgress: (phase: String, progress: Float) -> Unit
    ): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val fetchCmd = git.fetch()
                    .setRemote(remote)
                    .setProgressMonitor(createProgressMonitor(onProgress))
                configureAuth(fetchCmd, auth)
                fetchCmd.call()
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun pull(
        remote: String,
        branch: String,
        auth: GitAuth,
        rebase: Boolean
    ): Result<MergeResult> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val pullCmd = git.pull()
                    .setRemote(remote)
                    .setRemoteBranchName(branch)
                    .setRebase(rebase)

                configureAuth(pullCmd, auth)
                val pullResult = pullCmd.call()

                if (!pullResult.isSuccessful) {
                    val mergeResult = pullResult.mergeResult
                    if (mergeResult != null && mergeResult.conflicts != null) {
                        return@runCatching MergeResult.Conflicts(mergeResult.conflicts.keys.toList())
                    }
                }

                val mergeResult = pullResult.mergeResult
                when {
                    mergeResult == null -> MergeResult.AlreadyUpToDate
                    mergeResult.mergeStatus == JGitMergeResult.MergeStatus.FAST_FORWARD -> MergeResult.FastForward
                    mergeResult.mergeStatus == JGitMergeResult.MergeStatus.ALREADY_UP_TO_DATE -> MergeResult.AlreadyUpToDate
                    mergeResult.mergeStatus.isSuccessful -> MergeResult.Merged(mergeResult.newHead?.name ?: "")
                    mergeResult.conflicts != null -> MergeResult.Conflicts(mergeResult.conflicts.keys.toList())
                    else -> MergeResult.Merged(mergeResult.newHead?.name ?: "")
                }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun push(
        remote: String,
        branch: String,
        auth: GitAuth,
        onProgress: (phase: String, progress: Float) -> Unit
    ): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val pushCmd = git.push()
                    .setRemote(remote)
                    .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
                    .setProgressMonitor(createProgressMonitor(onProgress))

                configureAuth(pushCmd, auth)
                val pushResults = pushCmd.call()

                for (result in pushResults) {
                    for (update in result.remoteUpdates) {
                        if (update.status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                            throw GitError.NonFastForward
                        }
                        if (update.status != org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK &&
                            update.status != org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE
                        ) {
                            throw GitError.Unknown("Push failed: ${update.status} - ${update.message}")
                        }
                    }
                }
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun listRemotes(): Result<List<GitRemote>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.remoteList().call().map { remoteConfig ->
                    GitRemote(
                        name = remoteConfig.name,
                        url = remoteConfig.urIs.firstOrNull()?.toString() ?: ""
                    )
                }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun addRemote(name: String, url: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.remoteAdd()
                    .setName(name)
                    .setUri(URIish(url))
                    .call()
                credentialsStore.setRemoteName(name)
                credentialsStore.setRemoteUrl(url)
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun removeRemote(name: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.remoteRemove().setRemoteName(name).call()
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun listBranches(): Result<List<GitBranch>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val currentBranchName = git.repository.branch
                val branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()

                branches.map { ref ->
                    val simpleName = ref.name.removePrefix("refs/heads/")
                    GitBranch(
                        name = simpleName,
                        isCurrent = simpleName == currentBranchName
                    )
                }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun createBranch(branchName: String, startPoint: String?): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val cmd = git.branchCreate().setName(branchName)
                if (startPoint != null) cmd.setStartPoint(startPoint)
                cmd.call()
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun switchBranch(branchName: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.checkout().setName(branchName).call()
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun deleteBranch(branchName: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.branchDelete().setBranchNames(branchName).setForce(true).call()
                Unit
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getLog(maxCount: Int, skip: Int): Result<List<GitCommit>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val logCmd = git.log().setMaxCount(maxCount).setSkip(skip)
                logCmd.call().map { it.toGitCommit() }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getFileHistory(path: String, maxCount: Int): Result<List<GitCommit>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                git.log()
                    .addPath(path)
                    .setMaxCount(maxCount)
                    .call()
                    .map { it.toGitCommit() }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getDiffBetweenCommits(oldCommit: String, newCommit: String): Result<List<GitDiffEntry>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val repo = git.repository
                val oldTreeIter = prepareTreeParser(repo, oldCommit)
                val newTreeIter = prepareTreeParser(repo, newCommit)

                val out = ByteArrayOutputStream()
                val df = DiffFormatter(out)
                df.setRepository(repo)
                df.setDiffComparator(RawTextComparator.DEFAULT)
                df.isDetectRenames = true

                val diffs = df.scan(oldTreeIter, newTreeIter)
                diffs.map { entry ->
                    out.reset()
                    df.format(entry)
                    GitDiffEntry(
                        path = entry.newPath,
                        oldPath = if (entry.oldPath != entry.newPath) entry.oldPath else null,
                        changeType = when (entry.changeType) {
                            DiffEntry.ChangeType.ADD -> GitDiffEntry.ChangeType.ADD
                            DiffEntry.ChangeType.MODIFY -> GitDiffEntry.ChangeType.MODIFY
                            DiffEntry.ChangeType.DELETE -> GitDiffEntry.ChangeType.DELETE
                            DiffEntry.ChangeType.RENAME -> GitDiffEntry.ChangeType.RENAME
                            else -> GitDiffEntry.ChangeType.MODIFY
                        },
                        patch = out.toString("UTF-8")
                    )
                }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getWorkingTreeDiff(): Result<List<GitDiffEntry>> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val repo = git.repository
                val head = repo.resolve(Constants.HEAD) ?: return@runCatching emptyList()

                val oldTreeIter = prepareTreeParser(repo, head.name)
                val workingTreeIter = FileTreeIterator(repo)

                val out = ByteArrayOutputStream()
                val df = DiffFormatter(out)
                df.setRepository(repo)
                df.setDiffComparator(RawTextComparator.DEFAULT)

                val diffs = df.scan(oldTreeIter, workingTreeIter)
                diffs.map { entry ->
                    out.reset()
                    df.format(entry)
                    GitDiffEntry(
                        path = entry.newPath,
                        oldPath = entry.oldPath,
                        changeType = when (entry.changeType) {
                            DiffEntry.ChangeType.ADD -> GitDiffEntry.ChangeType.ADD
                            DiffEntry.ChangeType.MODIFY -> GitDiffEntry.ChangeType.MODIFY
                            DiffEntry.ChangeType.DELETE -> GitDiffEntry.ChangeType.DELETE
                            DiffEntry.ChangeType.RENAME -> GitDiffEntry.ChangeType.RENAME
                            else -> GitDiffEntry.ChangeType.MODIFY
                        },
                        patch = out.toString("UTF-8")
                    )
                }
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getFileContentAtCommit(path: String, commitHash: String): Result<String> = withContext(gitDispatcher) {
        runCatching {
            getGit().use { git ->
                val repo = git.repository
                val commitId = repo.resolve(commitHash) ?: throw GitError.Unknown("Commit $commitHash not found")
                val revWalk = RevWalk(repo)
                val commit = revWalk.parseCommit(commitId)
                val tree = commit.tree

                val treeWalk = TreeWalk.forPath(repo, path, tree)
                    ?: throw GitError.Unknown("File $path not found in commit $commitHash")

                val objectId = treeWalk.getObjectId(0)
                val loader = repo.open(objectId)
                String(loader.bytes, Charsets.UTF_8)
            }
        }.recoverCatching { mapException(it) }
    }

    override suspend fun restoreFileFromCommit(path: String, commitHash: String): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            val content = getFileContentAtCommit(path, commitHash).getOrThrow()
            val targetFile = File(getActiveRepoDir(), path)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content, Charsets.UTF_8)

            // Restore creates a new commit
            val shortHash = if (commitHash.length >= 7) commitHash.take(7) else commitHash
            val fileName = File(path).name
            getGit().use { git ->
                git.add().addFilepattern(path).call()
                git.commit()
                    .setMessage("Restore $fileName from commit $shortHash")
                    .call()
            }
            Unit
        }.recoverCatching { mapException(it) }
    }

    override suspend fun getHeadCommitHash(): String? = withContext(gitDispatcher) {
        try {
            getGit().use { git ->
                git.repository.resolve(Constants.HEAD)?.name
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun ensureGitIgnore(): Result<Unit> = withContext(gitDispatcher) {
        runCatching {
            val activeDir = getActiveRepoDir()
            val gitignore = File(activeDir, ".gitignore")
            val defaultIgnore = """
                # Nool Cache & Derived Index
                *.db
                *.db-shm
                *.db-wal
                .nool_cache/
                .git/index.lock
                *.tmp
                *.bak
            """.trimIndent()

            if (!gitignore.exists()) {
                gitignore.writeText(defaultIgnore)
            } else {
                val current = gitignore.readText()
                if (!current.contains(".nool_cache")) {
                    gitignore.appendText("\n$defaultIgnore\n")
                }
            }
            Unit
        }
    }

    private fun configureAuth(command: org.eclipse.jgit.api.TransportCommand<*, *>, auth: GitAuth) {
        when (auth) {
            is GitAuth.Token -> {
                command.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(auth.username, auth.token)
                )
            }
            is GitAuth.Ssh -> Unit // Handled automatically by registered SshSessionFactory
            GitAuth.None -> Unit
        }
    }

    private fun createProgressMonitor(
        onProgress: (phase: String, progress: Float) -> Unit
    ): ProgressMonitor {
        return object : ProgressMonitor {
            private var taskTitle = ""
            private var totalWork = 0
            private var completedWork = 0

            override fun start(totalTasks: Int) {}
            override fun beginTask(title: String?, totalWork: Int) {
                this.taskTitle = title ?: "Working"
                this.totalWork = totalWork
                this.completedWork = 0
                onProgress(taskTitle, 0f)
            }

            override fun update(completed: Int) {
                completedWork += completed
                val progress = if (totalWork > 0) completedWork.toFloat() / totalWork else 0f
                onProgress(taskTitle, progress.coerceIn(0f, 1f))
            }

            override fun endTask() {
                onProgress(taskTitle, 1f)
            }

            override fun isCancelled(): Boolean = false
            override fun showDuration(enabled: Boolean) {}
        }
    }

    private fun prepareTreeParser(repository: Repository, ref: String): AbstractTreeIterator {
        val head = repository.resolve(ref)
        val walk = RevWalk(repository)
        val commit = walk.parseCommit(head)
        val tree = walk.parseTree(commit.tree.id)

        val treeParser = CanonicalTreeParser()
        val reader = repository.newObjectReader()
        try {
            treeParser.reset(reader, tree.id)
        } finally {
            reader.close()
        }
        walk.dispose()
        return treeParser
    }

    private fun RevCommit.toGitCommit(): GitCommit {
        val ident = authorIdent
        return GitCommit(
            hash = name,
            shortHash = name.take(7),
            authorName = ident.name ?: "Unknown",
            authorEmail = ident.emailAddress ?: "",
            timestamp = commitTime.toLong() * 1000L,
            message = fullMessage.trim()
        )
    }

    private fun <T> mapException(throwable: Throwable): T {
        throw when (throwable) {
            is GitError -> throwable
            is LockFailedException -> GitError.LockHeld(throwable.file?.absolutePath ?: "index.lock")
            is CheckoutConflictException -> GitError.Conflict(throwable.conflictingPaths.toList())
            is TransportException -> {
                val msg = throwable.message.orEmpty()
                if (msg.contains("not authorized", ignoreCase = true) ||
                    msg.contains("authentication", ignoreCase = true) ||
                    msg.contains("Auth fail", ignoreCase = true)
                ) {
                    GitError.Auth(msg)
                } else {
                    GitError.Network(msg)
                }
            }
            is InvalidRemoteException -> GitError.Network("Invalid remote: ${throwable.message}")
            is GitAPIException -> GitError.Unknown(throwable.message ?: "Git API error", throwable)
            is IOException -> GitError.Unknown("I/O error: ${throwable.message}", throwable)
            else -> GitError.Unknown(throwable.message ?: "Unexpected error", throwable)
        }
    }
}
