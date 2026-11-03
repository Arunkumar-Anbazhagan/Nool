package com.nool.core.markdown

data class ParsedWikiLink(
    val rawText: String,
    val isEmbed: Boolean,
    val target: String,
    val heading: String?,
    val alias: String?,
    val startIndex: Int,
    val endIndex: Int
)

object WikiLinkParser {
    // Matches ![[...]] or [[...]]
    // Group 1: optional '!'
    // Group 2: inside link text
    private val WIKI_LINK_REGEX = Regex("""(!?)\[\[([^\]\n]+)\]\]""")

    fun parseLinks(content: String): List<ParsedWikiLink> {
        val matches = WIKI_LINK_REGEX.findAll(content)
        return matches.map { match ->
            val isEmbed = match.groupValues[1] == "!"
            val rawInside = match.groupValues[2].trim()

            // Handle alias: [[target|alias]]
            val targetAndHeading: String
            val alias: String?
            if (rawInside.contains("|")) {
                targetAndHeading = rawInside.substringBefore("|").trim()
                alias = rawInside.substringAfter("|").trim().ifEmpty { null }
            } else {
                targetAndHeading = rawInside
                alias = null
            }

            // Handle heading: [[target#heading]]
            val target: String
            val heading: String?
            if (targetAndHeading.contains("#")) {
                target = targetAndHeading.substringBefore("#").trim()
                heading = targetAndHeading.substringAfter("#").trim().ifEmpty { null }
            } else {
                target = targetAndHeading
                heading = null
            }

            ParsedWikiLink(
                rawText = match.value,
                isEmbed = isEmbed,
                target = target,
                heading = heading,
                alias = alias,
                startIndex = match.range.first,
                endIndex = match.range.last + 1
            )
        }.toList()
    }
}
