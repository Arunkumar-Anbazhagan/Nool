package com.nool.app

import com.nool.app.ui.editor.table.TableColumnAlignment
import com.nool.app.ui.editor.table.buildMarkdownTable
import com.nool.core.markdown.PasteHandler
import com.nool.core.markdown.SlashCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTableAndSlashCommandTest {

    @Test
    fun testBuildMarkdownTableGeneratesGfmAlignment() {
        val headers = listOf("Name", "Qty", "Price")
        val alignments = listOf(TableColumnAlignment.LEFT, TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT)
        val rows = listOf(
            listOf("Apples", "10", "$5.00"),
            listOf("Oranges", "5", "$3.50")
        )

        val table = buildMarkdownTable(headers, alignments, rows)
        val lines = table.trim().lines()

        assertEquals(4, lines.size)
        assertTrue(lines[0].contains("| Name | Qty | Price |"))
        assertTrue(lines[1].contains("| :--- | :---: | ---: |"))
        assertTrue(lines[2].contains("| Apples | 10 | $5.00 |"))
        assertTrue(lines[3].contains("| Oranges | 5 | $3.50 |"))
    }

    @Test
    fun testSlashCommandCatalogFilters() {
        val tableCmd = SlashCommandCatalog.filter("table")
        assertEquals(1, tableCmd.size)
        assertEquals("table", tableCmd[0].id)

        val calloutCmds = SlashCommandCatalog.filter("callout")
        assertTrue(calloutCmds.size >= 3)
    }

    @Test
    fun testHtmlPasteConversion() {
        val html = "<h2>Notes</h2><p>Here is <b>important</b> text.</p>"
        assertTrue(PasteHandler.isHtml(html))
        val md = PasteHandler.htmlToMarkdown(html)
        assertTrue(md.contains("## Notes"))
        assertTrue(md.contains("**important**"))
    }

    @Test
    fun testWordCountAndReadingTime() {
        val text = "One two three four five six seven eight nine ten."
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val readingTimeMin = maxOf(1, Math.ceil(words / 200.0).toInt())

        assertEquals(10, words)
        assertEquals(1, readingTimeMin)
    }
}
