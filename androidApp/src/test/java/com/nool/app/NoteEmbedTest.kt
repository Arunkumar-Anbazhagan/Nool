package com.nool.app

import com.nool.app.ui.editor.embed.NoteEmbedHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NoteEmbedTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `extractHeadingSection extracts target section until next heading of same level`() {
        val markdown = """
            # Document Title
            Introduction content here.
            
            ## Section One
            First section content.
            Line 2 of first section.
            
            ## Section Two
            Second section content.
        """.trimIndent()

        val extracted = NoteEmbedHelper.extractHeadingSection(markdown, "Section One")
        val lines = extracted.lines()
        assertEquals(3, lines.size)
        assertEquals("## Section One", lines[0])
        assertEquals("First section content.", lines[1])
        assertEquals("Line 2 of first section.", lines[2])
    }

    @Test
    fun `extractHeadingSection includes lower level subheadings`() {
        val markdown = """
            # Main Heading
            
            ## Chapter 1
            Chapter 1 text.
            
            ### Subsection 1.1
            Details of 1.1.
            
            #### Deep 1.1.1
            Deep detail.
            
            ## Chapter 2
            Chapter 2 text.
        """.trimIndent()

        val extracted = NoteEmbedHelper.extractHeadingSection(markdown, "Chapter 1")
        assertTrue(extracted.contains("## Chapter 1"))
        assertTrue(extracted.contains("### Subsection 1.1"))
        assertTrue(extracted.contains("#### Deep 1.1.1"))
        assertTrue(!extracted.contains("## Chapter 2"))
    }

    @Test
    fun `extractHeadingSection falls back to full content when heading not found`() {
        val markdown = "# Note\nSome content"
        val extracted = NoteEmbedHelper.extractHeadingSection(markdown, "Missing Heading")
        assertEquals(markdown, extracted)
    }

    @Test
    fun `resolveNoteFile resolves files with or without extension and nested paths`() {
        val root = tempFolder.newFolder("vault")
        val note1 = File(root, "SimpleNote.md").apply { writeText("Hello") }
        val subDir = File(root, "SubFolder").apply { mkdir() }
        val note2 = File(subDir, "DeepNote.md").apply { writeText("Deep") }

        // By title without extension
        val resolved1 = NoteEmbedHelper.resolveNoteFile(root, "SimpleNote")
        assertNotNull(resolved1)
        assertEquals(note1.absolutePath, resolved1?.absolutePath)

        // By relative path with extension
        val resolved2 = NoteEmbedHelper.resolveNoteFile(root, "SubFolder/DeepNote.md")
        assertNotNull(resolved2)
        assertEquals(note2.absolutePath, resolved2?.absolutePath)

        // By filename alone when in subdirectory
        val resolved3 = NoteEmbedHelper.resolveNoteFile(root, "DeepNote")
        assertNotNull(resolved3)
        assertEquals(note2.absolutePath, resolved3?.absolutePath)

        // Non-existent note returns null
        val resolvedMissing = NoteEmbedHelper.resolveNoteFile(root, "GhostNote")
        assertNull(resolvedMissing)
    }

    @Test
    fun `max recursion depth is capped at 3`() {
        assertEquals(3, NoteEmbedHelper.MAX_RECURSION_DEPTH)
    }

    @Test
    fun `extractBlock finds block by id and strips marker`() {
        val markdown = """
            # Header
            
            This is paragraph one.
            
            This is an important definition. ^def-123
            
            - List item 1
            - List item 2 ^task-item
        """.trimIndent()

        val defBlock = NoteEmbedHelper.extractBlock(markdown, "^def-123")
        assertNotNull(defBlock)
        assertEquals("This is an important definition.", defBlock)

        val taskBlock = NoteEmbedHelper.extractBlock(markdown, "task-item")
        assertNotNull(taskBlock)
        assertEquals("- List item 2", taskBlock)

        val notFound = NoteEmbedHelper.extractBlock(markdown, "nonexistent")
        assertNull(notFound)
    }

    @Test
    fun `extractBlock handles standalone block marker on separate line`() {
        val markdown = """
            First line of block.
            Second line of block.
            ^my-block
            
            Next paragraph.
        """.trimIndent()

        val block = NoteEmbedHelper.extractBlock(markdown, "my-block")
        assertNotNull(block)
        assertEquals("First line of block.\nSecond line of block.", block)
    }

    @Test
    fun `resolveNoteFile resolves folder notes`() {
        val root = tempFolder.newFolder("vault2")
        val projDir = File(root, "Projects").apply { mkdir() }
        val projNote = File(projDir, "Projects.md").apply { writeText("# Projects Index") }

        val resolved = NoteEmbedHelper.resolveNoteFile(root, "Projects")
        assertNotNull(resolved)
        assertEquals(projNote.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `WikiLinkParser parses block references`() {
        val text = "Check [[MyNote#^block-42]] and ![[Other#^sec-1|Custom Alias]]"
        val links = com.nool.core.markdown.WikiLinkParser.parseLinks(text)
        assertEquals(2, links.size)

        assertEquals("MyNote", links[0].target)
        assertTrue(links[0].isBlockRef)
        assertEquals("block-42", links[0].blockId)
        assertNull(links[0].heading)

        assertEquals("Other", links[1].target)
        assertTrue(links[1].isBlockRef)
        assertEquals("sec-1", links[1].blockId)
        assertEquals("Custom Alias", links[1].alias)
        assertTrue(links[1].isEmbed)
    }
}
