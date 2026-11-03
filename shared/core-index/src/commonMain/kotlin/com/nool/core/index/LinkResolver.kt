package com.nool.core.index

import com.nool.core.model.NoteTitleAndPath

object LinkResolver {

    /**
     * Resolves a wiki-link target into a relative vault path (e.g. "Work/Project Alpha.md").
     *
     * Resolution order:
     * 1. Path Match: If target contains '/' (e.g. "Folder/Note"), match relative path directly.
     * 2. Title Match (case-insensitive): Match file name without extension.
     *    - If duplicates exist in different folders, prefer the sibling note in the same folder
     *      as [sourceNotePath], then the root folder, then shortest path.
     * 3. Alias Match (case-insensitive): Match any entry in frontmatter `aliases`.
     * 4. Unresolved: Returns null (indicating a broken or uncreated link).
     */
    fun resolveLink(
        rawTarget: String,
        sourceNotePath: String,
        allNotes: List<NoteTitleAndPath>
    ): String? {
        val cleanTarget = rawTarget.trim()
            .removePrefix("/")
            .removeSuffix(".md")
            .replace('\\', '/')

        if (cleanTarget.isEmpty()) return null

        val normalizedSource = sourceNotePath.replace('\\', '/')
        val sourceFolder = if (normalizedSource.contains('/')) {
            normalizedSource.substringBeforeLast('/')
        } else {
            ""
        }

        // 1. Direct path match if target has slashes
        if (cleanTarget.contains("/")) {
            val directMatch = allNotes.firstOrNull {
                it.relativePath.removeSuffix(".md").equals(cleanTarget, ignoreCase = true)
            }
            if (directMatch != null) return directMatch.relativePath
        }

        // 2. Title match (case-insensitive)
        val titleMatches = allNotes.filter {
            it.title.equals(cleanTarget, ignoreCase = true)
        }

        if (titleMatches.isNotEmpty()) {
            if (titleMatches.size == 1) {
                return titleMatches.first().relativePath
            }

            // Disambiguation for duplicate titles:
            // Prefer sibling in same folder
            val sibling = titleMatches.firstOrNull {
                val folder = if (it.relativePath.contains('/')) {
                    it.relativePath.replace('\\', '/').substringBeforeLast('/')
                } else {
                    ""
                }
                folder == sourceFolder
            }
            if (sibling != null) return sibling.relativePath

            // Prefer root folder
            val rootItem = titleMatches.firstOrNull {
                !it.relativePath.replace('\\', '/').contains("/")
            }
            if (rootItem != null) return rootItem.relativePath

            // Fallback: shortest path
            return titleMatches.minByOrNull { it.relativePath.length }!!.relativePath
        }

        // 3. Alias match (case-insensitive)
        val aliasMatch = allNotes.firstOrNull { note ->
            if (note.aliases.isBlank()) false
            else {
                note.aliases.split(",")
                    .map { it.trim().lowercase() }
                    .contains(cleanTarget.lowercase())
            }
        }
        if (aliasMatch != null) return aliasMatch.relativePath

        // 4. Broken / Uncreated note
        return null
    }
}
