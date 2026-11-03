package com.nool.core.markdown

object PasteHandler {

    /**
     * Checks if text contains common HTML markup.
     */
    fun isHtml(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("<") && trimmed.contains(">")) ||
               Regex("""<(p|div|span|h[1-6]|ul|ol|li|table|tr|td|th|a|b|i|strong|em|code|pre)\b""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
    }

    /**
     * Converts common HTML elements into clean, deterministic Markdown.
     */
    fun htmlToMarkdown(html: String): String {
        var result = html

        // Headings
        for (level in 1..6) {
            val hashes = "#".repeat(level)
            result = result.replace(Regex("""<h$level\b[^>]*>(.*?)</h$level>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
                "\n$hashes ${cleanInline(match.groupValues[1])}\n"
            }
        }

        // Bold & Italic
        result = result.replace(Regex("""<(strong|b)\b[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "**${cleanInline(it.groupValues[2])}**"
        }
        result = result.replace(Regex("""<(em|i)\b[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "*${cleanInline(it.groupValues[2])}*"
        }
        result = result.replace(Regex("""<(s|strike|del)\b[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "~~${cleanInline(it.groupValues[2])}~~"
        }

        // Code blocks & inline code
        result = result.replace(Regex("""<pre><code\b[^>]*>(.*?)</code></pre>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "\n```\n${it.groupValues[1].trim()}\n```\n"
        }
        result = result.replace(Regex("""<code\b[^>]*>(.*?)</code>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "`${it.groupValues[1]}`"
        }

        // Links
        result = result.replace(Regex("""<a\b[^>]*href="([^"]*)"[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "[${cleanInline(it.groupValues[2])}](${it.groupValues[1]})"
        }

        // Unordered & ordered lists
        result = result.replace(Regex("""<li\b[^>]*>(.*?)</li>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "\n- ${cleanInline(it.groupValues[1])}"
        }

        // Line breaks & paragraphs
        result = result.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        result = result.replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n\n")
        result = result.replace(Regex("""<p\b[^>]*>""", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("""<blockquote\b[^>]*>(.*?)</blockquote>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
            "\n> ${cleanInline(it.groupValues[1])}\n"
        }

        // Strip remaining HTML tags
        result = result.replace(Regex("""<[^>]+>"""), "")

        // Normalize HTML entities
        result = result
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        // Clean up excessive blank lines
        result = result.replace(Regex("""\n{3,}"""), "\n\n")

        return result.trim()
    }

    private fun cleanInline(htmlSnippet: String): String {
        return htmlSnippet.replace(Regex("""<[^>]+>"""), "").trim()
    }
}
