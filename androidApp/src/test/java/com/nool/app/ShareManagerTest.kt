package com.nool.app

import android.content.Context
import com.nool.app.data.files.FileManager
import com.nool.app.data.files.VaultRepositoryImpl
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nool.core.index.SqlDelightIndexManager
import com.nool.core.index.db.NoolDatabase
import com.nool.app.data.index.IndexManager
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.index.NoteTitleAndPath
import com.nool.app.data.share.ShareManager
import com.nool.app.data.share.ShareTarget
import com.nool.app.data.share.SharedContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ShareManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var vaultDir: File
    private lateinit var fileManager: FileManager
    private lateinit var indexManager: IndexManager
    private lateinit var repository: VaultRepositoryImpl
    private lateinit var shareManager: ShareManager

    @Before
    fun setup() {
        vaultDir = tempFolder.newFolder("share_vault")
        fileManager = FileManager(customVaultDir = vaultDir)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { NoolDatabase.Schema.create(it) }
        val sqlDelightManager = SqlDelightIndexManager(NoolDatabase(driver))
        indexManager = IndexManager(vaultDir, sqlDelightManager)
        repository = VaultRepositoryImpl(fileManager, indexManager)
        shareManager = ShareManager(null, repository)
    }

    @After
    fun tearDown() {
        vaultDir.deleteRecursively()
    }

    @Test
    fun `test formatMarkdown with text only`() {
        val result = shareManager.formatMarkdown("This is a simple shared thought.", emptyList())
        assertEquals("This is a simple shared thought.", result)
    }

    @Test
    fun `test formatMarkdown with attachments only`() {
        val attachments = listOf("attachments/photo1.jpg", "attachments/photo2.png")
        val result = shareManager.formatMarkdown(null, attachments)
        assertEquals("![[attachments/photo1.jpg]]\n\n![[attachments/photo2.png]]", result)
    }

    @Test
    fun `test formatMarkdown with text and attachments`() {
        val attachments = listOf("attachments/screenshot.png")
        val result = shareManager.formatMarkdown("Look at this:", attachments)
        assertEquals("Look at this:\n\n![[attachments/screenshot.png]]", result)
    }

    @Test
    fun `test saveSharedContent creates new note`() = runBlocking {
        val payload = SharedContent(
            text = "Shared article notes from browser",
            subject = "Article Summary"
        )
        val target = ShareTarget.NewNote(
            title = "Article Summary",
            folder = ""
        )

        val result = shareManager.saveSharedContent(payload, target)
        assertTrue(result.isSuccess)
        val path = result.getOrThrow()
        assertEquals("Article Summary.md", path)

        val content = repository.readNoteContent(path).getOrThrow()
        assertEquals("Shared article notes from browser", content)
    }

    @Test
    fun `test saveSharedContent appends to existing note`() = runBlocking {
        val note = repository.createNote("", "Inbox", "# Inbox\nInitial item").getOrThrow()

        val payload = SharedContent(
            text = "Appended shared item"
        )
        val target = ShareTarget.AppendToNote(
            noteRelativePath = note.relativePath
        )

        val result = shareManager.saveSharedContent(payload, target)
        assertTrue(result.isSuccess)

        val updatedContent = repository.readNoteContent(note.relativePath).getOrThrow()
        assertTrue(updatedContent.contains("# Inbox\nInitial item"))
        assertTrue(updatedContent.contains("Appended shared item"))
    }
}
