package com.nool.app.data.templates

import com.nool.core.model.VaultItem
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TemplateManager(
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun getTemplates(): List<VaultItem.NoteItem> = withContext(Dispatchers.IO) {
        val folder = settingsRepository.templatesFolder.first()
        val items = vaultRepository.listItems(folder).getOrDefault(emptyList())
        items.filterIsInstance<VaultItem.NoteItem>()
    }

    suspend fun applyTemplate(templateRelativePath: String, noteTitle: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val templateContent = vaultRepository.readNoteContent(templateRelativePath).getOrThrow()
            TemplateEngine.processTemplate(templateContent, noteTitle)
        }
    }

    suspend fun getOrCreateDailyNote(date: Date = Date()): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = settingsRepository.dailyNotesFolder.first()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateTitle = dateFormat.format(date)
            val expectedRelativePath = if (folder.isEmpty()) "$dateTitle.md" else "$folder/$dateTitle.md"

            // Check if already exists
            val existingContent = vaultRepository.readNoteContent(expectedRelativePath).getOrNull()
            if (existingContent != null) {
                return@runCatching expectedRelativePath
            }

            // Look for a daily note template if one exists
            val templates = getTemplates()
            val dailyTemplate = templates.find {
                it.title.equals("Daily Note", ignoreCase = true) ||
                it.title.equals("Daily", ignoreCase = true)
            } ?: templates.firstOrNull()

            val rawTemplateContent = if (dailyTemplate != null) {
                vaultRepository.readNoteContent(dailyTemplate.relativePath).getOrDefault("")
            } else {
                "# {{title}}\n\n"
            }

            val finalContent = TemplateEngine.processTemplate(rawTemplateContent, dateTitle, date)
            val created = vaultRepository.createNote(folder, dateTitle, finalContent).getOrThrow()
            created.relativePath
        }
    }
}
