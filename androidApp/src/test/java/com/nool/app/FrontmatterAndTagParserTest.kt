package com.nool.app

import com.nool.core.markdown.FrontmatterAndTagParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontmatterAndTagParserTest {

    @Test
    fun parseFrontmatterWithFlagsAndTags() {
        val markdown = """
            ---
            type: Task
            pinned: true
            favorite: true
            archived: false
            tags: [android, kotlin, mobile]
            ---
            # Task Overview
            Here is the body of the note.
        """.trimIndent()

        val parsed = FrontmatterAndTagParser.parse(markdown)

        assertEquals("Task", parsed.noteType)
        assertTrue(parsed.isPinned)
        assertTrue(parsed.isFavorite)
        assertFalse(parsed.isArchived)
        assertTrue(parsed.tags.contains("android"))
        assertTrue(parsed.tags.contains("kotlin"))
        assertTrue(parsed.tags.contains("mobile"))
        assertEquals("# Task Overview\nHere is the body of the note.", parsed.bodyText.trim())
    }

    @Test
    fun parseInlineTagsAndIgnoreHeadings() {
        val markdown = """
            # Project Notes
            ## Subheading
            ### Deep Heading

            Working on #nool with #jetpack-compose and #architecture/mvi.
            Also checking #offline-first features.
        """.trimIndent()

        val parsed = FrontmatterAndTagParser.parse(markdown)

        assertEquals("Note", parsed.noteType)
        assertFalse(parsed.isPinned)
        assertFalse(parsed.isFavorite)
        assertFalse(parsed.isArchived)

        // Headings must NOT be parsed as tags
        assertFalse(parsed.tags.contains("project"))
        assertFalse(parsed.tags.contains("subheading"))
        assertFalse(parsed.tags.contains("deep"))

        // Inline tags MUST be parsed
        assertTrue(parsed.tags.contains("nool"))
        assertTrue(parsed.tags.contains("jetpack-compose"))
        assertTrue(parsed.tags.contains("architecture/mvi"))
        assertTrue(parsed.tags.contains("offline-first"))
    }

    @Test
    fun mergeFrontmatterAndInlineTagsDeduplicated() {
        val markdown = """
            ---
            tags: [idea, research]
            ---
            Exploring a new #idea for #android development.
        """.trimIndent()

        val parsed = FrontmatterAndTagParser.parse(markdown)

        assertEquals(setOf("idea", "research", "android"), parsed.tags)
    }

    @Test
    fun updateFrontmatterFlag_whenNoFrontmatter_prependsYaml() {
        val original = "# Plain Note\nJust regular content."
        val updated = FrontmatterAndTagParser.updateFrontmatterFlag(original, "pinned", true)

        assertTrue(updated.startsWith("---\npinned: true\n---"))
        assertTrue(updated.contains("# Plain Note\nJust regular content."))

        val reparsed = FrontmatterAndTagParser.parse(updated)
        assertTrue(reparsed.isPinned)
        assertFalse(reparsed.isFavorite)
    }

    @Test
    fun updateFrontmatterFlag_whenFrontmatterExists_updatesExistingKey() {
        val original = """
            ---
            type: Book
            pinned: false
            favorite: false
            ---
            Reading notes...
        """.trimIndent()

        val updated = FrontmatterAndTagParser.updateFrontmatterFlag(original, "pinned", true)
        val reparsed = FrontmatterAndTagParser.parse(updated)

        assertTrue(reparsed.isPinned)
        assertFalse(reparsed.isFavorite)
        assertEquals("Book", reparsed.noteType)
    }

    @Test
    fun updateFrontmatterFlag_whenKeyMissingInFrontmatter_appendsToFrontmatter() {
        val original = """
            ---
            type: Meeting
            ---
            Discussion notes.
        """.trimIndent()

        val updated = FrontmatterAndTagParser.updateFrontmatterFlag(original, "favorite", true)
        val reparsed = FrontmatterAndTagParser.parse(updated)

        assertTrue(reparsed.isFavorite)
        assertFalse(reparsed.isPinned)
        assertEquals("Meeting", reparsed.noteType)
    }

    @Test
    fun gracefulHandlingOfMalformedOrEmptyFrontmatter() {
        val unclosed = """
            ---
            type: Draft
            no closing delimiter
            body content
        """.trimIndent()

        val parsedUnclosed = FrontmatterAndTagParser.parse(unclosed)
        assertEquals("Note", parsedUnclosed.noteType)

        val emptyParsed = FrontmatterAndTagParser.parse("")
        assertEquals("Note", emptyParsed.noteType)
        assertFalse(emptyParsed.isPinned)
        assertTrue(emptyParsed.tags.isEmpty())
    }
}
