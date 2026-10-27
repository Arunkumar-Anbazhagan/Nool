package com.nool.app

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.nool.app.ui.editor.MarkdownFormattingHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormattingHelperTest {

    @Test
    fun `test toggle bold wraps and unwraps selection`() {
        val initial = TextFieldValue("Hello World", TextRange(6, 11))
        val bolded = MarkdownFormattingHelper.toggleBold(initial)
        assertEquals("Hello **World**", bolded.text)

        val unbolded = MarkdownFormattingHelper.toggleBold(bolded)
        assertEquals("Hello World", unbolded.text)
    }

    @Test
    fun `test toggle italic wraps selection`() {
        val initial = TextFieldValue("Text", TextRange(0, 4))
        val italicized = MarkdownFormattingHelper.toggleItalic(initial)
        assertEquals("*Text*", italicized.text)
    }

    @Test
    fun `test toggle strikethrough wraps selection`() {
        val initial = TextFieldValue("Old", TextRange(0, 3))
        val struck = MarkdownFormattingHelper.toggleStrikethrough(initial)
        assertEquals("~~Old~~", struck.text)
    }

    @Test
    fun `test cycle heading transitions levels`() {
        val level0 = TextFieldValue("My Heading", TextRange(3, 3))
        val level1 = MarkdownFormattingHelper.cycleHeading(level0)
        assertEquals("# My Heading", level1.text)

        val level2 = MarkdownFormattingHelper.cycleHeading(level1)
        assertEquals("## My Heading", level2.text)

        val level3 = MarkdownFormattingHelper.cycleHeading(level2)
        assertEquals("### My Heading", level3.text)

        val unheaded = MarkdownFormattingHelper.cycleHeading(level3)
        assertEquals("My Heading", unheaded.text)
    }

    @Test
    fun `test toggle checklist prefixes line`() {
        val initial = TextFieldValue("Buy groceries", TextRange(0, 0))
        val checklist = MarkdownFormattingHelper.toggleChecklist(initial)
        assertEquals("- [ ] Buy groceries", checklist.text)

        val unchecked = MarkdownFormattingHelper.toggleChecklist(checklist)
        assertEquals("Buy groceries", unchecked.text)
    }

    @Test
    fun `test insert code block with selection`() {
        val initial = TextFieldValue("val x = 10", TextRange(0, 10))
        val formatted = MarkdownFormattingHelper.insertCodeBlock(initial)
        assertTrue(formatted.text.contains("```\nval x = 10\n```"))
    }

    @Test
    fun `test insert table`() {
        val initial = TextFieldValue("Notes:", TextRange(6, 6))
        val formatted = MarkdownFormattingHelper.insertTable(initial)
        assertTrue(formatted.text.contains("| Header 1 | Header 2 |"))
        assertTrue(formatted.text.contains("| --- | --- |"))
    }

    @Test
    fun `test insert attachment`() {
        val initial = TextFieldValue("", TextRange.Zero)
        val attached = MarkdownFormattingHelper.insertAttachment(initial, "chart", "attachments/chart.png")
        assertEquals("![chart](attachments/chart.png)\n", attached.text)
    }
}
