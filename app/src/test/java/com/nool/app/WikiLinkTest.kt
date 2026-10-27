package com.nool.app

import com.nool.app.data.index.FrontmatterAndTagParser
import com.nool.app.data.index.LinkResolver
import com.nool.app.data.index.NoteTitleAndPath
import com.nool.app.data.index.WikiLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WikiLinkTest {

    @Test
    fun testParseSimpleWikiLink() {
        val text = "Check out [[Product Vision]] for details."
        val links = WikiLinkParser.parseLinks(text)

        assertEquals(1, links.size)
        val link = links[0]
        assertEquals("[[Product Vision]]", link.rawText)
        assertEquals("Product Vision", link.target)
        assertNull(link.alias)
        assertNull(link.heading)
        assertFalse(link.isEmbed)
    }

    @Test
    fun testParseWikiLinkWithAlias() {
        val text = "Read the [[Documentation|User Guide]] before starting."
        val links = WikiLinkParser.parseLinks(text)

        assertEquals(1, links.size)
        val link = links[0]
        assertEquals("[[Documentation|User Guide]]", link.rawText)
        assertEquals("Documentation", link.target)
        assertEquals("User Guide", link.alias)
        assertNull(link.heading)
        assertFalse(link.isEmbed)
    }

    @Test
    fun testParseWikiLinkWithHeading() {
        val text = "See [[Architecture#Storage Layer]] for details."
        val links = WikiLinkParser.parseLinks(text)

        assertEquals(1, links.size)
        val link = links[0]
        assertEquals("[[Architecture#Storage Layer]]", link.rawText)
        assertEquals("Architecture", link.target)
        assertNull(link.alias)
        assertEquals("Storage Layer", link.heading)
        assertFalse(link.isEmbed)
    }

    @Test
    fun testParseWikiLinkWithHeadingAndAlias() {
        val text = "See [[Architecture#Storage Layer|Storage]] for details."
        val links = WikiLinkParser.parseLinks(text)

        assertEquals(1, links.size)
        val link = links[0]
        assertEquals("[[Architecture#Storage Layer|Storage]]", link.rawText)
        assertEquals("Architecture", link.target)
        assertEquals("Storage Layer", link.heading)
        assertEquals("Storage", link.alias)
        assertFalse(link.isEmbed)
    }

    @Test
    fun testParseEmbedLink() {
        val text = "Here is the diagram:\n![[attachments/arch_diagram.png]]\nAnd note embed:\n![[Summary]]"
        val links = WikiLinkParser.parseLinks(text)

        assertEquals(2, links.size)

        val imageEmbed = links[0]
        assertTrue(imageEmbed.isEmbed)
        assertEquals("attachments/arch_diagram.png", imageEmbed.target)

        val noteEmbed = links[1]
        assertTrue(noteEmbed.isEmbed)
        assertEquals("Summary", noteEmbed.target)
    }

    @Test
    fun testParseMultipleMixedLinks() {
        val text = """
            Welcome to [[Project Overview]].
            Key team member: [[Alice Smith|Alice]].
            Check out the roadmap: [[Roadmap#Q4 Goals|Q4]].
            ![[attachments/logo.png]]
        """.trimIndent()

        val links = WikiLinkParser.parseLinks(text)
        assertEquals(4, links.size)
        assertEquals("Project Overview", links[0].target)
        assertEquals("Alice", links[1].alias)
        assertEquals("Q4 Goals", links[2].heading)
        assertTrue(links[3].isEmbed)
    }

    @Test
    fun testFrontmatterAliasesParsingInlineArray() {
        val content = """
            ---
            type: Person
            aliases: [Bob, Robert, Bobby]
            tags: #team/engineering
            ---
            # Robert Smith
        """.trimIndent()

        val parsed = FrontmatterAndTagParser.parse(content)
        assertEquals(3, parsed.aliases.size)
        assertTrue(parsed.aliases.contains("Bob"))
        assertTrue(parsed.aliases.contains("Robert"))
        assertTrue(parsed.aliases.contains("Bobby"))
    }

    @Test
    fun testFrontmatterAliasesParsingListSyntax() {
        val content = """
            ---
            title: Artificial Intelligence
            aliases:
              - AI
              - Machine Learning
            ---
            Content here.
        """.trimIndent()

        val parsed = FrontmatterAndTagParser.parse(content)
        assertEquals(2, parsed.aliases.size)
        assertTrue(parsed.aliases.contains("AI"))
        assertTrue(parsed.aliases.contains("Machine Learning"))
    }

    @Test
    fun testLinkResolverDirectAndCaseInsensitive() {
        val catalog = listOf(
            NoteTitleAndPath(title = "Kotlin", relativePath = "Tech/Kotlin.md"),
            NoteTitleAndPath(title = "Jetpack Compose", relativePath = "Tech/Jetpack Compose.md"),
            NoteTitleAndPath(title = "Roadmap", relativePath = "Roadmap.md")
        )

        // Exact match
        val resolvedExact = LinkResolver.resolveLink("Kotlin", "Notes/Draft.md", catalog)
        assertEquals("Tech/Kotlin.md", resolvedExact)

        // Case-insensitive match
        val resolvedCaseInsensitive = LinkResolver.resolveLink("kotlin", "Notes/Draft.md", catalog)
        assertEquals("Tech/Kotlin.md", resolvedCaseInsensitive)

        // Missing note
        val resolvedMissing = LinkResolver.resolveLink("Flutter", "Notes/Draft.md", catalog)
        assertNull(resolvedMissing)
    }

    @Test
    fun testLinkResolverSiblingPreference() {
        val catalog = listOf(
            NoteTitleAndPath(title = "Tasks", relativePath = "Archive/Tasks.md"),
            NoteTitleAndPath(title = "Tasks", relativePath = "Projects/Tasks.md"),
            NoteTitleAndPath(title = "Tasks", relativePath = "Tasks.md")
        )

        // A note in "Projects/Nool.md" linking to "Tasks" should resolve to sibling "Projects/Tasks.md"
        val resolved = LinkResolver.resolveLink("Tasks", "Projects/Nool.md", catalog)
        assertEquals("Projects/Tasks.md", resolved)

        // A note in the root folder linking to "Tasks" should resolve to root "Tasks.md"
        val resolvedRoot = LinkResolver.resolveLink("Tasks", "Notes.md", catalog)
        assertEquals("Tasks.md", resolvedRoot)
    }

    @Test
    fun testLinkResolverAliasMatching() {
        val catalog = listOf(
            NoteTitleAndPath(
                title = "Artificial Intelligence",
                relativePath = "AI.md",
                aliases = "ai, machine learning"
            )
        )

        // Match via alias
        val resolved = LinkResolver.resolveLink("machine learning", "Notes.md", catalog)
        assertEquals("AI.md", resolved)
    }

    @Test
    fun testRenameUpdatesLinksRegex() {
        val oldTitle = "Old Note"
        val newTitle = "New Note"
        val oldEscaped = Regex.escape(oldTitle)

        // Regex matching [[Old Note]] with optional heading/alias or embed
        val linkRegex = Regex("(!?\\[\\[)$oldEscaped((?:#[^\\]|]+)?(?:\\|[^\\]]+)?\\]\\])")

        val sourceText = """
            Here is a link to [[Old Note]].
            And another to [[Old Note|alias]].
            With a section: [[Old Note#Heading|Section]].
            And embedded: ![[Old Note]].
            Unrelated: [[Old Note Extended]] should remain unchanged.
        """.trimIndent()

        val updated = linkRegex.replace(sourceText) { matchResult ->
            "${matchResult.groupValues[1]}$newTitle${matchResult.groupValues[2]}"
        }

        assertTrue(updated.contains("[[New Note]]"))
        assertTrue(updated.contains("[[New Note|alias]]"))
        assertTrue(updated.contains("[[New Note#Heading|Section]]"))
        assertTrue(updated.contains("![[New Note]]"))
        assertTrue(updated.contains("[[Old Note Extended]]"))
    }
}
