package com.nool.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nool_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class SortOption(val label: String) {
    MODIFIED_DESC("Last modified (newest)"),
    MODIFIED_ASC("Last modified (oldest)"),
    CREATED_DESC("Created (newest)"),
    CREATED_ASC("Created (oldest)"),
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)")
}

class SettingsRepository(
    private val context: Context? = null,
    customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore = customDataStore ?: context?.dataStore

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
        private val KEY_SORT_OPTION = stringPreferencesKey("sort_option")
        private val KEY_SHOW_ARCHIVED = booleanPreferencesKey("show_archived")
        private val KEY_PURE_BLACK = booleanPreferencesKey("pure_black")
        private val KEY_TEMPLATES_FOLDER = stringPreferencesKey("templates_folder")
        private val KEY_DAILY_NOTES_FOLDER = stringPreferencesKey("daily_notes_folder")
    }

    val themeMode: Flow<ThemeMode> = dataStore?.data?.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try { ThemeMode.valueOf(raw) } catch (e: Exception) { ThemeMode.SYSTEM }
    } ?: kotlinx.coroutines.flow.flowOf(ThemeMode.SYSTEM)

    val dynamicColor: Flow<Boolean> = dataStore?.data?.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    } ?: kotlinx.coroutines.flow.flowOf(true)

    val fontSize: Flow<Int> = dataStore?.data?.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: 16
    } ?: kotlinx.coroutines.flow.flowOf(16)

    val sortOption: Flow<SortOption> = dataStore?.data?.map { prefs ->
        val raw = prefs[KEY_SORT_OPTION] ?: SortOption.MODIFIED_DESC.name
        try { SortOption.valueOf(raw) } catch (e: Exception) { SortOption.MODIFIED_DESC }
    } ?: kotlinx.coroutines.flow.flowOf(SortOption.MODIFIED_DESC)

    val showArchived: Flow<Boolean> = dataStore?.data?.map { prefs ->
        prefs[KEY_SHOW_ARCHIVED] ?: false
    } ?: kotlinx.coroutines.flow.flowOf(false)

    val pureBlack: Flow<Boolean> = dataStore?.data?.map { prefs ->
        prefs[KEY_PURE_BLACK] ?: false
    } ?: kotlinx.coroutines.flow.flowOf(false)

    val templatesFolder: Flow<String> = dataStore?.data?.map { prefs ->
        prefs[KEY_TEMPLATES_FOLDER] ?: "Templates"
    } ?: kotlinx.coroutines.flow.flowOf("Templates")

    val dailyNotesFolder: Flow<String> = dataStore?.data?.map { prefs ->
        prefs[KEY_DAILY_NOTES_FOLDER] ?: "Daily Notes"
    } ?: kotlinx.coroutines.flow.flowOf("Daily Notes")

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore?.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore?.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFontSize(size: Int) {
        dataStore?.edit { it[KEY_FONT_SIZE] = size.coerceIn(12, 24) }
    }

    suspend fun setSortOption(option: SortOption) {
        dataStore?.edit { it[KEY_SORT_OPTION] = option.name }
    }

    suspend fun setShowArchived(show: Boolean) {
        dataStore?.edit { it[KEY_SHOW_ARCHIVED] = show }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        dataStore?.edit { it[KEY_PURE_BLACK] = enabled }
    }

    suspend fun setTemplatesFolder(folder: String) {
        dataStore?.edit { it[KEY_TEMPLATES_FOLDER] = folder.trim().removePrefix("/").removeSuffix("/") }
    }

    suspend fun setDailyNotesFolder(folder: String) {
        dataStore?.edit { it[KEY_DAILY_NOTES_FOLDER] = folder.trim().removePrefix("/").removeSuffix("/") }
    }
}
