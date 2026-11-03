package com.nool.core.index

import com.nool.core.model.IndexedNote

data class SearchQuery(
    val rawQuery: String,
    val includeTags: List<String> = emptyList(),
    val excludeTags: List<String> = emptyList(),
    val includePaths: List<String> = emptyList(),
    val excludePaths: List<String> = emptyList(),
    val includeFiles: List<String> = emptyList(),
    val excludeFiles: List<String> = emptyList(),
    val includeFolders: List<String> = emptyList(),
    val excludeFolders: List<String> = emptyList(),
    val includeTitles: List<String> = emptyList(),
    val excludeTitles: List<String> = emptyList(),
    val exactPhrases: List<String> = emptyList(),
    val excludePhrases: List<String> = emptyList(),
    val includeTerms: List<String> = emptyList(),
    val excludeTerms: List<String> = emptyList()
) {
    val hasOperators: Boolean
        get() = includeTags.isNotEmpty() || excludeTags.isNotEmpty() ||
                includePaths.isNotEmpty() || excludePaths.isNotEmpty() ||
                includeFiles.isNotEmpty() || excludeFiles.isNotEmpty() ||
                includeFolders.isNotEmpty() || excludeFolders.isNotEmpty() ||
                includeTitles.isNotEmpty() || excludeTitles.isNotEmpty() ||
                exactPhrases.isNotEmpty() || excludePhrases.isNotEmpty() ||
                excludeTerms.isNotEmpty()
}

object SearchQueryParser {

    private val TOKEN_REGEX = Regex("""(-?[a-zA-Z]+:"[^"]*"|-?"[^"]*"|\S+)""")

    fun parse(query: String): SearchQuery {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchQuery(rawQuery = "")

        val includeTags = mutableListOf<String>()
        val excludeTags = mutableListOf<String>()
        val includePaths = mutableListOf<String>()
        val excludePaths = mutableListOf<String>()
        val includeFiles = mutableListOf<String>()
        val excludeFiles = mutableListOf<String>()
        val includeFolders = mutableListOf<String>()
        val excludeFolders = mutableListOf<String>()
        val includeTitles = mutableListOf<String>()
        val excludeTitles = mutableListOf<String>()
        val exactPhrases = mutableListOf<String>()
        val excludePhrases = mutableListOf<String>()
        val includeTerms = mutableListOf<String>()
        val excludeTerms = mutableListOf<String>()

        val matches = TOKEN_REGEX.findAll(trimmed)
        for (match in matches) {
            val token = match.value
            when {
                token.startsWith("-tag:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").removePrefix("#").trim()
                    if (rawVal.isNotEmpty()) excludeTags.add(rawVal.lowercase())
                }
                token.startsWith("tag:", ignoreCase = true) -> {
                    val rawVal = token.substring(4).removeSurrounding("\"").removePrefix("#").trim()
                    if (rawVal.isNotEmpty()) includeTags.add(rawVal.lowercase())
                }
                token.startsWith("-folder:", ignoreCase = true) -> {
                    val rawVal = token.substring(8).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludeFolders.add(rawVal.lowercase())
                }
                token.startsWith("folder:", ignoreCase = true) -> {
                    val rawVal = token.substring(7).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includeFolders.add(rawVal.lowercase())
                }
                token.startsWith("-title:", ignoreCase = true) -> {
                    val rawVal = token.substring(7).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludeTitles.add(rawVal.lowercase())
                }
                token.startsWith("title:", ignoreCase = true) -> {
                    val rawVal = token.substring(6).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includeTitles.add(rawVal.lowercase())
                }
                token.startsWith("-path:", ignoreCase = true) -> {
                    val rawVal = token.substring(6).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludePaths.add(rawVal.lowercase())
                }
                token.startsWith("path:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includePaths.add(rawVal.lowercase())
                }
                token.startsWith("-file:", ignoreCase = true) -> {
                    val rawVal = token.substring(6).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludeFiles.add(rawVal.lowercase())
                }
                token.startsWith("file:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includeFiles.add(rawVal.lowercase())
                }
                token.startsWith("-\"") && token.endsWith("\"") && token.length > 3 -> {
                    val phrase = token.substring(2, token.length - 1).trim()
                    if (phrase.isNotEmpty()) excludePhrases.add(phrase)
                }
                token.startsWith("\"") && token.endsWith("\"") && token.length > 2 -> {
                    val phrase = token.substring(1, token.length - 1).trim()
                    if (phrase.isNotEmpty()) exactPhrases.add(phrase)
                }
                token.startsWith("-") && token.length > 1 -> {
                    val term = token.substring(1).trim()
                    if (term.isNotEmpty()) excludeTerms.add(term)
                }
                else -> {
                    val term = token.removeSurrounding("\"").trim()
                    if (term.isNotEmpty()) includeTerms.add(term)
                }
            }
        }

