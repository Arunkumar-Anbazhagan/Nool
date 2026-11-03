package com.nool.app

import com.nool.core.model.IndexedNote
import com.nool.core.index.SearchQueryParser
import com.nool.app.settings.SavedSearch
import com.nool.app.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOperatorsTest {

    private fun createTestNote(
        path: String,
        title: String,
        content: String = "",
        tags: String = "",
        folderPath: String = ""
    ) = IndexedNote(
        id = 0,
        relativePath = path,
        title = title,
        folderPath = folderPath,
        content = content,
        excerpt = content.take(50),
        lastModified = 1000L,
        createdTimestamp = 1000L,
        size = 100L,
        tags = tags.split(",").filter { it.isNotBlank() }
    )

    @Test
    fun `tag operator matches positive and negative tags`() {
        val note1 = createTestNote("n1.md", "Android Tips", "Learn Kotlin", tags = "android,mobile")
        val note2 = createTestNote("n2.md", "Web Tips", "Learn TypeScript", tags = "web,frontend")
        val note3 = createTestNote("n3.md", "Draft Note", "WIP", tags = "android,draft")

        val queryPositive = SearchQueryParser.parse("tag:android")
        assertTrue(SearchQueryParser.matches(note1, queryPositive))
        assertFalse(SearchQueryParser.matches(note2, queryPositive))
        assertTrue(SearchQueryParser.matches(note3, queryPositive))

        val queryNegative = SearchQueryParser.parse("tag:android -tag:draft")
        assertTrue(SearchQueryParser.matches(note1, queryNegative))
        assertFalse(SearchQueryParser.matches(note3, queryNegative))
    }

    @Test
    fun `path operator matches directory prefixes`() {
        val note1 = createTestNote("work/projects/alpha.md", "Alpha", folderPath = "work/projects")
        val note2 = createTestNote("personal/journal.md", "Journal", folderPath = "personal")

        val query = SearchQueryParser.parse("path:work/projects")
        assertTrue(SearchQueryParser.matches(note1, query))
        assertFalse(SearchQueryParser.matches(note2, query))

        val queryNeg = SearchQueryParser.parse("-path:work")
        assertFalse(SearchQueryParser.matches(note1, queryNeg))
        assertTrue(SearchQueryParser.matches(note2, queryNeg))
    }

    @Test
    fun `file operator matches note titles and filenames`() {
        val note1 = createTestNote("docs/ArchitectureGuide.md", "ArchitectureGuide")
        val note2 = createTestNote("docs/QuickStart.md", "QuickStart")

        val query = SearchQueryParser.parse("file:architecture")
        assertTrue(SearchQueryParser.matches(note1, query))
        assertFalse(SearchQueryParser.matches(note2, query))
    }

    @Test
    fun `exclude term operator filters out matching notes`() {
        val note1 = createTestNote("notes/meeting.md", "Meeting", content = "Discuss quarterly goals")
        val note2 = createTestNote("notes/secret.md", "Confidential", content = "Internal only draft")

        val query = SearchQueryParser.parse("-draft")
        assertTrue(SearchQueryParser.matches(note1, query))
        assertFalse(SearchQueryParser.matches(note2, query))
    }

    @Test
    fun `exact phrase operator matches word sequences in quotes`() {
        val note1 = createTestNote("n1.md", "AI", content = "We love machine learning and deep models")
        val note2 = createTestNote("n2.md", "Tools", content = "The machine was learning slowly")

        val query = SearchQueryParser.parse("\"machine learning\"")
        assertTrue(SearchQueryParser.matches(note1, query))
        assertFalse(SearchQueryParser.matches(note2, query))
    }

    @Test
    fun `combined operators evaluation`() {
        val matchingNote = createTestNote(
            path = "study/android/compose_guide.md",
            title = "Compose Guide",
            content = "Introduction to Jetpack Compose declarative UI",
            tags = "android,compose",
            folderPath = "study/android"
        )
        val nonMatchingNote = createTestNote(
            path = "study/android/compose_legacy.md",
            title = "Old Guide",
            content = "Jetpack Compose with deprecated views",
            tags = "android",
            folderPath = "study/android"
        )

        val query = SearchQueryParser.parse("tag:android path:study \"Jetpack Compose\" -deprecated file:Guide")
        assertTrue(SearchQueryParser.matches(matchingNote, query))
        assertFalse(SearchQueryParser.matches(nonMatchingNote, query))
    }

    @Test
    fun `folder and title operators match specifically`() {
        val note1 = createTestNote("work/projects/alpha.md", "Project Alpha", folderPath = "work/projects")
        val note2 = createTestNote("personal/journal.md", "Daily Log", folderPath = "personal")

        val query1 = SearchQueryParser.parse("folder:work/projects title:Alpha")
        assertTrue(SearchQueryParser.matches(note1, query1))
        assertFalse(SearchQueryParser.matches(note2, query1))

        val query2 = SearchQueryParser.parse("-folder:personal")
        assertTrue(SearchQueryParser.matches(note1, query2))
        assertFalse(SearchQueryParser.matches(note2, query2))
    }

    @Test
    fun `hierarchical tag query matches sub-tags`() {
        val note = createTestNote("n1.md", "Task", tags = "project/android/ui")
        val queryParent = SearchQueryParser.parse("tag:project")
        val queryMid = SearchQueryParser.parse("tag:project/android")
        val queryExact = SearchQueryParser.parse("tag:project/android/ui")
        val queryOther = SearchQueryParser.parse("tag:project/ios")

        assertTrue(SearchQueryParser.matches(note, queryParent))
        assertTrue(SearchQueryParser.matches(note, queryMid))
        assertTrue(SearchQueryParser.matches(note, queryExact))
        assertFalse(SearchQueryParser.matches(note, queryOther))
    }

    @Test
    fun `toFtsMatchQuery generates valid FTS5 expressions`() {
        val query = SearchQueryParser.parse("tag:android title:Guide \"hello world\" kotlin")
        val fts = SearchQueryParser.toFtsMatchQuery(query)
        assertNotNull(fts)
        assertTrue(fts!!.contains("\"hello world\""))
        assertTrue(fts.contains("kotlin*"))
        assertTrue(fts.contains("tags: android*"))
        assertTrue(fts.contains("title: guide*"))
    }

    @Test
    fun `saved search model equality and fields`() {
        val saved = SavedSearch(id = "123", name = "Android Projects", query = "tag:android path:projects")
        assertEquals("123", saved.id)
        assertEquals("Android Projects", saved.name)
        assertEquals("tag:android path:projects", saved.query)
    }
}
