package com.nool.core.markdown

enum class SyntaxTokenType {
    KEYWORD,
    STRING,
    NUMBER,
    COMMENT,
    TYPE_OR_CLASS,
    FUNCTION,
    PLAIN
}

data class HighlightedToken(
    val text: String,
    val type: SyntaxTokenType
)

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        // Kotlin & Java
        "val", "var", "fun", "class", "interface", "object", "enum", "sealed", "data",
        "public", "private", "protected", "internal", "override", "import", "package",
        "if", "else", "when", "for", "while", "return", "try", "catch", "finally",
        "throw", "suspend", "is", "in", "as", "null", "true", "false", "this", "super",
        "static", "void", "final", "const", "new", "abstract",
        // JavaScript / TypeScript / Python / Go / Rust
        "let", "function", "def", "async", "await", "yield", "from", "export", "default",
        "struct", "impl", "trait", "fn", "mut", "pub", "type", "select", "case", "switch",
        // SQL
        "select", "from", "where", "insert", "into", "values", "update", "delete", "join",
        "left", "right", "inner", "outer", "group", "by", "order", "having", "limit"
    )

    fun highlight(code: String, language: String? = null): List<HighlightedToken> {
        val tokens = mutableListOf<HighlightedToken>()
        val lines = code.lines()

        for ((lineIdx, line) in lines.withIndex()) {
            var index = 0
            while (index < line.length) {
                // 1. Single-line comments // or # or --
                if (line.startsWith("//", index) || line.startsWith("#", index) || line.startsWith("--", index)) {
                    tokens.add(HighlightedToken(line.substring(index), SyntaxTokenType.COMMENT))
                    index = line.length
                    break
                }

                // 2. String literals "..." or '...'
                val char = line[index]
                if (char == '"' || char == '\'') {
                    val quote = char
                    val start = index
                    index++
                    while (index < line.length && line[index] != quote) {
                        if (line[index] == '\\' && index + 1 < line.length) {
                            index += 2
                        } else {
                            index++
                        }
                    }
                    if (index < line.length && line[index] == quote) {
                        index++
                    }
                    tokens.add(HighlightedToken(line.substring(start, index), SyntaxTokenType.STRING))
                    continue
                }

                // 3. Numbers
                if (char.isDigit()) {
                    val start = index
                    while (index < line.length && (line[index].isDigit() || line[index] == '.' || line[index] == 'L' || line[index] == 'f')) {
                        index++
                    }
                    tokens.add(HighlightedToken(line.substring(start, index), SyntaxTokenType.NUMBER))
                    continue
                }

                // 4. Identifiers / Keywords
                if (char.isLetter() || char == '_') {
                    val start = index
                    while (index < line.length && (line[index].isLetterOrDigit() || line[index] == '_')) {
                        index++
                    }
                    val word = line.substring(start, index)
                    val type = when {
                        KEYWORDS.contains(word.lowercase()) -> SyntaxTokenType.KEYWORD
                        word.first().isUpperCase() -> SyntaxTokenType.TYPE_OR_CLASS
                        index < line.length && line[index] == '(' -> SyntaxTokenType.FUNCTION
                        else -> SyntaxTokenType.PLAIN
                    }
                    tokens.add(HighlightedToken(word, type))
                    continue
                }

                // 5. Punctuation / Whitespace
                val start = index
                while (index < line.length && !line[index].isLetterOrDigit() && line[index] != '_' && line[index] != '"' && line[index] != '\'' && !line.startsWith("//", index) && !line.startsWith("#", index) && !line.startsWith("--", index)) {
                    index++
                }
                tokens.add(HighlightedToken(line.substring(start, index), SyntaxTokenType.PLAIN))
            }

            if (lineIdx < lines.size - 1) {
                tokens.add(HighlightedToken("\n", SyntaxTokenType.PLAIN))
            }
        }

        return tokens
    }
}
