package com.nool.app.ui.editor.embed

import java.io.File

object NoteEmbedHelper {

    const val MAX_RECURSION_DEPTH = 3

    /**
     * Extracts the markdown content starting from [targetHeading] until the next heading
     * of equal or higher level (smaller or equal level number) or EOF.
     */
    fun extractHeadingSection(fullText: String, targetHeading: String): String =
        com.nool.core.markdown.NoteEmbedHelper.extractHeadingSection(fullText, targetHeading)

    /**
     * Extracts a block referenced by [blockId] (matching Obsidian syntax `^blockId`).
     */
    fun extractBlock(fullText: String, blockId: String): String? =
        com.nool.core.markdown.NoteEmbedHelper.extractBlock(fullText, blockId)

    /**
     * Resolves a note target (e.g. "Meeting Notes", "folder/note", "note.md", or folder note "folder")
     * to an existing [File] in [vaultRoot].
     */
    fun resolveNoteFile(vaultRoot: File, noteTarget: String): File? {
        val clean = noteTarget.trim().removePrefix("/").removeSuffix(".md")
        if (clean.isBlank()) return null

        // 1. Direct path as file
        val directFile = File(vaultRoot, "$clean.md")
        if (directFile.exists() && directFile.isFile) return directFile

        val directAsIs = File(vaultRoot, clean)
        if (directAsIs.exists() && directAsIs.isFile) return directAsIs

        // 2. Folder notes convention (folder/folder.md, folder/_index.md, folder/index.md)
        if (directAsIs.exists() && directAsIs.isDirectory) {
            val folderName = directAsIs.name
            val folderNote = File(directAsIs, "$folderName.md")
            if (folderNote.exists() && folderNote.isFile) return folderNote
            val underscoreIndex = File(directAsIs, "_index.md")
            if (underscoreIndex.exists() && underscoreIndex.isFile) return underscoreIndex
            val plainIndex = File(directAsIs, "index.md")
            if (plainIndex.exists() && plainIndex.isFile) return plainIndex
        }

        // 3. Search recursively by name
        val targetName = if (clean.contains("/")) clean.substringAfterLast("/") else clean
        val found = try {
            vaultRoot.walkTopDown()
                .filter { it.isFile && it.name.equals("$targetName.md", ignoreCase = true) }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
        if (found != null) return found

        // 4. Search recursively for folder note
        return try {
            vaultRoot.walkTopDown()
                .filter { it.isDirectory && it.name.equals(targetName, ignoreCase = true) }
                .mapNotNull { dir ->
                    val fn = File(dir, "${dir.name}.md")
                    if (fn.exists() && fn.isFile) return@mapNotNull fn
                    val uIdx = File(dir, "_index.md")
                    if (uIdx.exists() && uIdx.isFile) return@mapNotNull uIdx
                    val pIdx = File(dir, "index.md")
                    if (pIdx.exists() && pIdx.isFile) return@mapNotNull pIdx
                    null
                }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
