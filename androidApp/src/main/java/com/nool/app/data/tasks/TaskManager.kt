package com.nool.app.data.tasks

import com.nool.app.data.files.FileManager
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultTask(
    val id: String,
    val notePath: String,
    val noteTitle: String,
    val taskText: String,
    val isCompleted: Boolean,
    val lineNumber: Int,
    val rawLine: String
)

class TaskManager(
    private val fileManager: FileManager,
    private val vaultRepository: VaultRepository? = null
) {
    private val taskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])\]\s+(.*)$""")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun getAllTasks(): List<VaultTask> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<VaultTask>()
        val root = fileManager.vaultRoot
        if (!root.exists()) return@withContext emptyList()

        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".md", ignoreCase = true) && !it.name.startsWith(".") && it.parentFile?.name != "attachments" && !it.path.contains(".trash") }
            .forEach { file ->
                val relative = file.relativeTo(root).path.replace('\\', '/')
                val title = file.nameWithoutExtension
                val lines = try { file.readLines() } catch (e: Exception) { emptyList() }
                lines.forEachIndexed { index, line ->
                    val match = taskRegex.find(line)
                    if (match != null) {
                        val mark = match.groupValues[2]
                        val isDone = mark.equals("x", ignoreCase = true)
                        val text = match.groupValues[3].trim()
                        tasks.add(
                            VaultTask(
                                id = "${relative}_$index",
                                notePath = relative,
                                noteTitle = title,
                                taskText = text,
                                isCompleted = isDone,
                                lineNumber = index,
                                rawLine = line
                            )
                        )
                    }
                }
            }
        tasks
    }

    suspend fun toggleTask(task: VaultTask): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = try {
                vaultRepository?.readNoteContent(task.notePath)?.getOrNull()
                    ?: fileManager.readNoteContent(task.notePath)
            } catch (e: Exception) {
                return@withContext false
            }

            val lines = content.lines().toMutableList()

            if (task.lineNumber in lines.indices) {
                val currentLine = lines[task.lineNumber]
                val match = taskRegex.find(currentLine)
                if (match != null) {
                    val indent = match.groupValues[1]
                    val currentMark = match.groupValues[2]
                    val text = match.groupValues[3]
                    val newMark = if (currentMark.equals("x", ignoreCase = true)) " " else "x"
                    val newLine = "$indent- [$newMark] $text"
                    lines[task.lineNumber] = newLine
                    saveContent(task.notePath, lines.joinToString("\n"))
                    return@withContext true
                }
            }

            // Fallback match by exact task text if line numbers shifted
            val targetIdx = lines.indexOfFirst { line ->
                val m = taskRegex.find(line)
                m != null && m.groupValues[3].trim() == task.taskText.trim()
            }
            if (targetIdx != -1) {
                val currentLine = lines[targetIdx]
                val match = taskRegex.find(currentLine)!!
                val indent = match.groupValues[1]
                val currentMark = match.groupValues[2]
                val text = match.groupValues[3]
                val newMark = if (currentMark.equals("x", ignoreCase = true)) " " else "x"
                lines[targetIdx] = "$indent- [$newMark] $text"
                saveContent(task.notePath, lines.joinToString("\n"))
                return@withContext true
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addTaskToNote(taskText: String, targetNotePath: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = if (!targetNotePath.isNullOrBlank()) {
                targetNotePath
            } else {
                val todayKey = dateFormat.format(Date())
                "Daily Notes/$todayKey.md"
            }

            val existingContent = try {
                vaultRepository?.readNoteContent(path)?.getOrNull()
                    ?: if (File(fileManager.vaultRoot, path).exists()) fileManager.readNoteContent(path) else ""
            } catch (e: Exception) {
                ""
            }

            val newContent = if (existingContent.isBlank()) {
                "# ${path.substringAfterLast("/").removeSuffix(".md")}\n\n## Tasks\n- [ ] $taskText\n"
            } else {
                buildString {
                    append(existingContent.trimEnd())
                    append("\n- [ ] ")
                    append(taskText)
                    append("\n")
                }
            }

            saveContent(path, newContent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun saveContent(path: String, content: String) {
        if (vaultRepository != null) {
            vaultRepository.saveNoteContent(path, content)
        } else {
            fileManager.saveNoteContent(path, content)
        }
    }
}
