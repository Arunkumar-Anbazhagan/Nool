package com.nool.app.ui.editor.embed

import java.io.File

object NoteEmbedHelper {

    const val MAX_RECURSION_DEPTH = 3

    /**
     * Extracts the markdown content starting from [targetHeading] until the next heading
     * of equal or higher level (smaller or equal level number) or EOF.
     */
    fun extractHeadingSection(fullText: String, targetHeading: String): String {
        val cleanTarget = targetHeading.trim().removePrefix("#").trim()
        val lines = fullText.lines()
        var capturing = false
        var targetLevel = 0
        val resultLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            val headingMatch = Regex("""^(#{1,6})\s+(.+)$""").matchEntire(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].trim()

                if (capturing) {
                    // Reached another heading of equal or higher level
                    if (level <= targetLevel) {
                        break
                    } else {
                        resultLines.add(line)
                    }
                } else if (title.equals(cleanTarget, ignoreCase = true)) {
                    capturing = true
                    targetLevel = level
                    resultLines.add(line)
                }
            } else if (capturing) {
                resultLines.add(line)
            }
        }

        return if (resultLines.isNotEmpty()) resultLines.joinToString("\n").trimEnd() else fullText
    }

    /**
     * Resolves a note target (e.g. "Meeting Notes", "folder/note", "note.md") to an existing [File] in [vaultRoot].
     */
    fun resolveNoteFile(vaultRoot: File, noteTarget: String): File? {
        val clean = noteTarget.trim().removePrefix("/").removeSuffix(".md")
        if (clean.isBlank()) return null

        // 1. Direct path
        val directFile = File(vaultRoot, "$clean.md")
        if (directFile.exists() && directFile.isFile) return directFile

        val directAsIs = File(vaultRoot, clean)
        if (directAsIs.exists() && directAsIs.isFile) return directAsIs

        // 2. Search recursively by name
        val targetName = if (clean.contains("/")) clean.substringAfterLast("/") else clean
        return try {
            vaultRoot.walkTopDown()
                .filter { it.isFile && it.name.equals("$targetName.md", ignoreCase = true) }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
