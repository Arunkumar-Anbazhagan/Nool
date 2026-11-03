package com.nool.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlashCommandTest {

    @Test
    fun testCatalogContainsEssentialCommands() {
        val commands = SlashCommandCatalog.commands
        assertTrue(commands.any { it.id == "table" })
        assertTrue(commands.any { it.id == "callout_note" })
        assertTrue(commands.any { it.id == "heading1" })
        assertTrue(commands.any { it.id == "embed" })
        assertTrue(commands.any { it.id == "math" })
        assertTrue(commands.any { it.id == "mermaid" })
    }

    @Test
    fun testFilteringByQuery() {
        val tableMatches = SlashCommandCatalog.filter("tab")
        assertEquals(1, tableMatches.size)
        assertEquals("table", tableMatches[0].id)

        val calloutMatches = SlashCommandCatalog.filter("callout")
        assertTrue(calloutMatches.size >= 3)

        val mathMatches = SlashCommandCatalog.filter("math")
        assertEquals(1, mathMatches.size)
        assertEquals("math", mathMatches[0].id)
    }
}
