package com.nool.app.data.index

data class SearchQuery(
    val rawQuery: String,
    val includeTags: List<String> = emptyList(),
    val excludeTags: List<String> = emptyList(),
    val includePaths: List<String> = emptyList(),
    val excludePaths: List<String> = emptyList(),
    val includeFiles: List<String> = emptyList(),
    val excludeFiles: List<String> = emptyList(),
    val exactPhrases: List<String> = emptyList(),
    val excludePhrases: List<String> = emptyList(),
    val includeTerms: List<String> = emptyList(),
    val excludeTerms: List<String> = emptyList()
) {
    val hasOperators: Boolean
        get() = includeTags.isNotEmpty() || excludeTags.isNotEmpty() ||
                includePaths.isNotEmpty() || excludePaths.isNotEmpty() ||
                includeFiles.isNotEmpty() || excludeFiles.isNotEmpty() ||
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
        val exactPhrases = mutableListOf<String>()
        val excludePhrases = mutableListOf<String>()
        val includeTerms = mutableListOf<String>()
        val excludeTerms = mutableListOf<String>()

        val matches = TOKEN_REGEX.findAll(trimmed)
        for (match in matches) {
            val token = match.value
            when {
                // Negative tag: -tag:foo or -tag:"foo bar"
                token.startsWith("-tag:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").removePrefix("#").trim()
                    if (rawVal.isNotEmpty()) excludeTags.add(rawVal.lowercase())
                }
                // Positive tag: tag:foo or tag:"foo bar"
                token.startsWith("tag:", ignoreCase = true) -> {
                    val rawVal = token.substring(4).removeSurrounding("\"").removePrefix("#").trim()
                    if (rawVal.isNotEmpty()) includeTags.add(rawVal.lowercase())
                }
                // Negative path: -path:foo
                token.startsWith("-path:", ignoreCase = true) -> {
                    val rawVal = token.substring(6).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludePaths.add(rawVal.lowercase())
                }
                // Positive path: path:foo
                token.startsWith("path:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includePaths.add(rawVal.lowercase())
                }
                // Negative file: -file:foo
                token.startsWith("-file:", ignoreCase = true) -> {
                    val rawVal = token.substring(6).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) excludeFiles.add(rawVal.lowercase())
                }
                // Positive file: file:foo
                token.startsWith("file:", ignoreCase = true) -> {
                    val rawVal = token.substring(5).removeSurrounding("\"").trim()
                    if (rawVal.isNotEmpty()) includeFiles.add(rawVal.lowercase())
                }
                // Negative exact phrase: -"phrase"
                token.startsWith("-\"") && token.endsWith("\"") && token.length > 3 -> {
                    val phrase = token.substring(2, token.length - 1).trim()
                    if (phrase.isNotEmpty()) excludePhrases.add(phrase)
                }
                // Positive exact phrase: "phrase"
                token.startsWith("\"") && token.endsWith("\"") && token.length > 2 -> {
                    val phrase = token.substring(1, token.length - 1).trim()
                    if (phrase.isNotEmpty()) exactPhrases.add(phrase)
                }
                // Negative term: -foo
                token.startsWith("-") && token.length > 1 -> {
                    val term = token.substring(1).trim()
                    if (term.isNotEmpty()) excludeTerms.add(term)
                }
                // Plain positive term
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
            exactPhrases = exactPhrases,
            excludePhrases = excludePhrases,
            includeTerms = includeTerms,
            excludeTerms = excludeTerms
        )
    }

    fun matches(note: NoteEntity, query: SearchQuery): Boolean {
        if (query.rawQuery.isEmpty()) return true

        val noteTags = note.tags
            .split(",")
            .map { it.trim().removePrefix("#").lowercase() }
            .filter { it.isNotEmpty() }

        val notePathLower = note.relativePath.lowercase()
        val noteFolderLower = note.folderPath.lowercase()
        val noteTitleLower = note.title.lowercase()
        val noteContentLower = note.content.lowercase()

        // 1. Exclude tags
        for (exTag in query.excludeTags) {
            if (noteTags.contains(exTag)) return false
        }

        // 2. Exclude paths
        for (exPath in query.excludePaths) {
            if (notePathLower.contains(exPath) || noteFolderLower.contains(exPath)) return false
        }

        // 3. Exclude files
        for (exFile in query.excludeFiles) {
            if (noteTitleLower.contains(exFile) || notePathLower.substringAfterLast("/").contains(exFile)) return false
        }

        // 4. Exclude exact phrases
        for (exPhrase in query.excludePhrases) {
            val exPhraseLower = exPhrase.lowercase()
            if (noteTitleLower.contains(exPhraseLower) || noteContentLower.contains(exPhraseLower)) return false
        }

        // 5. Exclude terms
        for (exTerm in query.excludeTerms) {
            val exTermLower = exTerm.lowercase()
            if (noteTitleLower.contains(exTermLower) || noteContentLower.contains(exTermLower) || noteTags.contains(exTermLower)) return false
        }

        // 6. Include tags
        for (inTag in query.includeTags) {
            if (!noteTags.contains(inTag)) return false
        }

        // 7. Include paths
        for (inPath in query.includePaths) {
            if (!notePathLower.contains(inPath) && !noteFolderLower.contains(inPath)) return false
        }

        // 8. Include files
        for (inFile in query.includeFiles) {
            if (!noteTitleLower.contains(inFile) && !notePathLower.substringAfterLast("/").contains(inFile)) return false
        }

        // 9. Exact phrases
        for (phrase in query.exactPhrases) {
            val phraseLower = phrase.lowercase()
            if (!noteTitleLower.contains(phraseLower) && !noteContentLower.contains(phraseLower)) return false
        }

        // 10. Include terms
        for (term in query.includeTerms) {
            val termLower = term.lowercase()
            val inTitle = noteTitleLower.contains(termLower)
            val inContent = noteContentLower.contains(termLower)
            val inTags = noteTags.contains(termLower)
            if (!inTitle && !inContent && !inTags) return false
        }

        return true
    }
}
