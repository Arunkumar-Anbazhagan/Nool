package com.nool.core.markdown

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
}
