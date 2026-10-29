package com.nool.app

import com.nool.app.data.files.FileManager
import com.nool.app.data.files.VaultRepositoryImpl
import com.nool.app.data.index.IndexManager
import com.nool.app.data.templates.TemplateEngine
import com.nool.app.data.templates.TemplateManager
import com.nool.app.settings.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TemplateManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var vaultDir: File
    private lateinit var fileManager: FileManager
    private lateinit var indexManager: IndexManager
    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setup() {
        vaultDir = tempFolder.newFolder("template_vault")
        fileManager = FileManager(customVaultDir = vaultDir)
        val fakeIndexDao = FakeIndexDao()
        indexManager = IndexManager(vaultDir, fakeIndexDao)
        repository = VaultRepositoryImpl(fileManager, indexManager)
    }

    @After
    fun tearDown() {
        vaultDir.deleteRecursively()
    }

    @Test
    fun `test TemplateEngine replaces title date and time`() {
        val testDate = Date(1700000000000L) // Fixed timestamp
        val template = """
            # {{title}}
            Created: {{date}} at {{time}}
        """.trimIndent()

        val expectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(testDate)
        val expectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(testDate)

        val result = TemplateEngine.processTemplate(template, "Project Roadmap", testDate)
        assertTrue(result.contains("# Project Roadmap"))
        assertTrue(result.contains("Created: $expectedDate at $expectedTime"))
    }

    @Test
    fun `test TemplateEngine supports custom date pattern`() {
        val testDate = Date(1700000000000L)
        val template = "Year: {{date:yyyy}}, Day: {{date:dd}}"

        val expectedYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(testDate)
        val expectedDay = SimpleDateFormat("dd", Locale.getDefault()).format(testDate)

        val result = TemplateEngine.processTemplate(template, "Note", testDate)
        assertEquals("Year: $expectedYear, Day: $expectedDay", result)
    }

    @Test
    fun `test getOrCreateDailyNote creates daily note with date title`() = runBlocking {
        val settingsRepository = SettingsRepository(context = null)
        val templateManager = TemplateManager(repository, settingsRepository)

        val testDate = Date()
        val expectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(testDate)

        val result = templateManager.getOrCreateDailyNote(testDate)
        assertTrue(result.isSuccess)
        val path = result.getOrThrow()
        assertEquals("Daily Notes/$expectedDateStr.md", path)

        val content = repository.readNoteContent(path).getOrThrow()
        assertTrue(content.contains("# $expectedDateStr"))
    }

    @Test
    fun `test applyTemplate populates template variables`() = runBlocking {
        // Create a template in Templates/
        repository.createFolder("", "Templates")
        val templateNote = repository.createNote(
            "Templates",
            "Meeting Note",
            "# Meeting: {{title}}\nDate: {{date}}\n\n## Attendees\n\n## Action Items"
        ).getOrThrow()

        val settingsRepository = SettingsRepository(context = null)
        val templateManager = TemplateManager(repository, settingsRepository)
        val result = templateManager.applyTemplate(templateNote.relativePath, "Sprint Retro")
        assertTrue(result.isSuccess)
        val applied = result.getOrThrow()
        assertTrue(applied.contains("# Meeting: Sprint Retro"))
        assertTrue(applied.contains("## Action Items"))
    }
}
