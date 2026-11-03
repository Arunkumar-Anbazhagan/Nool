package com.nool.core.index

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nool.core.index.db.NoolDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoolDatabaseTest {

    private fun createInMemoryDatabase(): NoolDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoolDatabase.Schema.create(driver)
        return NoolDatabase(driver)
    }

    @Test
    fun testInsertNoteAndFtsTriggers() {
        val db = createInMemoryDatabase()
        val queries = db.noolIndexQueries

        queries.upsertNote(
            relativePath = "Notes/Test.md",
            title = "Test",
            folderPath = "Notes",
            content = "This is a quick test of the knowledge base indexer",
            excerpt = "This is a quick test...",
            lastModified = 1000L,
            createdTimestamp = 1000L,
            size = 50L,
            isPinned = false,
            isFavorite = true,
            isArchived = false,
            tags = "knowledge,testing",
            aliases = "MyTest",
            noteType = "Note"
        )

        val note = queries.getNote("Notes/Test.md").executeAsOneOrNull()
        kotlin.test.assertNotNull(note)
        assertEquals("Test", note.title)
        assertEquals(true, note.isFavorite)

        // Test FTS search via MATCH on note_fts
        val searchResults = queries.searchFts("knowledge*").executeAsList()
        assertEquals(1, searchResults.size)
        assertEquals("Notes/Test.md", searchResults[0].relativePath)

        // Test prefix matching with 2-3 prefix index
        val prefixResults = queries.searchFts("qui*").executeAsList()
        assertEquals(1, prefixResults.size)
        assertEquals("Notes/Test.md", prefixResults[0].relativePath)
    }

    @Test
    fun testUpdateNoteTriggerUpdatesFts() {
        val db = createInMemoryDatabase()
        val queries = db.noolIndexQueries

        queries.upsertNote(
            relativePath = "Notes/Article.md",
            title = "Article",
            folderPath = "Notes",
            content = "Original content about cryptography and privacy",
            excerpt = "Original content...",
            lastModified = 1000L,
            createdTimestamp = 1000L,
            size = 40L,
            isPinned = false,
            isFavorite = false,
            isArchived = false,
            tags = "crypto",
            aliases = "",
            noteType = "Note"
        )

        // Matches 'cryptography'
        assertEquals(1, queries.searchFts("cryptography*").executeAsList().size)

        // Update content to mention quantum computing instead
        queries.upsertNote(
            relativePath = "Notes/Article.md",
            title = "Article",
            folderPath = "Notes",
            content = "Updated content about quantum algorithms",
            excerpt = "Updated content...",
            lastModified = 2000L,
            createdTimestamp = 1000L,
            size = 42L,
            isPinned = false,
            isFavorite = false,
            isArchived = false,
            tags = "quantum",
            aliases = "",
            noteType = "Note"
        )

        // Old term should no longer match
        assertEquals(0, queries.searchFts("cryptography*").executeAsList().size)

        // New term should match
        assertEquals(1, queries.searchFts("quantum*").executeAsList().size)
    }

    @Test
    fun testDeleteNoteTriggerRemovesFromFts() {
        val db = createInMemoryDatabase()
        val queries = db.noolIndexQueries

        queries.upsertNote(
            relativePath = "Notes/Deletable.md",
            title = "Deletable",
            folderPath = "Notes",
            content = "Temporary scratchpad notes",
            excerpt = "Temporary scratchpad...",
            lastModified = 1000L,
            createdTimestamp = 1000L,
            size = 20L,
            isPinned = false,
            isFavorite = false,
            isArchived = false,
            tags = "temp",
            aliases = "",
            noteType = "Note"
        )

        assertEquals(1, queries.searchFts("scratchpad*").executeAsList().size)

        queries.deleteNote("Notes/Deletable.md")

        assertEquals(0, queries.searchFts("scratchpad*").executeAsList().size)
        assertEquals(null, queries.getNote("Notes/Deletable.md").executeAsOneOrNull())
    }

    @Test
    fun testLinksAndBacklinksQueries() {
        val db = createInMemoryDatabase()
        val queries = db.noolIndexQueries

        queries.insertLink(
            sourcePath = "Notes/Source.md",
            rawTarget = "Target#Heading",
            targetTitle = "Target",
            targetHeading = "Heading",
            alias = null,
            isEmbed = false,
            resolvedPath = "Notes/Target.md"
        )

        val backlinks = queries.getBacklinksForNote(
            resolvedPath = "Notes/Target.md",
            targetTitle = "target"
        ).executeAsList()

        assertEquals(1, backlinks.size)
        assertEquals("Notes/Source.md", backlinks[0].sourcePath)

        val referencing = queries.getReferencingSourcePaths(
            targetTitle = "target",
            resolvedPath = "Notes/Target.md"
        ).executeAsList()

        assertEquals(listOf("Notes/Source.md"), referencing)
    }
}
