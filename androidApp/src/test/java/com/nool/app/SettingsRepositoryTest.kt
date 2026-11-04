package com.nool.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nool.app.settings.CalendarDefaultView
import com.nool.app.settings.NoteLayoutMode
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Calendar

class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun createRepository(): SettingsRepository {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + Job()),
            produceFile = { File(tmpFolder.newFolder(), "test_settings.preferences_pb") }
        )
        return SettingsRepository(customDataStore = testDataStore)
    }

    @Test
    fun testDefaultSettings() = runBlocking {
        val repo = createRepository()

        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        assertTrue(repo.dynamicColor.first())
        assertFalse(repo.pureBlack.first())
        assertEquals(16, repo.fontSize.first())
        assertEquals(NoteLayoutMode.LIST, repo.noteLayoutMode.first())
        assertEquals("Templates", repo.templatesFolder.first())
        assertEquals("Daily Notes", repo.dailyNotesFolder.first())
        assertTrue(repo.autoSave.first())
        assertFalse(repo.biometricLock.first())
        assertEquals(CalendarDefaultView.MONTH, repo.calendarDefaultView.first())
        assertEquals(Calendar.MONDAY, repo.calendarFirstDayOfWeek.first())
        assertEquals(15, repo.calendarDefaultReminderMinutes.first())
        assertEquals("Personal", repo.calendarDefaultCalendar.first())
    }

    @Test
    fun testUpdateAppearanceSettings() = runBlocking {
        val repo = createRepository()

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.themeMode.first())

        repo.setPureBlack(true)
        assertTrue(repo.pureBlack.first())

        repo.setDynamicColor(false)
        assertFalse(repo.dynamicColor.first())

        repo.setFontSize(20)
        assertEquals(20, repo.fontSize.first())

        repo.setNoteLayoutMode(NoteLayoutMode.GRID)
        assertEquals(NoteLayoutMode.GRID, repo.noteLayoutMode.first())
    }

    @Test
    fun testUpdateCalendarSettings() = runBlocking {
        val repo = createRepository()

        repo.setCalendarDefaultView(CalendarDefaultView.WEEK)
        assertEquals(CalendarDefaultView.WEEK, repo.calendarDefaultView.first())

        repo.setCalendarFirstDayOfWeek(Calendar.SUNDAY)
        assertEquals(Calendar.SUNDAY, repo.calendarFirstDayOfWeek.first())

        repo.setCalendarDefaultReminderMinutes(30)
        assertEquals(30, repo.calendarDefaultReminderMinutes.first())

        repo.setCalendarDefaultCalendar("Work")
        assertEquals("Work", repo.calendarDefaultCalendar.first())
    }

    @Test
    fun testSavedSearchesPersistence() = runBlocking {
        val repo = createRepository()

        val s1 = repo.saveSearch("Active Tasks", "tag:todo")
        val list1 = repo.savedSearches.first()
        assertEquals(1, list1.size)
        assertEquals("Active Tasks", list1[0].name)
        assertEquals("tag:todo", list1[0].query)

        val s2 = repo.saveSearch("Architecture Notes", "folder:Docs")
        val list2 = repo.savedSearches.first()
        assertEquals(2, list2.size)

        repo.deleteSavedSearch(s1.id)
        val list3 = repo.savedSearches.first()
        assertEquals(1, list3.size)
        assertEquals("Architecture Notes", list3[0].name)
    }
}
