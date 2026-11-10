package com.nool.app

import com.nool.app.data.files.FileManager
import com.nool.app.data.tasks.TaskManager
import com.nool.app.data.tasks.VaultTask
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class TaskManagerTest {

    private lateinit var tempVaultDir: File
    private lateinit var fileManager: FileManager
    private lateinit var taskManager: TaskManager

    @Before
    fun setUp() {
        tempVaultDir = File.createTempFile("test_tasks_vault", "")
        tempVaultDir.delete()
        tempVaultDir.mkdirs()

        fileManager = FileManager(customVaultDir = tempVaultDir)
        taskManager = TaskManager(fileManager)
    }

    @After
    fun tearDown() {
        tempVaultDir.deleteRecursively()
    }

    @Test
    fun testExtractTasksFromNotes() = runBlocking {
        val noteContent = """
            # Project Plan
            Here are some tasks:
            - [ ] Buy groceries
            - [x] Write code
            * [ ] Call mom
            + [X] Ship release
            Regular bullet without checkbox
        """.trimIndent()

        fileManager.createNote("Projects", "TasksNote", noteContent)

        val tasks = taskManager.getAllTasks()
        assertEquals(4, tasks.size)

        val task1 = tasks.find { it.taskText == "Buy groceries" }!!
        assertFalse(task1.isCompleted)
        assertEquals("Projects/TasksNote.md", task1.notePath)

        val task2 = tasks.find { it.taskText == "Write code" }!!
        assertTrue(task2.isCompleted)

        val task3 = tasks.find { it.taskText == "Call mom" }!!
        assertFalse(task3.isCompleted)

        val task4 = tasks.find { it.taskText == "Ship release" }!!
        assertTrue(task4.isCompleted)
    }

    @Test
    fun testToggleTaskCompletesAndUncompletes() = runBlocking {
        val noteContent = """
            # Todo
            - [ ] Test toggling
        """.trimIndent()

        fileManager.createNote("", "TodoNote", noteContent)

        var tasks = taskManager.getAllTasks()
        assertEquals(1, tasks.size)
        val initialTask = tasks.first()
        assertFalse(initialTask.isCompleted)

        // Toggle to completed
        val toggled = taskManager.toggleTask(initialTask)
        assertTrue(toggled)

        val contentAfterToggle = fileManager.readNoteContent("TodoNote.md")
        assertTrue(contentAfterToggle.contains("- [x] Test toggling"))

        tasks = taskManager.getAllTasks()
        assertTrue(tasks.first().isCompleted)

        // Toggle back to uncompleted
        val toggledBack = taskManager.toggleTask(tasks.first())
        assertTrue(toggledBack)

        val contentAfterSecondToggle = fileManager.readNoteContent("TodoNote.md")
        assertTrue(contentAfterSecondToggle.contains("- [ ] Test toggling"))
    }

    @Test
    fun testAddTaskToNoteAppendsCorrectly() = runBlocking {
        val success = taskManager.addTaskToNote("New automated task", "Notes/Quick.md")
        assertTrue(success)

        val content = fileManager.readNoteContent("Notes/Quick.md")
        assertTrue(content.contains("- [ ] New automated task"))

        val tasks = taskManager.getAllTasks()
        assertEquals(1, tasks.size)
        assertEquals("New automated task", tasks.first().taskText)
    }
}
