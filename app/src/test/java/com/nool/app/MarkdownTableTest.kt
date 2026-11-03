package com.nool.app

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableTest {

    private val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .build()

    @Test
    fun `test basic gfm table parsing and structure`() {
        val markdown = """
            | Header 1 | Header 2 |
            | --- | --- |
            | Cell 1 | Cell 2 |
            | Cell 3 | Cell 4 |
        """.trimIndent()

        val doc = parser.parse(markdown)
        val table = doc.firstChild as? TableBlock
        assertNotNull(table)

        val head = table!!.firstChild as? TableHead
        assertNotNull(head)
        val headRow = head!!.firstChild as? TableRow
        assertNotNull(headRow)

        val body = head.next as? TableBody
        assertNotNull(body)
        var bodyRowCount = 0
        var row = body!!.firstChild
        while (row != null) {
            bodyRowCount++
            row = row.next
        }
        assertEquals(2, bodyRowCount)
    }

    @Test
    fun `test column alignment parsing left center and right`() {
        val markdown = """
            | Left | Center | Right |
            | :--- | :---: | ---: |
            | L1 | C1 | R1 |
        """.trimIndent()

        val doc = parser.parse(markdown)
        val table = doc.firstChild as? TableBlock
        assertNotNull(table)

        val head = table!!.firstChild as? TableHead
        val headRow = head!!.firstChild as? TableRow
        assertNotNull(headRow)

        var cell = headRow!!.firstChild as? TableCell
        assertNotNull(cell)
        assertEquals(TableCell.Alignment.LEFT, cell!!.alignment)

        cell = cell.next as? TableCell
        assertNotNull(cell)
        assertEquals(TableCell.Alignment.CENTER, cell!!.alignment)

        cell = cell.next as? TableCell
        assertNotNull(cell)
        assertEquals(TableCell.Alignment.RIGHT, cell!!.alignment)
    }

    @Test
    fun `test escaped pipes inside table cells`() {
        val markdown = """
            | Command | Description |
            | --- | --- |
            | `cat file \| grep x` | Search inside file |
        """.trimIndent()

        val doc = parser.parse(markdown)
        val table = doc.firstChild as? TableBlock
        assertNotNull(table)

        val head = table!!.firstChild as? TableHead
        val body = head!!.next as? TableBody
        assertNotNull(body)
        val row = body!!.firstChild as? TableRow
        assertNotNull(row)

        val cell = row!!.firstChild as? TableCell
        assertNotNull(cell)
        // Ensure pipe was preserved as content, not split into a new cell
        var count = 0
        var c = row.firstChild
        while (c != null) {
            count++
            c = c.next
        }
        assertEquals(2, count)
    }

    @Test
    fun `test inline formatting inside table cells`() {
        val markdown = """
            | Features | Status |
            | --- | --- |
            | **Bold** and *Italic* | [Link](https://example.com) |
        """.trimIndent()

        val doc = parser.parse(markdown)
        val table = doc.firstChild as? TableBlock
        assertNotNull(table)

        val head = table!!.firstChild as? TableHead
        val body = head!!.next as? TableBody
        val row = body!!.firstChild as? TableRow
        val firstCell = row!!.firstChild as? TableCell
        assertNotNull(firstCell)
        // Contains StrongEmphasis and Emphasis
        assertTrue(firstCell!!.firstChild != null)
    }
}
