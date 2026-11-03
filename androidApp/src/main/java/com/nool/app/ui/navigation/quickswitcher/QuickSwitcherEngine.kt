package com.nool.app.ui.navigation.quickswitcher

import com.nool.app.data.index.NoteEntity

enum class QuickSwitcherMatchField {
    TITLE,
    ALIAS,
    PATH,
    RECENT
}

data class QuickSwitcherResult(
    val note: NoteEntity,
    val matchedField: QuickSwitcherMatchField,
    val matchSnippet: String? = null,
    val score: Int
)

object QuickSwitcherEngine {

    fun filter(notes: List<NoteEntity>, query: String, limit: Int = 30): List<QuickSwitcherResult> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return notes
                .sortedByDescending { it.lastModified }
                .take(limit)
                .map {
                    QuickSwitcherResult(
                        note = it,
                        matchedField = QuickSwitcherMatchField.RECENT,
                        matchSnippet = null,
                        score = 0
                    )
                }
        }

        val q = trimmedQuery.lowercase()
        val results = mutableListOf<QuickSwitcherResult>()

        for (note in notes) {
            val titleLower = note.title.lowercase()
            val pathLower = note.relativePath.lowercase()
            val aliases = note.aliases
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val titleScore = computeMatchScore(titleLower, q)
            val pathScore = computeMatchScore(pathLower, q)
            var bestAlias: String? = null
            var bestAliasScore = 0

            for (alias in aliases) {
                val aScore = computeMatchScore(alias.lowercase(), q)
                if (aScore > bestAliasScore) {
                    bestAliasScore = aScore
                    bestAlias = alias
                }
            }

            // Title gets highest priority, then alias, then path
            val weightedTitleScore = if (titleScore > 0) titleScore + 2000 else 0
            val weightedAliasScore = if (bestAliasScore > 0) bestAliasScore + 1000 else 0
            val weightedPathScore = pathScore

            val maxScore = maxOf(weightedTitleScore, weightedAliasScore, weightedPathScore)
            if (maxScore > 0) {
                val field: QuickSwitcherMatchField
                val snippet: String?

                if (weightedTitleScore >= weightedAliasScore && weightedTitleScore >= weightedPathScore) {
                    field = QuickSwitcherMatchField.TITLE
                    snippet = null
                } else if (weightedAliasScore >= weightedPathScore) {
                    field = QuickSwitcherMatchField.ALIAS
                    snippet = bestAlias
                } else {
                    field = QuickSwitcherMatchField.PATH
                    snippet = note.relativePath
                }

                results.add(
                    QuickSwitcherResult(
                        note = note,
                        matchedField = field,
                        matchSnippet = snippet,
                        score = maxScore
                    )
                )
            }
        }

        return results
            .sortedWith(
                compareByDescending<QuickSwitcherResult> { it.score }
                    .thenByDescending { it.note.lastModified }
            )
            .take(limit)
    }

    fun computeMatchScore(target: String, query: String): Int {
        if (target == query) return 10000
        if (target.startsWith(query)) return 5000 + (100 - minOf(target.length - query.length, 90))
        if (target.contains(" $query") || target.contains("/$query") || target.contains("-$query") || target.contains("_$query")) {
            return 3500
        }
        val subIndex = target.indexOf(query)
        if (subIndex >= 0) {
            return 2000 - minOf(subIndex, 500)
        }

        // Fuzzy subsequence match
        var qIdx = 0
        var score = 0
        var consecutive = 0
        var prevTargetIdx = -2

        for (tIdx in target.indices) {
            if (qIdx < query.length && target[tIdx] == query[qIdx]) {
                qIdx++
                var charScore = 15
                if (tIdx == prevTargetIdx + 1) {
                    consecutive++
                    charScore += consecutive * 20
                } else {
                    consecutive = 0
                }
                if (tIdx == 0 || target[tIdx - 1] in " /_.-") {
                    charScore += 40
                }
                score += charScore
                prevTargetIdx = tIdx
            }
        }

        return if (qIdx == query.length) score else 0
    }
}
