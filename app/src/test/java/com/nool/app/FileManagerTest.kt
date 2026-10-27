package com.nool.app

import com.nool.app.data.files.FileManager
import com.nool.app.domain.model.VaultItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var vaultDir: File
    private lateinit var fileManager: FileManager

    @Before
    fun setup() {
        vaultDir = tempFolder.newFolder("test_vault")
        fileManager = FileManager(
            context = null,
            customVaultDir = vaultDir
        )
    }

    @After
    fun tearDown() {
        vaultDir.deleteRecursively()
    }

    @Test
    fun `test vault initializes with welcome note when empty`() = runBlocking {
        val items = fileManager.listItems("")
        assertTrue(items.any { it.name == "Welcome to Nool.md" })
    }

    @Test
    fun `test create and read note`() = runBlocking {
        val note = fileManager.createNote("", "My First Note", "# Header\nSome markdown text")
        assertEquals("My First Note.md", note.name)
        assertEquals("My First Note", note.title)

        val content = fileManager.readNoteContent(note.relativePath)
        assertEquals("# Header\nSome markdown text", content)
    }

    @Test
    fun `test create nested folder and note inside it`() = runBlocking {
        val folder = fileManager.createFolder("", "Projects")
        assertEquals("Projects", folder.name)

        val note = fileManager.createNote("Projects", "Android Architecture", "MVVM and UDF")
        assertEquals("Projects/Android Architecture.md", note.relativePath.replace('\\', '/'))

        val itemsInProjects = fileManager.listItems("Projects")
        assertEquals(1, itemsInProjects.size)
        assertEquals("Android Architecture.md", itemsInProjects.first().name)
    }

    @Test
    fun `test rename note and folder`() = runBlocking {
        val note = fileManager.createNote("", "Draft Note", "content")
        val renamedPath = fileManager.renameItem(note.relativePath, "Final Note")
        assertEquals("Final Note.md", renamedPath)

        val items = fileManager.listItems("")
        assertTrue(items.any { it.name == "Final Note.md" })
        assertFalse(items.any { it.name == "Draft Note.md" })
    }

    @Test
    fun `test move note to another folder`() = runBlocking {
        fileManager.createFolder("", "Archive")
        val note = fileManager.createNote("", "Old Note", "archive me")

        val movedPath = fileManager.moveItem(note.relativePath, "Archive")
        assertEquals("Archive/Old Note.md", movedPath.replace('\\', '/'))

        val rootItems = fileManager.listItems("")
        assertFalse(rootItems.any { it.name == "Old Note.md" })

        val archiveItems = fileManager.listItems("Archive")
        assertTrue(archiveItems.any { it.name == "Old Note.md" })
    }

    @Test
    fun `test move folder into itself throws exception`() {
        runBlocking {
            fileManager.createFolder("", "Parent")
            fileManager.createFolder("Parent", "Child")

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    fileManager.moveItem("Parent", "Parent/Child")
                }
            }
        }
    }

    @Test
    fun `test delete note and folder`() = runBlocking {
        val folder = fileManager.createFolder("", "ToDelete")
        fileManager.createNote("ToDelete", "Note Inside", "data")

        fileManager.deleteItem(folder.relativePath)
        val items = fileManager.listItems("")
        assertFalse(items.any { it.name == "ToDelete" })
    }

    @Test
    fun `test path traversal throws security exception`() {
        assertThrows(SecurityException::class.java) {
            runBlocking {
                fileManager.readNoteContent("../../etc/passwd")
            }
        }
        Unit
    }

    @Test
    fun `test move note to trash and list trash`() = runBlocking {
        val note = fileManager.createNote("", "Note To Trash", "some content")
        val trashed = fileManager.moveToTrash(note.relativePath)
        assertEquals("Note To Trash.md", trashed.name)

        val rootItems = fileManager.listItems("")
        assertFalse(rootItems.any { it.name == "Note To Trash.md" })

        val trashList = fileManager.listTrash()
        assertTrue(trashList.any { it.id == trashed.id })
    }

    @Test
    fun `test restore note from trash`() = runBlocking {
        val note = fileManager.createNote("", "Recoverable Note", "important data")
        val trashed = fileManager.moveToTrash(note.relativePath)

        val restoredPath = fileManager.restoreFromTrash(trashed.id)
        assertEquals("Recoverable Note.md", restoredPath)

        val rootItems = fileManager.listItems("")
        assertTrue(rootItems.any { it.name == "Recoverable Note.md" })
        val content = fileManager.readNoteContent(restoredPath)
        assertEquals("important data", content)

        val trashList = fileManager.listTrash()
        assertFalse(trashList.any { it.id == trashed.id })
    }

    @Test
    fun `test permanently delete and empty trash`() = runBlocking {
        val note1 = fileManager.createNote("", "Trash 1", "data")
        val note2 = fileManager.createNote("", "Trash 2", "data")
        val t1 = fileManager.moveToTrash(note1.relativePath)
        val t2 = fileManager.moveToTrash(note2.relativePath)

        fileManager.permanentlyDeleteFromTrash(t1.id)
        val listAfterSingleDelete = fileManager.listTrash()
        assertFalse(listAfterSingleDelete.any { it.id == t1.id })
        assertTrue(listAfterSingleDelete.any { it.id == t2.id })

        fileManager.emptyTrash()
        val emptyList = fileManager.listTrash()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun `test save and read attachment`() = runBlocking {
        val dummyData = "PNG_SAMPLE_BYTES".toByteArray()
        val path = fileManager.saveAttachment("test_chart.png", dummyData.inputStream())
        assertEquals("attachments/test_chart.png", path)

        val file = fileManager.getAttachmentFile(path)
        assertTrue(file.exists())
        assertEquals("PNG_SAMPLE_BYTES", file.readText())
    }
}
