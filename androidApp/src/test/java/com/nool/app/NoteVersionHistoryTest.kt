package com.nool.app

import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NoteVersionHistoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): Pair<GitRepositoryImpl, File> {
        val vaultDir = tempFolder.newFolder("vault_${System.currentTimeMillis()}")
        val credentialsStore = GitCredentialsStore()
        val gitRepo = GitRepositoryImpl(vaultDir, credentialsStore)
        return Pair(gitRepo, vaultDir)
    }

    @Test
    fun `getFileHistory returns commits for specific note and restore creates new commit`() = runBlocking {
        val (repo, vaultDir) = createRepository()
        repo.init().getOrThrow()

        val jgit = Git.open(vaultDir)

        // Commit 1: Initial note
        val noteFile = File(vaultDir, "MyNote.md")
        noteFile.writeText("Content v1", Charsets.UTF_8)
        jgit.add().addFilepattern("MyNote.md").call()
        val c1 = jgit.commit().setMessage("Initial MyNote commit").call()

        // Commit 2: Update note
        noteFile.writeText("Content v2", Charsets.UTF_8)
        jgit.add().addFilepattern("MyNote.md").call()
        val c2 = jgit.commit().setMessage("Updated MyNote to v2").call()

        // Commit 3: Unrelated file
        val otherFile = File(vaultDir, "Other.md")
        otherFile.writeText("Other content", Charsets.UTF_8)
        jgit.add().addFilepattern("Other.md").call()
        jgit.commit().setMessage("Added Other.md").call()

        // Test 1: getFileHistory only returns the 2 commits touching MyNote.md
        val history = repo.getFileHistory("MyNote.md").getOrThrow()
        assertEquals(2, history.size)
        assertEquals(c2.name, history[0].hash)
        assertEquals(c1.name, history[1].hash)

        // Test 2: getFileContentAtCommit returns v1 content
        val contentAtC1 = repo.getFileContentAtCommit("MyNote.md", c1.name).getOrThrow()
        assertEquals("Content v1", contentAtC1)

        // Test 3: restoreFileFromCommit restores to v1 and creates a new commit
        repo.restoreFileFromCommit("MyNote.md", c1.name).getOrThrow()
        assertEquals("Content v1", noteFile.readText(Charsets.UTF_8))

        val updatedHistory = repo.getFileHistory("MyNote.md").getOrThrow()
        assertEquals(3, updatedHistory.size)
        assertTrue(updatedHistory[0].message.startsWith("Restore MyNote.md from"))
        jgit.close()
    }
}
