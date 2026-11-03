package com.nool.core.markdown

data class ParsedMetadata(
    val frontmatter: Map<String, Any>,
    val noteType: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val tags: Set<String>,
    val aliases: List<String>,
    val bodyText: String
)

object FrontmatterAndTagParser {

    private val INLINE_TAG_REGEX = Regex("""(?<!\S)#([a-zA-Z0-9_\-]+(?:/[a-zA-Z0-9_\-]+)*)(?=[.,!?;:\)\]]*(?:\s|$))""")

    fun parse(rawContent: String): ParsedMetadata {
        val lines = rawContent.lines()
        val frontmatterMap = mutableMapOf<String, Any>()
        val frontmatterTags = mutableSetOf<String>()
        val frontmatterAliases = mutableListOf<String>()
        var bodyStartLine = 0
        var currentListKey = ""

        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            var closingIndex = -1
            for (i in 1 until lines.size) {
                if (lines[i].trim() == "---") {
                    closingIndex = i
                    break
                }
            }

            if (closingIndex != -1) {
                bodyStartLine = closingIndex + 1
                for (i in 1 until closingIndex) {
                    val line = lines[i].trim()
                    if (line.isEmpty() || line.startsWith("#")) continue

                    if (line.contains(":")) {
                        val key = line.substringBefore(":").trim().lowercase()
                        val valueStr = line.substringAfter(":").trim()
                        currentListKey = key

                        when (key) {
                            "type" -> frontmatterMap["type"] = valueStr.removeSurrounding("\"").removeSurrounding("'")
                            "pinned" -> frontmatterMap["pinned"] = valueStr.equals("true", ignoreCase = true)
                            "favorite" -> frontmatterMap["favorite"] = valueStr.equals("true", ignoreCase = true)
                            "archived" -> frontmatterMap["archived"] = valueStr.equals("true", ignoreCase = true)
                            "aliases", "alias" -> {
                                if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                                    val arrayItems = valueStr.removePrefix("[").removeSuffix("]")
                                        .split(",")
                                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                        .filter { it.isNotBlank() }
                                    frontmatterAliases.addAll(arrayItems)
                                } else if (valueStr.isNotBlank()) {
                                    val items = valueStr.split(",")
                                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                        .filter { it.isNotBlank() }
                                    frontmatterAliases.addAll(items)
                                }
                            }
                            "tags", "tag" -> {
                                if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                                    val arrayTags = valueStr.removePrefix("[").removeSuffix("]")
                                        .split(",")
                                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'").removePrefix("#") }
                                        .filter { it.isNotBlank() }
                                    frontmatterTags.addAll(arrayTags)
                                } else if (valueStr.isNotBlank()) {
                                    val tags = valueStr.split(",")
                                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'").removePrefix("#") }
                                        .filter { it.isNotBlank() }
                                    frontmatterTags.addAll(tags)
                                }
                            }
                            else -> {
                                frontmatterMap[key] = valueStr.removeSurrounding("\"").removeSurrounding("'")
                            }
                        }
                    } else if (line.startsWith("-") && currentListKey.isNotEmpty()) {
                        val itemValue = line.removePrefix("-").trim().removeSurrounding("\"").removeSurrounding("'")
                        if (itemValue.isNotBlank()) {
                            when (currentListKey) {
                                "tags", "tag" -> frontmatterTags.add(itemValue.removePrefix("#"))
                                "aliases", "alias" -> frontmatterAliases.add(itemValue)
                            }
                        }
                    }
                }
            }
        }

        val bodyLines = if (bodyStartLine < lines.size) lines.subList(bodyStartLine, lines.size) else emptyList()
        val bodyText = bodyLines.joinToString("\n")

        val inlineTags = extractInlineTags(bodyText)
        val allTags = frontmatterTags + inlineTags

        val noteType = (frontmatterMap["type"] as? String)?.ifBlank { "Note" } ?: "Note"
        val isPinned = frontmatterMap["pinned"] as? Boolean ?: false
        val isFavorite = frontmatterMap["favorite"] as? Boolean ?: false
        val isArchived = frontmatterMap["archived"] as? Boolean ?: false

        return ParsedMetadata(
            frontmatter = frontmatterMap,
            noteType = noteType,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isArchived = isArchived,
            tags = allTags,
            aliases = frontmatterAliases,
            bodyText = bodyText
        )
    }

    fun extractInlineTags(text: String): Set<String> {
        val lines = text.lines()
        val tags = mutableSetOf<String>()
        var inFencedCodeBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                inFencedCodeBlock = !inFencedCodeBlock
                continue
            }
            if (inFencedCodeBlock) continue

            val lineWithoutInlineCode = trimmed.replace(Regex("""`[^`]*`"""), " ")

            val matches = INLINE_TAG_REGEX.findAll(lineWithoutInlineCode)
            for (match in matches) {
                val tag = match.groupValues[1]
                if (!tag.all { it.isDigit() }) {
                    tags.add(tag)
                }
            }
        }
        return tags
    }

    fun updateFrontmatterFlag(content: String, key: String, value: Boolean): String {
        val lines = content.lines().toMutableList()
        val normalizedKey = key.lowercase()

        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            var closingIndex = -1
            for (i in 1 until lines.size) {
                if (lines[i].trim() == "---") {
                    closingIndex = i
                    break
                }
            }

            if (closingIndex != -1) {
                var found = false
                for (i in 1 until closingIndex) {
                    val line = lines[i].trim()
                    if (line.startsWith("$normalizedKey:", ignoreCase = true)) {
                        lines[i] = "$normalizedKey: $value"
                        found = true
                        break
                    }
                }
                if (!found) {
                    lines.add(closingIndex, "$normalizedKey: $value")
                }
                return lines.joinToString("\n")
            }
        }

        // No frontmatter found, prepend frontmatter
        return "---\n$normalizedKey: $value\n---\n\n$content"
    }
}
