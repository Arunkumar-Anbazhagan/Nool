package com.nool.app

import com.nool.app.data.git.CloneJobState
import com.nool.app.data.git.CloneJobStatus
import com.nool.app.data.git.CloneStrategy
import com.nool.app.data.git.GitAuth
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitCloneResumableTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test clone job state persistence and recovery in credentials store`() {
        val store = GitCredentialsStore() // in-memory store for unit test
        
        // Initial state is empty
        val initial = store.getCloneJobState()
        assertEquals("", initial.url)
        assertEquals(CloneJobStatus.IDLE, initial.status)

        // Save running state
        val runningState = CloneJobState(
            url = "https://github.com/nool/sample-repo.git",
            subfolder = "work-notes",
            authType = "HTTPS",
            strategy = CloneStrategy.BACKUP_AND_CLEAN,
            status = CloneJobStatus.RUNNING,
            phase = "Receiving objects",
            progress = 0.45f
        )
        store.saveCloneJobState(runningState)

        val retrieved = store.getCloneJobState()
        assertEquals("https://github.com/nool/sample-repo.git", retrieved.url)
        assertEquals("work-notes", retrieved.subfolder)
        assertEquals("HTTPS", retrieved.authType)
        assertEquals(CloneStrategy.BACKUP_AND_CLEAN, retrieved.strategy)
        assertEquals(CloneJobStatus.RUNNING, retrieved.status)
        assertEquals("Receiving objects", retrieved.phase)
        assertEquals(0.45f, retrieved.progress, 0.001f)

        // Save failed state with error message
        val failedState = runningState.copy(
            status = CloneJobStatus.FAILED,
            errorMessage = "Network connection lost"
        )
        store.saveCloneJobState(failedState)

        val retrievedFailed = store.getCloneJobState()
        assertEquals(CloneJobStatus.FAILED, retrievedFailed.status)
        assertEquals("Network connection lost", retrievedFailed.errorMessage)

        // Clear state
        store.clearCloneJobState()
        val afterClear = store.getCloneJobState()
        assertEquals("", afterClear.url)
        assertEquals(CloneJobStatus.IDLE, afterClear.status)
    }

    @Test
    fun `test atomic staging cleans up temporary directory on failure and leaves vault intact`() = runTest {
        val vaultDir = tempFolder.newFolder("test_vault")
        val canaryFile = File(vaultDir, "important_note.md").apply { writeText("original content") }
        val store = GitCredentialsStore()
        val repo = GitRepositoryImpl(vaultDir, store)

        // Attempt clone from an invalid/unreachable URL to trigger failure
        val result = repo.clone(
            remoteUrl = "https://127.0.0.1:9/invalid/nonexistent.git",
            targetSubfolder = "",
            auth = GitAuth.None,
            strategy = CloneStrategy.FAIL_IF_NOT_EMPTY,
            onProgress = { _, _ -> }
        )

        assertTrue(result.isFailure)
        // Verify original vault content was NOT corrupted or deleted
        assertTrue(canaryFile.exists())
        assertEquals("original content", canaryFile.readText())

        // Verify all temporary staging folders (.clone_staging_*) were cleaned up
        val stagingFolders = vaultDir.listFiles()?.filter { it.name.startsWith(".clone_staging_") } ?: emptyList()
        assertTrue("Staging directories must be cleaned up on failure", stagingFolders.isEmpty())
    }
}
