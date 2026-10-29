package com.nool.app.data.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class SearchScope {
    ALL,
    PINNED,
    FAVORITES,
    ARCHIVED
}

data class BacklinkItem(
    val sourcePath: String,
    val sourceTitle: String,
    val snippet: String,
    val alias: String? = null,
    val isEmbed: Boolean = false
)

data class UnlinkedMentionItem(
    val sourcePath: String,
    val sourceTitle: String,
    val matchedText: String,
    val snippet: String
)

class IndexManager(
    private val vaultRoot: File,
    private val indexDao: IndexDao
) {
    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    suspend fun indexFile(file: File): Unit = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile || !file.name.endsWith(".md", ignoreCase = true)) {
            return@withContext
        }
        val relative = file.relativeTo(vaultRoot).path.replace('\\', '/')
        if (relative.startsWith(".") || relative.startsWith("attachments/")) {
            return@withContext
        }

        val content = try { file.readText() } catch (e: Exception) { "" }
        val parsed = FrontmatterAndTagParser.parse(content)
        val folder = file.parentFile?.relativeTo(vaultRoot)?.path?.replace('\\', '/') ?: ""
        val cleanFolder = if (folder == ".") "" else folder

        val existing = indexDao.getNote(relative)
        val entity = NoteEntity(
            rowid = existing?.rowid ?: 0,
            relativePath = relative,
            title = file.nameWithoutExtension,
            folderPath = cleanFolder,
            content = parsed.bodyText,
            excerpt = extractExcerpt(parsed.bodyText),
            lastModified = file.lastModified(),
            createdTimestamp = file.lastModified(),
            size = file.length(),
            isPinned = parsed.isPinned,
            isFavorite = parsed.isFavorite,
            isArchived = parsed.isArchived,
            tags = parsed.tags.joinToString(","),
            aliases = parsed.aliases.joinToString(","),
            noteType = parsed.noteType
        )

        // 1. Upsert note metadata
        indexDao.upsertNote(entity)

        // 2. Index wiki-links
        val allNotes = indexDao.getAllNoteTitlesAndPaths()
        val parsedLinks = WikiLinkParser.parseLinks(content)
        val linkEntities = parsedLinks.map { link ->
            val resolved = LinkResolver.resolveLink(link.target, relative, allNotes)
            LinkEntity(
                sourcePath = relative,
                rawTarget = link.target + (if (link.heading != null) "#${link.heading}" else "") + (if (link.alias != null) "|${link.alias}" else ""),
                targetTitle = link.target,
                targetHeading = link.heading,
                alias = link.alias,
                isEmbed = link.isEmbed,
                resolvedPath = resolved
            )
        }

        indexDao.deleteLinksForSource(relative)
        if (linkEntities.isNotEmpty()) {
            indexDao.insertLinks(linkEntities)
        }
    }

    suspend fun removeFile(relativePath: String): Unit = withContext(Dispatchers.IO) {
        val clean = relativePath.trim().removePrefix("/").replace('\\', '/')
        indexDao.deleteNote(clean)
        indexDao.deleteLinksForSource(clean)
    }

    private data class FileScanData(
        val file: File,
        val relative: String,
        val content: String
    )

    suspend fun syncIndex(): Int = withContext(Dispatchers.IO) {
        if (!vaultRoot.exists()) return@withContext 0
        _isIndexing.value = true
        try {
            val allDbNotes = indexDao.getAllNotes().associateBy { it.relativePath }
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
            _isIndexing.value = false
        }
    }

    suspend fun rebuildIndex(): Int = withContext(Dispatchers.IO) {
        _isIndexing.value = true
        try {
            indexDao.clearAll()
            indexDao.clearAllLinks()

            val entities = mutableListOf<NoteEntity>()
            val scanData = mutableListOf<FileScanData>()

            fun scanDir(dir: File) {
                val children = dir.listFiles() ?: return
                for (child in children) {
                    if (child.name.startsWith(".") || child.name == "attachments") continue
                    if (child.isDirectory) {
                        scanDir(child)
                    } else if (child.isFile && child.name.endsWith(".md", ignoreCase = true)) {
                        val relative = child.relativeTo(vaultRoot).path.replace('\\', '/')
                        val content = try { child.readText() } catch (e: Exception) { "" }
                        val parsed = FrontmatterAndTagParser.parse(content)
                        val folder = child.parentFile?.relativeTo(vaultRoot)?.path?.replace('\\', '/') ?: ""
                        val cleanFolder = if (folder == ".") "" else folder

                        scanData.add(FileScanData(child, relative, content))

                        entities.add(
                            NoteEntity(
                                relativePath = relative,
                                title = child.nameWithoutExtension,
                                folderPath = cleanFolder,
                                content = parsed.bodyText,
                                excerpt = extractExcerpt(parsed.bodyText),
                                lastModified = child.lastModified(),
                                createdTimestamp = child.lastModified(),
                                size = child.length(),
                                isPinned = parsed.isPinned,
                                isFavorite = parsed.isFavorite,
                                isArchived = parsed.isArchived,
                                tags = parsed.tags.joinToString(","),
                                aliases = parsed.aliases.joinToString(","),
                                noteType = parsed.noteType
                            )
                        )
                    }
                }
            }

            scanDir(vaultRoot)
            if (entities.isNotEmpty()) {
                indexDao.upsertNotes(entities)
            }

            // Phase 2: Index all links with full note catalog available for link resolution
            val allNoteTitles = indexDao.getAllNoteTitlesAndPaths()
            val allLinkEntities = mutableListOf<LinkEntity>()

            for (item in scanData) {
                val parsedLinks = WikiLinkParser.parseLinks(item.content)
                for (link in parsedLinks) {
                    val resolved = LinkResolver.resolveLink(link.target, item.relative, allNoteTitles)
                    allLinkEntities.add(
                        LinkEntity(
                            sourcePath = item.relative,
                            rawTarget = link.target + (if (link.heading != null) "#${link.heading}" else "") + (if (link.alias != null) "|${link.alias}" else ""),
                            targetTitle = link.target,
                            targetHeading = link.heading,
                            alias = link.alias,
                            isEmbed = link.isEmbed,
                            resolvedPath = resolved
                        )
                    )
                }
            }

            if (allLinkEntities.isNotEmpty()) {
                indexDao.insertLinks(allLinkEntities)
            }

            entities.size
        } finally {
            _isIndexing.value = false
        }
    }

    suspend fun getBacklinks(targetRelativePath: String): List<BacklinkItem> = withContext(Dispatchers.IO) {
        val note = indexDao.getNote(targetRelativePath) ?: return@withContext emptyList()
        val links = indexDao.getBacklinksForNote(note.title, note.relativePath)
        links.map { link ->
            val sourceNote = indexDao.getNote(link.sourcePath)
            val sourceTitle = sourceNote?.title ?: File(link.sourcePath).nameWithoutExtension
            val snippet = sourceNote?.content?.let { extractSurroundingSnippet(it, link.targetTitle) } ?: ""
            BacklinkItem(
                sourcePath = link.sourcePath,
                sourceTitle = sourceTitle,
                snippet = snippet,
                alias = link.alias,
                isEmbed = link.isEmbed
            )
        }
    }

    suspend fun getUnlinkedMentions(targetRelativePath: String): List<UnlinkedMentionItem> = withContext(Dispatchers.IO) {
        val note = indexDao.getNote(targetRelativePath) ?: return@withContext emptyList()
        val allNotes = indexDao.getAllNotes()

        val candidateTerms = mutableListOf(note.title)
        if (note.aliases.isNotBlank()) {
            val aliasList = note.aliases.split(",").map { it.trim() }.filter { it.length >= 2 }
            candidateTerms.addAll(aliasList)
        }

        val referencingPaths = indexDao.getReferencingSourcePaths(note.title, note.relativePath).toSet()
        val results = mutableListOf<UnlinkedMentionItem>()

        for (candidateNote in allNotes) {
            if (candidateNote.relativePath == note.relativePath) continue
            if (referencingPaths.contains(candidateNote.relativePath)) continue

            for (term in candidateTerms) {
                val termRegex = Regex("""(?<!\[\[)\b${Regex.escape(term)}\b(?!\]\])""", RegexOption.IGNORE_CASE)
                if (termRegex.containsMatchIn(candidateNote.content)) {
                    val snippet = extractSurroundingSnippet(candidateNote.content, term)
                    results.add(
                        UnlinkedMentionItem(
                            sourcePath = candidateNote.relativePath,
                            sourceTitle = candidateNote.title,
                            matchedText = term,
                            snippet = snippet
                        )
                    )
                    break
                }
            }
        }
        results
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

        val referencingSources = indexDao.getReferencingSourcePaths(oldTitle, oldRelativePath)
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

    private fun extractSurroundingSnippet(fullText: String, targetTerm: String): String {
        val index = fullText.indexOf(targetTerm, ignoreCase = true)
        if (index == -1) return extractExcerpt(fullText)
        val start = (index - 40).coerceAtLeast(0)
        val end = (index + targetTerm.length + 60).coerceAtMost(fullText.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < fullText.length) "..." else ""
        return prefix + fullText.substring(start, end).replace('\n', ' ') + suffix
    }

    suspend fun search(
        query: String,
        folderFilter: String? = null,
        tagFilter: String? = null,
        typeFilter: String? = null,
        scope: SearchScope = SearchScope.ALL
    ): List<NoteEntity> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        val parsedQuery = SearchQueryParser.parse(trimmedQuery)

        val rawResults = if (parsedQuery.hasOperators) {
            indexDao.getAllNotes()
        } else if (trimmedQuery.isNotEmpty()) {
            val ftsQuery = sanitizeFtsQuery(trimmedQuery)
            try {
                indexDao.searchFts(ftsQuery)
            } catch (e: Exception) {
                indexDao.getAllNotes().filter {
                    it.title.contains(trimmedQuery, ignoreCase = true) ||
                    it.content.contains(trimmedQuery, ignoreCase = true) ||
                    it.tags.contains(trimmedQuery, ignoreCase = true)
                }
            }
        } else {
            indexDao.getAllNotes()
        }

        rawResults.filter { note ->
            val matchesFolder = folderFilter.isNullOrBlank() || note.folderPath == folderFilter || note.folderPath.startsWith("$folderFilter/")
            val matchesTag = tagFilter.isNullOrBlank() || note.tags.split(",").map { it.trim().lowercase() }.contains(tagFilter.lowercase())
            val matchesType = typeFilter.isNullOrBlank() || note.noteType.equals(typeFilter, ignoreCase = true)

            val matchesScope = when (scope) {
                SearchScope.ALL -> !note.isArchived
                SearchScope.PINNED -> note.isPinned && !note.isArchived
                SearchScope.FAVORITES -> note.isFavorite && !note.isArchived
                SearchScope.ARCHIVED -> note.isArchived
            }

            val matchesOperators = SearchQueryParser.matches(note, parsedQuery)

            matchesFolder && matchesTag && matchesType && matchesScope && matchesOperators
        }
    }

    suspend fun getAllTagsWithCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        val rawTags = indexDao.getAllTagsRaw()
        val countMap = mutableMapOf<String, Int>()
        for (row in rawTags) {
            val tags = row.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            for (tag in tags) {
                countMap[tag] = (countMap[tag] ?: 0) + 1
            }
        }
        countMap.toSortedMap()
    }

    suspend fun getNoteCatalog(): List<NoteTitleAndPath> = withContext(Dispatchers.IO) {
        indexDao.getAllNoteTitlesAndPaths()
    }

    suspend fun getGraphData(
        localNotePath: String? = null,
        localDepth: Int = 1
    ): com.nool.app.domain.model.GraphData = withContext(Dispatchers.IO) {
        val notes = indexDao.getAllNotes()
        val rawLinks = indexDao.getAllLinks()

        val allEdges = rawLinks.map { link ->
            com.nool.app.domain.model.GraphEdge(
                source = link.sourcePath,
                target = link.resolvedPath ?: link.targetTitle,
                isEmbed = link.isEmbed,
                isResolved = link.resolvedPath != null
            )
        }

        val keptNodeIds: Set<String> = if (!localNotePath.isNullOrBlank()) {
            val visited = mutableSetOf(localNotePath)
            var currentLayer = setOf(localNotePath)
            val maxDepth = localDepth.coerceIn(1, 3)

            for (depth in 1..maxDepth) {
                val nextLayer = mutableSetOf<String>()
                for (edge in allEdges) {
                    if (edge.source in currentLayer && edge.isResolved && edge.target !in visited) {
                        nextLayer.add(edge.target)
                    }
                    if (edge.target in currentLayer && edge.source !in visited) {
                        nextLayer.add(edge.source)
                    }
                }
                visited.addAll(nextLayer)
                currentLayer = nextLayer
                if (currentLayer.isEmpty()) break
            }
            visited
        } else {
            notes.map { it.relativePath }.toSet()
        }

        // Filter edges where both ends are in keptNodeIds or source is in keptNodeIds
        val filteredEdges = allEdges.filter { edge ->
            edge.source in keptNodeIds && (edge.target in keptNodeIds || !edge.isResolved)
        }

        // Calculate degrees
        val degreeMap = mutableMapOf<String, Int>()
        for (edge in filteredEdges) {
            degreeMap[edge.source] = (degreeMap[edge.source] ?: 0) + 1
            if (edge.isResolved) {
                degreeMap[edge.target] = (degreeMap[edge.target] ?: 0) + 1
            }
        }

        val graphNodes = notes.filter { it.relativePath in keptNodeIds }.map { entity ->
            com.nool.app.domain.model.GraphNode(
                id = entity.relativePath,
                label = entity.title,
                folder = entity.folderPath,
                tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                noteType = entity.noteType,
                isPinned = entity.isPinned,
                isFavorite = entity.isFavorite,
                degree = degreeMap[entity.relativePath] ?: 0
            )
        }

        com.nool.app.domain.model.GraphData(
            nodes = graphNodes,
            edges = filteredEdges
        )
    }

    private fun sanitizeFtsQuery(query: String): String {
        val sanitized = query.replace("\"", "\"\"").replace("'", "''")
        val terms = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return "*"
        return terms.joinToString(" ") { "$it*" }
    }

    private fun extractExcerpt(text: String, maxChars: Int = 120): String {
        val clean = text.lines()
            .map { it.trim().removePrefix("#").trimStart() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return if (clean.length > maxChars) clean.substring(0, maxChars) + "..." else clean
    }
}
