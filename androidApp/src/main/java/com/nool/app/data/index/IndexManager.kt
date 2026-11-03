package com.nool.app.data.index

import com.nool.core.index.SqlDelightIndexManager
import com.nool.core.index.db.NoolDatabase
import com.nool.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

typealias SearchScope = com.nool.core.model.SearchScope
typealias BacklinkItem = com.nool.core.model.BacklinkItem
typealias UnlinkedMentionItem = com.nool.core.model.UnlinkedMentionItem
typealias NoteTitleAndPath = com.nool.core.model.NoteTitleAndPath

class IndexManager(
    val vaultRoot: File,
    val sqlDelightManager: SqlDelightIndexManager
) {
    val isIndexing: StateFlow<Boolean> = sqlDelightManager.isIndexing
    val indexProgress: StateFlow<Float> = sqlDelightManager.indexProgress

    suspend fun indexFile(file: File): Unit = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile || !file.name.endsWith(".md", ignoreCase = true)) {
            return@withContext
        }
        val relative = file.relativeTo(vaultRoot).path.replace('\\', '/')
        if (relative.startsWith(".") || relative.startsWith("attachments/")) {
            return@withContext
        }

        val content = try { file.readText() } catch (e: Exception) { "" }
        sqlDelightManager.indexNoteContent(
            relativePath = relative,
            content = content,
            lastModified = file.lastModified(),
            size = file.length()
        )
    }

    suspend fun removeFile(relativePath: String): Unit = withContext(Dispatchers.IO) {
        val clean = relativePath.trim().removePrefix("/").replace('\\', '/')
        sqlDelightManager.removeNote(clean)
    }

    suspend fun syncIndex(): Int = withContext(Dispatchers.IO) {
        if (!vaultRoot.exists()) return@withContext 0
        try {
            val diskFiles = mutableListOf<File>()

            fun scanDir(dir: File) {
                val children = dir.listFiles() ?: return
                for (child in children) {
                    if (child.name.startsWith(".") || child.name == "attachments") continue
                    if (child.isDirectory) {
                        scanDir(child)
                    } else if (child.isFile && child.name.endsWith(".md", ignoreCase = true)) {
                        diskFiles.add(child)
                    }
                }
            }
            scanDir(vaultRoot)

            val diskPaths = mutableSetOf<String>()
            var updatedCount = 0

            val allDbNotes = sqlDelightManager.getAllNotes().associateBy { it.relativePath }
            for (file in diskFiles) {
                val relative = file.relativeTo(vaultRoot).path.replace('\\', '/')
                diskPaths.add(relative)
                val existing = allDbNotes[relative]
                if (existing == null || existing.lastModified != file.lastModified() || existing.size != file.length()) {
                    indexFile(file)
                    updatedCount++
                }
            }
            for (dbNote in allDbNotes.values) {
                if (!diskPaths.contains(dbNote.relativePath)) {
                    removeFile(dbNote.relativePath)
                    updatedCount++
                }
            }

            updatedCount
        } finally {
            sqlDelightManager.updateProgress(1f)
        }
    }

    suspend fun rebuildIndex(): Int = withContext(Dispatchers.IO) {
        try {
            sqlDelightManager.clearAll()
            val diskFiles = mutableListOf<File>()
            fun scanDir(dir: File) {
                val children = dir.listFiles() ?: return
                for (child in children) {
                    if (child.name.startsWith(".") || child.name == "attachments") continue
                    if (child.isDirectory) scanDir(child)
                    else if (child.isFile && child.name.endsWith(".md", ignoreCase = true)) {
                        diskFiles.add(child)
                    }
                }
            }
            if (vaultRoot.exists()) scanDir(vaultRoot)

            val total = diskFiles.size
            diskFiles.forEachIndexed { index, file ->
                indexFile(file)
                val progress = if (total > 0) (index + 1).toFloat() / total.toFloat() else 1f
                sqlDelightManager.updateProgress(progress)
            }
            sqlDelightManager.updateProgress(1f)
            total
        } finally {
            sqlDelightManager.updateProgress(1f)
        }
    }

    suspend fun getBacklinks(targetRelativePath: String): List<BacklinkItem> = withContext(Dispatchers.IO) {
        sqlDelightManager.getBacklinks(targetRelativePath)
    }

    suspend fun getUnlinkedMentions(targetRelativePath: String): List<UnlinkedMentionItem> = withContext(Dispatchers.IO) {
        sqlDelightManager.getUnlinkedMentions(targetRelativePath)
    }

    suspend fun convertUnlinkedMention(
        sourceRelativePath: String,
        mentionText: String,
        targetNoteTitle: String
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(vaultRoot, sourceRelativePath)
        if (!file.exists() || !file.isFile) return@withContext false
        val content = file.readText()

        val regex = Regex("""(?<!\[\[)\b${Regex.escape(mentionText)}\b(?!\]\])""")
        val match = regex.find(content) ?: return@withContext false

        val linkText = if (mentionText.equals(targetNoteTitle, ignoreCase = true)) {
            "[[$targetNoteTitle]]"
        } else {
            "[[$targetNoteTitle|$mentionText]]"
        }

        val newContent = content.replaceRange(match.range, linkText)
        atomicWrite(file, newContent)
        indexFile(file)
        true
    }

    suspend fun updateLinksOnNoteRename(
        oldRelativePath: String,
        newRelativePath: String
    ): Int = withContext(Dispatchers.IO) {
        val oldTitle = File(oldRelativePath).nameWithoutExtension
        val newTitle = File(newRelativePath).nameWithoutExtension

        if (oldTitle == newTitle) return@withContext 0

        val referencingSources = sqlDelightManager.database.noolIndexQueries
            .getReferencingSourcePaths(oldTitle, oldRelativePath).executeAsList()

        var updatedCount = 0
        val linkRegex = Regex("""(!?\[\[)\s*${Regex.escape(oldTitle)}\s*([#|\]])""", RegexOption.IGNORE_CASE)

        for (sourcePath in referencingSources) {
            val sourceFile = File(vaultRoot, sourcePath)
            if (sourceFile.exists() && sourceFile.isFile) {
                val oldText = sourceFile.readText()
                val newText = linkRegex.replace(oldText) { match ->
                    val prefix = match.groupValues[1]
                    val suffix = match.groupValues[2]
                    "$prefix$newTitle$suffix"
                }
                if (oldText != newText) {
                    atomicWrite(sourceFile, newText)
                    indexFile(sourceFile)
                    updatedCount++
                }
            }
        }
        updatedCount
    }

    private fun atomicWrite(file: File, content: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(content)
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    suspend fun search(
        query: String,
        folderFilter: String? = null,
        tagFilter: String? = null,
        typeFilter: String? = null,
        scope: SearchScope = SearchScope.ALL
    ): List<NoteEntity> = withContext(Dispatchers.IO) {
        val indexedNotes = sqlDelightManager.search(
            query = query,
            folderFilter = folderFilter,
            tagFilter = tagFilter,
            typeFilter = typeFilter,
            scope = scope
        )
        indexedNotes.map { it.toEntity() }
    }

    suspend fun getAllTagsWithCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        sqlDelightManager.getAllTagsWithCounts()
    }

    suspend fun getNoteCatalog(): List<NoteTitleAndPath> = withContext(Dispatchers.IO) {
        sqlDelightManager.getNoteCatalog().map {
            NoteTitleAndPath(
                title = it.title,
                relativePath = it.relativePath,
                aliases = it.aliases
            )
        }
    }

    suspend fun getGraphData(
        localNotePath: String? = null,
        localDepth: Int = 1
    ): com.nool.core.model.GraphData = withContext(Dispatchers.IO) {
        sqlDelightManager.getGraphData(localNotePath, localDepth)
    }
}
