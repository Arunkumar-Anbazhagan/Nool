package com.nool.core.markdown

data class ParsedCalloutHeader(
    val type: String,
    val title: String,
    val isCollapsible: Boolean,
    val isInitiallyCollapsed: Boolean
)

object CalloutParser {
    private val CALLOUT_REGEX = Regex("""^\[!([a-zA-Z0-9_-]+)\]([+-]?)(.*)$""")

    fun parseCalloutHeader(firstLine: String): ParsedCalloutHeader? {
        val trimmed = firstLine.trim()
        val match = CALLOUT_REGEX.find(trimmed) ?: return null

        val type = match.groupValues[1].lowercase()
        val collapseSymbol = match.groupValues[2]
        val rawTitle = match.groupValues[3].trim()

        val isCollapsible = collapseSymbol.isNotEmpty()
        val isInitiallyCollapsed = collapseSymbol == "-"

        val defaultTitle = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val title = if (rawTitle.isNotEmpty()) rawTitle else defaultTitle

        return ParsedCalloutHeader(
            type = type,
            title = title,
            isCollapsible = isCollapsible,
            isInitiallyCollapsed = isInitiallyCollapsed
        )
    }
}
