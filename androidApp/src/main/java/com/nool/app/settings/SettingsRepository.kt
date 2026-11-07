package com.nool.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nool_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class NoteLayoutMode(val label: String) {
    LIST("List"),
    GRID("Grid")
}

enum class CalendarDefaultView(val label: String) {
    MONTH("Month"),
    WEEK("Week"),
    DAY("Day"),
    AGENDA("Agenda")
}

enum class SortOption(val label: String) {
    MODIFIED_DESC("Last modified (newest)"),
    MODIFIED_ASC("Last modified (oldest)"),
    CREATED_DESC("Created (newest)"),
    CREATED_ASC("Created (oldest)"),
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)")
}

data class SavedSearch(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val query: String
)

class SettingsRepository(
    private val context: Context? = null,
    customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore = customDataStore ?: context?.dataStore

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
        private val KEY_NOTE_LAYOUT_MODE = stringPreferencesKey("note_layout_mode")
        private val KEY_SORT_OPTION = stringPreferencesKey("sort_option")
        private val KEY_SHOW_ARCHIVED = booleanPreferencesKey("show_archived")
        private val KEY_PURE_BLACK = booleanPreferencesKey("pure_black")
        private val KEY_TEMPLATES_FOLDER = stringPreferencesKey("templates_folder")
        private val KEY_DAILY_NOTES_FOLDER = stringPreferencesKey("daily_notes_folder")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val KEY_SAVED_SEARCHES = stringPreferencesKey("saved_searches")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        private val KEY_CALENDAR_DEFAULT_VIEW = stringPreferencesKey("calendar_default_view")
        private val KEY_CALENDAR_FIRST_DAY = intPreferencesKey("calendar_first_day")
        private val KEY_CALENDAR_DEFAULT_REMINDER = intPreferencesKey("calendar_default_reminder")
        private val KEY_CALENDAR_DEFAULT_CALENDAR = stringPreferencesKey("calendar_default_calendar")
    }

    private val safePrefsFlow: Flow<Preferences> = dataStore?.data?.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    } ?: kotlinx.coroutines.flow.flowOf(emptyPreferences())

    val themeMode: Flow<ThemeMode> = safePrefsFlow.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try { ThemeMode.valueOf(raw) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    val dynamicColor: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    val fontSize: Flow<Int> = safePrefsFlow.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: 16
    }

    val noteLayoutMode: Flow<NoteLayoutMode> = safePrefsFlow.map { prefs ->
        val raw = prefs[KEY_NOTE_LAYOUT_MODE] ?: NoteLayoutMode.LIST.name
        try { NoteLayoutMode.valueOf(raw) } catch (e: Exception) { NoteLayoutMode.LIST }
    }

    val sortOption: Flow<SortOption> = safePrefsFlow.map { prefs ->
        val raw = prefs[KEY_SORT_OPTION] ?: SortOption.MODIFIED_DESC.name
        try { SortOption.valueOf(raw) } catch (e: Exception) { SortOption.MODIFIED_DESC }
    }

    val showArchived: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_SHOW_ARCHIVED] ?: false
    }

    val pureBlack: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_PURE_BLACK] ?: false
    }

    val templatesFolder: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_TEMPLATES_FOLDER] ?: "Templates"
    }

    val dailyNotesFolder: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_DAILY_NOTES_FOLDER] ?: "Daily Notes"
    }

    val autoSave: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_AUTO_SAVE] ?: true
    }

    val biometricLock: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK] ?: false
    }

    val calendarDefaultView: Flow<CalendarDefaultView> = safePrefsFlow.map { prefs ->
        val raw = prefs[KEY_CALENDAR_DEFAULT_VIEW] ?: CalendarDefaultView.MONTH.name
        try { CalendarDefaultView.valueOf(raw) } catch (e: Exception) { CalendarDefaultView.MONTH }
    }

    val calendarFirstDayOfWeek: Flow<Int> = safePrefsFlow.map { prefs ->
        prefs[KEY_CALENDAR_FIRST_DAY] ?: java.util.Calendar.MONDAY
    }

    val calendarDefaultReminderMinutes: Flow<Int> = safePrefsFlow.map { prefs ->
        prefs[KEY_CALENDAR_DEFAULT_REMINDER] ?: 15
    }

    val calendarDefaultCalendar: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_CALENDAR_DEFAULT_CALENDAR] ?: "Personal"
    }

    val savedSearches: Flow<List<SavedSearch>> = safePrefsFlow.map { prefs ->
        val raw = prefs[KEY_SAVED_SEARCHES] ?: ""
        deserializeSavedSearches(raw)
    }

    suspend fun saveSearch(name: String, query: String): SavedSearch {
        val newSearch = SavedSearch(name = name.trim(), query = query.trim())
        runCatching {
            dataStore?.edit { prefs ->
                val raw = prefs[KEY_SAVED_SEARCHES] ?: ""
                val current = deserializeSavedSearches(raw).toMutableList()
                val existingIndex = current.indexOfFirst { it.name.equals(name.trim(), ignoreCase = true) }
                if (existingIndex >= 0) {
                    current[existingIndex] = newSearch
                } else {
                    current.add(newSearch)
                }
                prefs[KEY_SAVED_SEARCHES] = serializeSavedSearches(current)
            }
        }
        return newSearch
    }

    suspend fun deleteSavedSearch(id: String) {
        runCatching {
            dataStore?.edit { prefs ->
                val raw = prefs[KEY_SAVED_SEARCHES] ?: ""
                val current = deserializeSavedSearches(raw).filter { it.id != id }
                prefs[KEY_SAVED_SEARCHES] = serializeSavedSearches(current)
            }
        }
    }

    private fun serializeSavedSearches(searches: List<SavedSearch>): String {
        val arr = org.json.JSONArray()
        for (s in searches) {
            val obj = org.json.JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("query", s.query)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeSavedSearches(raw: String): List<SavedSearch> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<SavedSearch>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedSearch(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Search"),
                        query = obj.optString("query", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        runCatching { dataStore?.edit { it[KEY_THEME_MODE] = mode.name } }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        runCatching { dataStore?.edit { it[KEY_DYNAMIC_COLOR] = enabled } }
    }

    suspend fun setFontSize(size: Int) {
        runCatching { dataStore?.edit { it[KEY_FONT_SIZE] = size.coerceIn(12, 24) } }
    }

    suspend fun setNoteLayoutMode(mode: NoteLayoutMode) {
        runCatching { dataStore?.edit { it[KEY_NOTE_LAYOUT_MODE] = mode.name } }
    }

    suspend fun setSortOption(option: SortOption) {
        runCatching { dataStore?.edit { it[KEY_SORT_OPTION] = option.name } }
    }

    suspend fun setShowArchived(show: Boolean) {
        runCatching { dataStore?.edit { it[KEY_SHOW_ARCHIVED] = show } }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        runCatching { dataStore?.edit { it[KEY_PURE_BLACK] = enabled } }
    }

    suspend fun setTemplatesFolder(folder: String) {
        runCatching { dataStore?.edit { it[KEY_TEMPLATES_FOLDER] = folder.trim().removePrefix("/").removeSuffix("/") } }
    }

    suspend fun setDailyNotesFolder(folder: String) {
        runCatching { dataStore?.edit { it[KEY_DAILY_NOTES_FOLDER] = folder.trim().removePrefix("/").removeSuffix("/") } }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        runCatching { dataStore?.edit { it[KEY_AUTO_SAVE] = enabled } }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        runCatching { dataStore?.edit { it[KEY_BIOMETRIC_LOCK] = enabled } }
    }

    suspend fun setCalendarDefaultView(view: CalendarDefaultView) {
        runCatching { dataStore?.edit { it[KEY_CALENDAR_DEFAULT_VIEW] = view.name } }
    }

    suspend fun setCalendarFirstDayOfWeek(day: Int) {
        runCatching { dataStore?.edit { it[KEY_CALENDAR_FIRST_DAY] = day } }
    }

    suspend fun setCalendarDefaultReminderMinutes(minutes: Int) {
        runCatching { dataStore?.edit { it[KEY_CALENDAR_DEFAULT_REMINDER] = minutes } }
    }

    suspend fun setCalendarDefaultCalendar(name: String) {
        runCatching { dataStore?.edit { it[KEY_CALENDAR_DEFAULT_CALENDAR] = name.trim() } }
    }
}
