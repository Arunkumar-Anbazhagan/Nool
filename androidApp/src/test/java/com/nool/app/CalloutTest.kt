package com.nool.app

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.nool.app.ui.editor.MarkdownFormattingHelper
import com.nool.app.ui.editor.callout.CalloutHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalloutTest {

    @Test
    fun testParseSimpleNoteCallout() {
        val info = CalloutHelper.parseCalloutHeader("[!note]")
        assertNotNull(info)
        assertEquals("note", info!!.type)
        assertEquals("Note", info.title)
        assertFalse(info.isCollapsible)
        assertFalse(info.isInitiallyCollapsed)
    }

    @Test
    fun testParseWarningWithCustomTitle() {
        val info = CalloutHelper.parseCalloutHeader("[!warning] Be careful here!")
        assertNotNull(info)
        assertEquals("warning", info!!.type)
        assertEquals("Be careful here!", info.title)
        assertFalse(info.isCollapsible)
    }

    @Test
    fun testParseCollapsibleExpandedCallout() {
        val info = CalloutHelper.parseCalloutHeader("[!tip]+ Pro Tip")
        assertNotNull(info)
        assertEquals("tip", info!!.type)
        assertEquals("Pro Tip", info.title)
        assertTrue(info.isCollapsible)
        assertFalse(info.isInitiallyCollapsed)
    }

    @Test
    fun testParseCollapsibleCollapsedCallout() {
        val info = CalloutHelper.parseCalloutHeader("[!danger]- Sensitive Information")
        assertNotNull(info)
        assertEquals("danger", info!!.type)
        assertEquals("Sensitive Information", info.title)
        assertTrue(info.isCollapsible)
        assertTrue(info.isInitiallyCollapsed)
    }

    @Test
    fun testParseRegularBlockquoteReturnsNull() {
        val info = CalloutHelper.parseCalloutHeader("This is just an ordinary quote.")
        assertNull(info)
    }

    @Test
    fun testInsertCalloutFormatting() {
        val current = TextFieldValue("Some important text", TextRange(0, 19))
        val result = MarkdownFormattingHelper.insertCallout(current, type = "tip", collapsible = "+", title = "Helpful Hint")
        assertTrue(result.text.contains("> [!tip]+ Helpful Hint"))
        assertTrue(result.text.contains("> Some important text"))
    }
}
