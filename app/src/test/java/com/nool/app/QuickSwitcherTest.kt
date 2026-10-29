package com.nool.app

import com.nool.app.data.index.NoteEntity
import com.nool.app.ui.navigation.quickswitcher.QuickSwitcherEngine
import com.nool.app.ui.navigation.quickswitcher.QuickSwitcherMatchField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSwitcherTest {

    private fun createTestNote(
        path: String,
        title: String,
        lastModified: Long,
        aliases: String = "",
        folderPath: String = ""
    ) = NoteEntity(
        relativePath = path,
        title = title,
        folderPath = folderPath,
        content = "content for $title",
        excerpt = "excerpt",
        lastModified = lastModified,
        createdTimestamp = 1000L,
        size = 100L,
        aliases = aliases
    )

    @Test
    fun `empty query returns recent notes ordered by lastModified`() {
        val notes = listOf(
            createTestNote("note1.md", "Older Note", 1000L),
            createTestNote("note2.md", "Newest Note", 3000L),
            createTestNote("note3.md", "Middle Note", 2000L)
        )

        val results = QuickSwitcherEngine.filter(notes, "")
        assertEquals(3, results.size)
        assertEquals("Newest Note", results[0].note.title)
        assertEquals(QuickSwitcherMatchField.RECENT, results[0].matchedField)
        assertEquals("Middle Note", results[1].note.title)
        assertEquals("Older Note", results[2].note.title)
    }

    @Test
    fun `exact title match ranks above prefix and contains match`() {
        val notes = listOf(
            createTestNote("notes/compose_tips.md", "Compose Tips", 1000L),
            createTestNote("notes/compose.md", "Compose", 2000L),
            createTestNote("notes/jetpack_compose.md", "Jetpack Compose", 3000L)
        )

        val results = QuickSwitcherEngine.filter(notes, "Compose")
        assertTrue(results.isNotEmpty())
        assertEquals("Compose", results[0].note.title)
        assertEquals(QuickSwitcherMatchField.TITLE, results[0].matchedField)
    }

    @Test
    fun `matches by alias and populates alias snippet`() {
        val notes = listOf(
            createTestNote("arch.md", "Architecture Decision", 1000L, aliases = "adr, design-doc"),
            createTestNote("other.md", "Other Note", 2000L)
        )

        val results = QuickSwitcherEngine.filter(notes, "adr")
        assertEquals(1, results.size)
        assertEquals("Architecture Decision", results[0].note.title)
        assertEquals(QuickSwitcherMatchField.ALIAS, results[0].matchedField)
        assertEquals("adr", results[0].matchSnippet)
    }

    @Test
    fun `matches by folder or path`() {
        val notes = listOf(
            createTestNote("work/projects/alpha.md", "Alpha", 1000L, folderPath = "work/projects"),
            createTestNote("personal/journal.md", "Journal", 2000L, folderPath = "personal")
        )

        val results = QuickSwitcherEngine.filter(notes, "projects")
        assertEquals(1, results.size)
        assertEquals("Alpha", results[0].note.title)
        assertEquals(QuickSwitcherMatchField.PATH, results[0].matchedField)
    }

    @Test
    fun `fuzzy subsequence matches across title`() {
        val notes = listOf(
            createTestNote("switcher.md", "QuickSwitcherEngine", 1000L),
            createTestNote("other.md", "Unrelated", 2000L)
        )

        val results = QuickSwitcherEngine.filter(notes, "qse")
        assertEquals(1, results.size)
        assertEquals("QuickSwitcherEngine", results[0].note.title)
    }

    @Test
    fun `non matching query returns empty list`() {
        val notes = listOf(
            createTestNote("note1.md", "Apple", 1000L),
            createTestNote("note2.md", "Banana", 2000L)
        )

        val results = QuickSwitcherEngine.filter(notes, "Zebra")
        assertTrue(results.isEmpty())
    }
}
