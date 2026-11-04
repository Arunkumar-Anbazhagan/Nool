package com.nool.core.index

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nool.core.index.db.NoolDatabase
import com.nool.core.model.SearchScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightIndexManagerTest {

    private fun createIndexManager(): SqlDelightIndexManager {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoolDatabase.Schema.create(driver)
        val db = NoolDatabase(driver)
        return SqlDelightIndexManager(db)
    }

    @Test
    fun testIndexNoteContentAndSearch() = runTest {
        val manager = createIndexManager()

        val content = """
            ---
            tags: [kotlin, multiplatform]
            pinned: true
            ---
            # Nool Architecture
            This is a test note about [[SharedCore]] and local-first architecture.
        """.trimIndent()

        manager.indexNoteContent(
            relativePath = "Docs/Architecture.md",
            content = content,
            lastModified = 5000L,
            size = content.length.toLong()
        )

        val note = manager.getNote("Docs/Architecture.md")
        assertNotNull(note)
        assertEquals("Architecture", note.title)
        assertEquals("Docs", note.folderPath)
        assertEquals(true, note.isPinned)
        assertTrue(note.tags.contains("kotlin"))

        // Search by FTS
        val results = manager.search("Architecture")
        assertEquals(1, results.size)
        assertEquals("Docs/Architecture.md", results[0].relativePath)

        // Search with operators
        val tagResults = manager.search("tag:kotlin")
        assertEquals(1, tagResults.size)

        val excludedResults = manager.search("-tag:kotlin")
        assertEquals(0, excludedResults.size)
    }

    @Test
    fun testBacklinksAndMentions() = runTest {
        val manager = createIndexManager()

        val noteA = """
            # Project Overview
            We need to consult [[Architecture]] before finalizing the design.
        """.trimIndent()

        val noteB = """
            # Architecture
            Core architectural principles and rules.
        """.trimIndent()

        manager.indexNoteContent("Docs/Architecture.md", noteB, 1000L, noteB.length.toLong())
        manager.indexNoteContent("Projects/Overview.md", noteA, 2000L, noteA.length.toLong())

        val backlinks = manager.getBacklinks("Docs/Architecture.md")
        assertEquals(1, backlinks.size)
        assertEquals("Projects/Overview.md", backlinks[0].sourcePath)
    }

    @Test
    fun testLinkResolverOrder() {
        val catalog = listOf(
            com.nool.core.model.NoteTitleAndPath("Alpha", "FolderA/Alpha.md", "AliasA"),
            com.nool.core.model.NoteTitleAndPath("Alpha", "FolderB/Alpha.md", ""),
            com.nool.core.model.NoteTitleAndPath("Beta", "Beta.md", "AliasBeta")
        )

        // Sibling in FolderA
        val resolvedSibling = LinkResolver.resolveLink("Alpha", "FolderA/Current.md", catalog)
        assertEquals("FolderA/Alpha.md", resolvedSibling)

        // Sibling in FolderB
        val resolvedSiblingB = LinkResolver.resolveLink("Alpha", "FolderB/Other.md", catalog)
        assertEquals("FolderB/Alpha.md", resolvedSiblingB)

        // Alias resolution
        val resolvedAlias = LinkResolver.resolveLink("AliasBeta", "Root.md", catalog)
        assertEquals("Beta.md", resolvedAlias)
    }

    @Test
    fun testCalendarEventsUpsertAndRangeQuery() = runTest {
        val manager = createIndexManager()

        val event1 = com.nool.core.model.CalendarEvent(
            id = "ev-1",
            title = "Doctor Appointment",
            startTime = 10_000L,
            endTime = 12_000L,
            calendarId = "Personal"
        )
        val event2 = com.nool.core.model.CalendarEvent(
            id = "ev-2",
            title = "Daily Sync",
            startTime = 10_000L,
            endTime = 11_000L,
            rrule = "FREQ=DAILY;COUNT=3",
            calendarId = "Work"
        )

        manager.upsertCalendarEvent(event1)
        manager.upsertCalendarEvent(event2)

        val retrieved = manager.getCalendarEvent("ev-1")
        assertNotNull(retrieved)
        assertEquals("Doctor Appointment", retrieved.title)
        assertEquals("Personal", retrieved.calendarId)

        // Query in range (includes event1 and 3 occurrences of event2)
        val instances = manager.getCalendarEventsInRange(0L, 300_000_000L)
        // 1 (event1) + 3 (event2 occurrences) = 4
        assertEquals(4, instances.size)

        // Delete ev-1
        manager.deleteCalendarEvent("ev-1")
        val deleted = manager.getCalendarEvent("ev-1")
        kotlin.test.assertNull(deleted)
    }
}
