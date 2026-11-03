package com.nool.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntaxHighlighterTest {

    @Test
    fun testHighlightKeywordsAndStrings() {
        val code = "val name: String = \"Nool\"\n// comment\nval count = 42"
        val tokens = SyntaxHighlighter.highlight(code, "kotlin")

        assertTrue(tokens.any { it.text == "val" && it.type == SyntaxTokenType.KEYWORD })
        assertTrue(tokens.any { it.text == "\"Nool\"" && it.type == SyntaxTokenType.STRING })
        assertTrue(tokens.any { it.text.contains("comment") && it.type == SyntaxTokenType.COMMENT })
        assertTrue(tokens.any { it.text == "42" && it.type == SyntaxTokenType.NUMBER })
        assertTrue(tokens.any { it.text == "String" && it.type == SyntaxTokenType.TYPE_OR_CLASS })
    }

    @Test
    fun testHighlightSqlKeywords() {
        val code = "SELECT * FROM notes WHERE id = 1;"
        val tokens = SyntaxHighlighter.highlight(code, "sql")

        assertTrue(tokens.any { it.text == "SELECT" && it.type == SyntaxTokenType.KEYWORD })
        assertTrue(tokens.any { it.text == "FROM" && it.type == SyntaxTokenType.KEYWORD })
        assertTrue(tokens.any { it.text == "WHERE" && it.type == SyntaxTokenType.KEYWORD })
        assertTrue(tokens.any { it.text == "1" && it.type == SyntaxTokenType.NUMBER })
    }
}
