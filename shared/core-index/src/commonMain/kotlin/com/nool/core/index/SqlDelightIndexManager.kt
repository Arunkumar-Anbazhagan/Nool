package com.nool.core.index

import com.nool.core.index.db.Links
import com.nool.core.index.db.NoolDatabase
import com.nool.core.index.db.Notes
import com.nool.core.markdown.FrontmatterAndTagParser
import com.nool.core.markdown.WikiLinkParser
import com.nool.core.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SqlDelightIndexManager(
    val database: NoolDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _indexProgress = MutableStateFlow(1f)
    val indexProgress: StateFlow<Float> = _indexProgress.asStateFlow()

    fun updateProgress(progress: Float) {
        _indexProgress.value = progress
    }

    suspend fun indexNoteContent(
        relativePath: String,
        content: String,
        lastModified: Long,
        size: Long
    ): Unit = withContext(ioDispatcher) {
        val cleanRelative = relativePath.trim().removePrefix("/").replace('\\', '/')
        if (cleanRelative.isEmpty() || cleanRelative.startsWith(".") || cleanRelative.startsWith("attachments/")) {
            return@withContext
        }

        val fileName = if (cleanRelative.contains('/')) cleanRelative.substringAfterLast('/') else cleanRelative
        val title = fileName.removeSuffix(".md")
        val folder = if (cleanRelative.contains('/')) cleanRelative.substringBeforeLast('/') else ""

        val parsed = FrontmatterAndTagParser.parse(content)

        database.noolIndexQueries.upsertNote(
            relativePath = cleanRelative,
            title = title,
            folderPath = folder,
            content = parsed.bodyText,
            excerpt = extractExcerpt(parsed.bodyText),
            lastModified = lastModified,
            createdTimestamp = lastModified,
            size = size,
            isPinned = parsed.isPinned,
            isFavorite = parsed.isFavorite,
            isArchived = parsed.isArchived,
            tags = parsed.tags.joinToString(","),
            aliases = parsed.aliases.joinToString(","),
            noteType = parsed.noteType
        )

        // Index wiki-links
        val allNotes = getNoteCatalog()
        val parsedLinks = WikiLinkParser.parseLinks(content)

        database.noolIndexQueries.deleteLinksForSource(cleanRelative)
        for (link in parsedLinks) {
            val resolved = LinkResolver.resolveLink(link.target, cleanRelative, allNotes)
            database.noolIndexQueries.insertLink(
                sourcePath = cleanRelative,
                rawTarget = link.target + (if (link.heading != null) "#${link.heading}" else "") + (if (link.alias != null) "|${link.alias}" else ""),
                targetTitle = link.target,
                targetHeading = link.heading,
                alias = link.alias,
                isEmbed = link.isEmbed,
                resolvedPath = resolved
            )
        }
    }

    suspend fun removeNote(relativePath: String): Unit = withContext(ioDispatcher) {
        val clean = relativePath.trim().removePrefix("/").replace('\\', '/')
        database.noolIndexQueries.deleteNote(clean)
        database.noolIndexQueries.deleteLinksForSource(clean)
    }

    suspend fun clearAll(): Unit = withContext(ioDispatcher) {
        database.noolIndexQueries.clearAllNotes()
        database.noolIndexQueries.clearAllLinks()
    }

    suspend fun getNote(relativePath: String): IndexedNote? = withContext(ioDispatcher) {
        val clean = relativePath.trim().removePrefix("/").replace('\\', '/')
        database.noolIndexQueries.getNote(clean).executeAsOneOrNull()?.toIndexedNote()
    }

    suspend fun getAllNotes(): List<IndexedNote> = withContext(ioDispatcher) {
        database.noolIndexQueries.getAllNotes().executeAsList().map { it.toIndexedNote() }
    }

    suspend fun getNoteCatalog(): List<NoteTitleAndPath> = withContext(ioDispatcher) {
        database.noolIndexQueries.getAllNoteTitlesAndPaths().executeAsList().map {
            NoteTitleAndPath(
                title = it.title,
                relativePath = it.relativePath,
                aliases = it.aliases
            )
        }
    }

    suspend fun getBacklinks(targetRelativePath: String): List<BacklinkItem> = withContext(ioDispatcher) {
        val clean = targetRelativePath.trim().removePrefix("/").replace('\\', '/')
        val note = database.noolIndexQueries.getNote(clean).executeAsOneOrNull() ?: return@withContext emptyList()
        val links = database.noolIndexQueries.getBacklinksForNote(
            resolvedPath = note.relativePath,
            targetTitle = note.title
        ).executeAsList()

        links.map { link ->
            val sourceNote = database.noolIndexQueries.getNote(link.sourcePath).executeAsOneOrNull()
            val sourceTitle = sourceNote?.title ?: link.sourcePath.substringAfterLast('/').removeSuffix(".md")
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

    suspend fun getUnlinkedMentions(targetRelativePath: String): List<UnlinkedMentionItem> = withContext(ioDispatcher) {
        val clean = targetRelativePath.trim().removePrefix("/").replace('\\', '/')
        val note = database.noolIndexQueries.getNote(clean).executeAsOneOrNull() ?: return@withContext emptyList()
        val allNotes = database.noolIndexQueries.getAllNotes().executeAsList()

        val candidateTerms = mutableListOf(note.title)
        if (note.aliases.isNotBlank()) {
            val aliasList = note.aliases.split(",").map { it.trim() }.filter { it.length >= 2 }
            candidateTerms.addAll(aliasList)
        }

        val referencingPaths = database.noolIndexQueries.getReferencingSourcePaths(
            targetTitle = note.title,
            resolvedPath = note.relativePath
        ).executeAsList().toSet()

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

    suspend fun search(
        query: String,
        folderFilter: String? = null,
        tagFilter: String? = null,
        typeFilter: String? = null,
        scope: SearchScope = SearchScope.ALL
    ): List<IndexedNote> = withContext(ioDispatcher) {
        val trimmedQuery = query.trim()
        val parsedQuery = SearchQueryParser.parse(trimmedQuery)

        val rawResults = if (parsedQuery.hasOperators) {
            val ftsQuery = SearchQueryParser.toFtsMatchQuery(parsedQuery)
            if (ftsQuery != null) {
                try {
                    database.noolIndexQueries.searchFts(ftsQuery).executeAsList().map { it.toIndexedNote() }
                } catch (e: Exception) {
                    database.noolIndexQueries.getAllNotes().executeAsList().map { it.toIndexedNote() }
                }
            } else {
                database.noolIndexQueries.getAllNotes().executeAsList().map { it.toIndexedNote() }
            }
        } else if (trimmedQuery.isNotEmpty()) {
            val ftsQuery = sanitizeFtsQuery(trimmedQuery)
            try {
                database.noolIndexQueries.searchFts(ftsQuery).executeAsList().map { it.toIndexedNote() }
            } catch (e: Exception) {
                val lowerQuery = trimmedQuery.lowercase()
                database.noolIndexQueries.getAllNotes().executeAsList().map { it.toIndexedNote() }.filter {
                    it.title.lowercase().contains(lowerQuery) ||
                    it.content.lowercase().contains(lowerQuery) ||
                    it.tags.any { tag -> tag.lowercase().contains(lowerQuery) }
                }
            }
        } else {
            database.noolIndexQueries.getAllNotes().executeAsList().map { it.toIndexedNote() }
        }

        rawResults.filter { note ->
            val matchesFolder = folderFilter.isNullOrBlank() || note.folderPath == folderFilter || note.folderPath.startsWith("$folderFilter/")
            val matchesTag = tagFilter.isNullOrBlank() || note.tags.map { it.trim().lowercase() }.contains(tagFilter.lowercase())
            val matchesType = typeFilter.isNullOrBlank() || note.noteType.lowercase() == typeFilter.lowercase()

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

    suspend fun getAllTagsWithCounts(): Map<String, Int> = withContext(ioDispatcher) {
        val rawTags = database.noolIndexQueries.getAllTagsRaw().executeAsList()
        val countMap = mutableMapOf<String, Int>()
        for (row in rawTags) {
            val tags = row.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            for (tag in tags) {
                countMap[tag] = (countMap[tag] ?: 0) + 1
            }
        }
        countMap.toSortedMap()
    }

    suspend fun getGraphData(
        localNotePath: String? = null,
        localDepth: Int = 1
    ): GraphData = withContext(ioDispatcher) {
        val notes = database.noolIndexQueries.getAllNotes().executeAsList()
        val rawLinks = database.noolIndexQueries.getAllLinks().executeAsList()

        val allEdges = rawLinks.map { link ->
            GraphEdge(
                source = link.sourcePath,
                target = link.resolvedPath ?: link.targetTitle,
                isEmbed = link.isEmbed,
                isResolved = link.resolvedPath != null
            )
        }

        val keptNodeIds: Set<String> = if (!localNotePath.isNullOrBlank()) {
            val cleanLocal = localNotePath.trim().removePrefix("/").replace('\\', '/')
            val visited = mutableSetOf(cleanLocal)
            var currentLayer = setOf(cleanLocal)
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

        val filteredEdges = allEdges.filter { edge ->
            edge.source in keptNodeIds && (edge.target in keptNodeIds || !edge.isResolved)
        }

        val degreeMap = mutableMapOf<String, Int>()
        for (edge in filteredEdges) {
            degreeMap[edge.source] = (degreeMap[edge.source] ?: 0) + 1
            if (edge.isResolved) {
                degreeMap[edge.target] = (degreeMap[edge.target] ?: 0) + 1
            }
        }

        val graphNodes = notes.filter { it.relativePath in keptNodeIds }.map { entity ->
            GraphNode(
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

        GraphData(
            nodes = graphNodes,
            edges = filteredEdges
        )
    }

    private fun Notes.toIndexedNote(): IndexedNote = IndexedNote(
        id = id,
        relativePath = relativePath,
        title = title,
        folderPath = folderPath,
        content = content,
        excerpt = excerpt,
        lastModified = lastModified,
        createdTimestamp = createdTimestamp,
        size = size,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
        aliases = if (aliases.isBlank()) emptyList() else aliases.split(",").map { it.trim() }.filter { it.isNotBlank() },
        noteType = noteType
    )

    private fun sanitizeFtsQuery(query: String): String {
        val sanitized = query.replace("\"", "\"\"").replace("'", "''")
        val terms = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return "*"
        return terms.joinToString(" ") { term ->
            val clean = term.trimEnd('*')
            if (clean.isNotEmpty()) "$clean*" else "*"
        }
    }

    private fun extractExcerpt(text: String, maxChars: Int = 120): String {
        val clean = text.lines()
            .map { it.trim().removePrefix("#").trimStart() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return if (clean.length > maxChars) clean.substring(0, maxChars) + "..." else clean
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
}
