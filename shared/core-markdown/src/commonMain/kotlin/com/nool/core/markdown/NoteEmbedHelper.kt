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

    /**
     * Extracts a block referenced by [blockId] (matching Obsidian syntax `^blockId`).
     * Returns the block content with the `^blockId` marker stripped, or null if not found.
     */
    fun extractBlock(fullText: String, blockId: String): String? {
        val cleanId = blockId.trim().removePrefix("^").trim()
        if (cleanId.isEmpty()) return null

        val lines = fullText.lines()
        val targetMarker = "^$cleanId"

        val foundIdx = lines.indexOfFirst { line ->
            val trimmed = line.trim()
            trimmed == targetMarker || trimmed.endsWith(" $targetMarker") || trimmed.endsWith("\t$targetMarker")
        }

        if (foundIdx == -1) return null

        val targetLine = lines[foundIdx].trim()

        // Case 1: Target line contains only the block ID marker (e.g. paragraph above ends, next line is ^blockId)
        if (targetLine == targetMarker) {
            val blockLines = mutableListOf<String>()
            var i = foundIdx - 1
            while (i >= 0 && lines[i].isNotBlank() && !lines[i].trim().startsWith("#")) {
                blockLines.add(0, lines[i])
                i--
            }
            return if (blockLines.isNotEmpty()) blockLines.joinToString("\n").trim() else null
        }

        // Case 2: Target line contains content and ends with marker
        val isListItem = Regex("""^\s*([-*+]|\d+\.)\s+""").containsMatchIn(lines[foundIdx])
        val startIdx: Int
        val endIdx: Int

        if (isListItem) {
            startIdx = foundIdx
            // Scan forwards for indented continuation lines belonging to this list item
            var next = foundIdx + 1
            while (next < lines.size && lines[next].isNotBlank() && (lines[next].startsWith(" ") || lines[next].startsWith("\t")) && !Regex("""^\s*([-*+]|\d+\.)\s+""").containsMatchIn(lines[next])) {
                next++
            }
            endIdx = next - 1
        } else {
            // Find block start (scan backwards until blank line, heading, or list item)
            var s = foundIdx
            while (s > 0 && lines[s - 1].isNotBlank() && !lines[s - 1].trim().startsWith("#") && !Regex("""^\s*([-*+]|\d+\.)\s+""").containsMatchIn(lines[s - 1])) {
                s--
            }
            startIdx = s

            // Find block end (scan forwards until blank line, heading, or list item)
            var e = foundIdx
            while (e < lines.size - 1 && lines[e + 1].isNotBlank() && !lines[e + 1].trim().startsWith("#") && !Regex("""^\s*([-*+]|\d+\.)\s+""").containsMatchIn(lines[e + 1])) {
                e++
            }
            endIdx = e
        }

        val resultLines = mutableListOf<String>()
        for (i in startIdx..endIdx) {
            val line = lines[i]
            if (i == foundIdx) {
                // Strip the marker from the target line
                val stripped = line.replace(Regex("""\s*\Q$targetMarker\E\s*$"""), "")
                if (stripped.isNotBlank()) {
                    resultLines.add(stripped)
                }
            } else {
                resultLines.add(line)
            }
        }

        return if (resultLines.isNotEmpty()) resultLines.joinToString("\n").trim() else null
    }
}
