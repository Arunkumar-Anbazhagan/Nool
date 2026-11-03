package com.nool.app.data.templates

import com.nool.core.model.VaultItem
import com.nool.core.templates.TemplateEngine
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

    suspend fun getOrCreateWeeklyNote(date: Date = Date()): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = settingsRepository.dailyNotesFolder.first().let {
                if (it.isEmpty()) "Weekly Notes" else "$it/Weekly"
            }
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            val year = cal.get(java.util.Calendar.YEAR)
            val week = cal.get(java.util.Calendar.WEEK_OF_YEAR)
            val weekTitle = String.format("%04d-W%02d", year, week)
            val expectedRelativePath = "$folder/$weekTitle.md"

            val existingContent = vaultRepository.readNoteContent(expectedRelativePath).getOrNull()
            if (existingContent != null) {
                return@runCatching expectedRelativePath
            }

            val templates = getTemplates()
            val weeklyTemplate = templates.find {
                it.title.equals("Weekly Note", ignoreCase = true) ||
                it.title.equals("Weekly", ignoreCase = true)
            }

            val rawTemplateContent = if (weeklyTemplate != null) {
                vaultRepository.readNoteContent(weeklyTemplate.relativePath).getOrDefault("")
            } else {
                "# {{title}}\n\n## Weekly Goals\n- [ ] \n\n## Highlights\n\n## Review\n"
            }

            val finalContent = TemplateEngine.processTemplate(rawTemplateContent, weekTitle, date)
            val created = vaultRepository.createNote(folder, weekTitle, finalContent).getOrThrow()
            created.relativePath
        }
    }

    suspend fun getNotesOnThisDay(date: Date = Date()): List<com.nool.app.data.index.NoteEntity> = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        val currentYear = cal.get(java.util.Calendar.YEAR)
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)

        val allNotes = vaultRepository.searchNotes("", scope = com.nool.core.model.SearchScope.ALL).getOrDefault(emptyList())

        val noteCal = java.util.Calendar.getInstance()
        allNotes.filter { note ->
            var matched = false

            // 1. Check title if it matches YYYY-MM-DD
            val titleMatch = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").matchEntire(note.title)
            if (titleMatch != null) {
                val y = titleMatch.groupValues[1].toIntOrNull() ?: 0
                val m = (titleMatch.groupValues[2].toIntOrNull() ?: 0) - 1
                val d = titleMatch.groupValues[3].toIntOrNull() ?: 0
                if (y < currentYear && m == currentMonth && d == currentDay) {
                    matched = true
                }
            }

            // 2. Check createdTimestamp
            if (!matched && note.createdTimestamp > 0) {
                noteCal.timeInMillis = note.createdTimestamp
                if (noteCal.get(java.util.Calendar.YEAR) < currentYear &&
                    noteCal.get(java.util.Calendar.MONTH) == currentMonth &&
                    noteCal.get(java.util.Calendar.DAY_OF_MONTH) == currentDay) {
                    matched = true
                }
            }

            // 3. Check lastModified
            if (!matched && note.lastModified > 0) {
                noteCal.timeInMillis = note.lastModified
                if (noteCal.get(java.util.Calendar.YEAR) < currentYear &&
                    noteCal.get(java.util.Calendar.MONTH) == currentMonth &&
                    noteCal.get(java.util.Calendar.DAY_OF_MONTH) == currentDay) {
                    matched = true
                }
            }

            matched
        }
    }
}