        return SearchQuery(
            rawQuery = trimmed,
            includeTags = includeTags,
            excludeTags = excludeTags,
            includePaths = includePaths,
            excludePaths = excludePaths,
            includeFiles = includeFiles,
            excludeFiles = excludeFiles,
            includeFolders = includeFolders,
            excludeFolders = excludeFolders,
            includeTitles = includeTitles,
            excludeTitles = excludeTitles,
            exactPhrases = exactPhrases,
            excludePhrases = excludePhrases,
            includeTerms = includeTerms,
            excludeTerms = excludeTerms
        )
    }

    fun matches(note: IndexedNote, query: SearchQuery): Boolean {
        if (query.rawQuery.isEmpty()) return true

        val noteTags = note.tags.map { it.trim().removePrefix("#").lowercase() }.filter { it.isNotEmpty() }
        val notePathLower = note.relativePath.lowercase()
        val noteFolderLower = note.folderPath.lowercase()
        val noteTitleLower = note.title.lowercase()
        val noteContentLower = note.content.lowercase()

        for (exTag in query.excludeTags) {
            if (noteTags.any { it == exTag || it.startsWith("$exTag/") }) return false
        }
        for (exFolder in query.excludeFolders) {
            if (noteFolderLower == exFolder || noteFolderLower.startsWith("$exFolder/") || noteFolderLower.contains(exFolder)) return false
        }
        for (exTitle in query.excludeTitles) {
            if (noteTitleLower.contains(exTitle)) return false
        }
        for (exPath in query.excludePaths) {
            if (notePathLower.contains(exPath) || noteFolderLower.contains(exPath)) return false
        }
        for (exFile in query.excludeFiles) {
            if (noteTitleLower.contains(exFile) || notePathLower.substringAfterLast("/").contains(exFile)) return false
        }
        for (exPhrase in query.excludePhrases) {
            val exPhraseLower = exPhrase.lowercase()
            if (noteTitleLower.contains(exPhraseLower) || noteContentLower.contains(exPhraseLower)) return false
        }
        for (exTerm in query.excludeTerms) {
            val exTermLower = exTerm.lowercase().trimEnd('*')
            if (noteTitleLower.contains(exTermLower) || noteContentLower.contains(exTermLower) || noteTags.any { it.contains(exTermLower) }) return false
        }
        for (inTag in query.includeTags) {
            // Support hierarchical tag matching (e.g. tag:work matches work/project)
            if (!noteTags.any { it == inTag || it.startsWith("$inTag/") }) return false
        }
        for (inFolder in query.includeFolders) {
            if (noteFolderLower != inFolder && !noteFolderLower.startsWith("$inFolder/") && !noteFolderLower.contains(inFolder)) return false
        }
        for (inTitle in query.includeTitles) {
            if (!noteTitleLower.contains(inTitle)) return false
        }
        for (inPath in query.includePaths) {
            if (!notePathLower.contains(inPath) && !noteFolderLower.contains(inPath)) return false
        }
        for (inFile in query.includeFiles) {
            if (!noteTitleLower.contains(inFile) && !notePathLower.substringAfterLast("/").contains(inFile)) return false
        }
        for (phrase in query.exactPhrases) {
            val phraseLower = phrase.lowercase()
            if (!noteTitleLower.contains(phraseLower) && !noteContentLower.contains(phraseLower)) return false
        }
        for (term in query.includeTerms) {
            val termLower = term.lowercase().trimEnd('*')
            val inTitle = noteTitleLower.contains(termLower)
            val inContent = noteContentLower.contains(termLower)
            val inTags = noteTags.any { it.contains(termLower) }
            if (!inTitle && !inContent && !inTags) return false
        }

        return true
    }

    /**
     * Translates a [SearchQuery] into an FTS5 MATCH expression if applicable,
     * or null if query has no term/phrase/tag constraints suitable for FTS5.
     */
    fun toFtsMatchQuery(query: SearchQuery): String? {
        val clauses = mutableListOf<String>()

        for (phrase in query.exactPhrases) {
            val safe = phrase.replace("\"", "\"\"")
            clauses.add("\"$safe\"")
        }

        for (term in query.includeTerms) {
            val clean = term.replace("\"", "").trim().trimEnd('*')
            if (clean.isNotEmpty()) {
                clauses.add("$clean*")
            }
        }

        for (tag in query.includeTags) {
            val clean = tag.replace("\"", "").trim().trimEnd('*')
            if (clean.isNotEmpty()) {
                clauses.add("tags: $clean*")
            }
        }

        for (title in query.includeTitles) {
            val clean = title.replace("\"", "").trim().trimEnd('*')
            if (clean.isNotEmpty()) {
                clauses.add("title: $clean*")
            }
        }

        for (folder in query.includeFolders) {
            val clean = folder.replace("\"", "").trim().trimEnd('*')
            if (clean.isNotEmpty()) {
                clauses.add("folderPath: $clean*")
            }
        }

        if (clauses.isEmpty()) return null
        return clauses.joinToString(" AND ")
    }
}
