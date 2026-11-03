package com.nool.app

import com.nool.app.data.git.FrontmatterMerger
import com.nool.app.data.git.GitStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitTest {

    @Test
    fun testGitStatusCleanCalculation() {
        val cleanStatus = GitStatus()
        assertTrue(cleanStatus.isClean)
        assertEquals(0, cleanStatus.totalChanges)

        val dirtyStatus = GitStatus(
            modified = listOf("note1.md"),
            untracked = listOf("note2.md")
        )
        assertFalse(dirtyStatus.isClean)
        assertEquals(2, dirtyStatus.totalChanges)
    }

    @Test
    fun testFrontmatterParse() {
        val content = """
            ---
            title: My Note
            tags: #project #mobile
            author: Alice
            ---
            This is the body of the note.
        """.trimIndent()

        val (props, body) = FrontmatterMerger.parseFrontmatterAndBody(content)
        assertEquals("My Note", props["title"])
        assertEquals("#project #mobile", props["tags"])
        assertEquals("Alice", props["author"])
        assertEquals("This is the body of the note.", body.trim())
    }

    @Test
    fun testFrontmatterCleanMergeNonConflicting() {
        val base = """
            ---
            title: Original Title
            status: draft
            ---
            Body content here.
        """.trimIndent()

        // Local changed status to in-progress
        val local = """
            ---
            title: Original Title
            status: in-progress
            ---
            Body content here.
        """.trimIndent()

        // Remote added tags
        val remote = """
            ---
            title: Original Title
            status: draft
            tags: #work
            ---
            Body content here.
        """.trimIndent()

        val result = FrontmatterMerger.merge(base, local, remote)
        assertTrue(result.isClean)
        assertFalse(result.bodyConflict)
        assertTrue(result.mergedText.contains("status: in-progress"))
        assertTrue(result.mergedText.contains("tags: #work"))
        assertTrue(result.mergedText.contains("Body content here."))
    }

    @Test
    fun testFrontmatterConflictWhenBothModifySamePropertyDifferently() {
        val base = """
            ---
            status: draft
            ---
            Body.
        """.trimIndent()

        val local = """
            ---
            status: review
            ---
            Body.
        """.trimIndent()

        val remote = """
            ---
            status: completed
            ---
            Body.
        """.trimIndent()

        val result = FrontmatterMerger.merge(base, local, remote)
        assertFalse(result.isClean)
        assertTrue(result.conflictingKeys.contains("status"))
    }

    @Test
    fun testBodyConflictDetection() {
        val base = """
            ---
            title: Note
            ---
            Original text.
        """.trimIndent()

        val local = """
            ---
            title: Note
            ---
            Alice edited this line.
        """.trimIndent()

        val remote = """
            ---
            title: Note
            ---
            Bob edited this line differently.
        """.trimIndent()

        val result = FrontmatterMerger.merge(base, local, remote)
        assertFalse(result.isClean)
        assertTrue(result.bodyConflict)
    }

    @Test
    fun testUrlRedaction() {
        val rawUrl = "https://pat_token_secret@github.com/org/repo.git"
        val redacted = rawUrl.replace(Regex("://[^@]+@"), "://***@")
        assertEquals("https://***@github.com/org/repo.git", redacted)
    }
}
