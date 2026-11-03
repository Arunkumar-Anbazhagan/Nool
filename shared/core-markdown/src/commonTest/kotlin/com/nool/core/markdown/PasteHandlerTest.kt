package com.nool.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasteHandlerTest {

    @Test
    fun testDetectHtml() {
        assertTrue(PasteHandler.isHtml("<p>Hello world</p>"))
        assertTrue(PasteHandler.isHtml("Here is a <a href=\"https://example.com\">link</a>"))
        assertTrue(!PasteHandler.isHtml("Just normal markdown text with [link](https://example.com)"))
    }

    @Test
    fun testConvertHtmlToMarkdown() {
        val html = """
            <h1>Title</h1>
            <p>This is <b>bold</b> and <i>italic</i> with <a href="https://nool.app">a link</a>.</p>
            <ul>
                <li>First item</li>
                <li>Second item</li>
            </ul>
            <pre><code>val x = 10</code></pre>
        """.trimIndent()

        val md = PasteHandler.htmlToMarkdown(html)
        assertTrue(md.contains("# Title"))
        assertTrue(md.contains("**bold**"))
        assertTrue(md.contains("*italic*"))
        assertTrue(md.contains("[a link](https://nool.app)"))
        assertTrue(md.contains("- First item"))
        assertTrue(md.contains("- Second item"))
        assertTrue(md.contains("```\nval x = 10\n```"))
    }
}
