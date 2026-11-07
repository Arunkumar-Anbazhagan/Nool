package com.nool.app.di

import com.nool.app.data.files.FileManager
import com.nool.app.data.files.VaultRepositoryImpl
import com.nool.app.data.git.GitCloneNotificationHelper
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitRepository
import com.nool.app.data.git.GitRepositoryImpl
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nool.core.index.db.NoolDatabase
import com.nool.core.index.SqlDelightIndexManager
import com.nool.core.index.VaultWatcher
import com.nool.app.data.index.AndroidFileObserverVaultWatcher
import com.nool.app.data.index.IndexManager
import com.nool.app.domain.repository.VaultRepository
import com.nool.app.settings.SettingsRepository
import com.nool.app.sync.NetworkMonitor
import com.nool.app.sync.SyncEngine
import com.nool.app.sync.SyncEngineImpl
import com.nool.app.ui.git.GitViewModel
import com.nool.app.ui.notes.NoteEditViewModel
import com.nool.app.ui.notes.NotesViewModel
import com.nool.app.ui.search.SearchViewModel
import com.nool.app.ui.settings.SettingsViewModel
import com.nool.app.ui.trash.TrashViewModel
import com.nool.app.data.share.ShareManager
import com.nool.app.ui.share.ShareViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FileManager(androidContext()) }
    single {
        val schema = NoolDatabase.Schema
        val callback = object : AndroidSqliteDriver.Callback(schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        id TEXT PRIMARY KEY NOT NULL,
                        calendarId TEXT NOT NULL DEFAULT 'Personal',
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        isAllDay INTEGER NOT NULL DEFAULT 0,
                        colorHex TEXT NOT NULL DEFAULT '#6750A4',
                        rrule TEXT,
                        reminderMinutes INTEGER NOT NULL DEFAULT 15,
                        linkedNotePath TEXT,
                        createdTimestamp INTEGER NOT NULL DEFAULT 0,
                        lastModified INTEGER NOT NULL DEFAULT 0
                    );
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_start_end ON calendar_events(startTime, endTime);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_calendar ON calendar_events(calendarId);")
            }
        }
        val driver = AndroidSqliteDriver(
            schema = schema,
            context = androidContext(),
            name = "nool_index_v4.db",
            callback = callback
        )
        NoolDatabase(driver)
    }
    single { SqlDelightIndexManager(get()) }
    single<VaultWatcher> { AndroidFileObserverVaultWatcher() }
    single { IndexManager(get<FileManager>().vaultRoot, get<SqlDelightIndexManager>()) }
    single { SettingsRepository(androidContext()) }
    single<VaultRepository> { VaultRepositoryImpl(get(), get()) }
    single { ShareManager(androidContext(), get()) }
    single { com.nool.app.data.templates.TemplateManager(get(), get()) }
    single { com.nool.app.data.flashcards.FlashcardManager(get()) }

    // Git & Sync Singletons
    single { GitCredentialsStore(androidContext()) }
    single { GitCloneNotificationHelper(androidContext()) }
    single { com.nool.app.sync.GitCloneManager(androidContext(), get()) }
    single { NetworkMonitor(androidContext()) }
    single<GitRepository> { GitRepositoryImpl(get<FileManager>().vaultRoot, get()) }
    single<SyncEngine> {
        SyncEngineImpl(
            context = androidContext(),
            gitRepository = get(),
            credentialsStore = get(),
            indexManager = get(),
            networkMonitor = get(),
            vaultDir = get<FileManager>().vaultRoot
        )
    }

    // ViewModels
    viewModel { (folder: String) -> NotesViewModel(get(), get(), folder, get(), get()) }
    viewModel { (notePath: String) -> NoteEditViewModel(get(), notePath, get(), get()) }
    viewModel { TrashViewModel(get()) }
    viewModel { (initialTag: String?, initialQuery: String?) -> SearchViewModel(get(), get(), initialTag, initialQuery) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { GitViewModel(get(), get(), get(), get(), get()) }
    viewModel { (initialNotePath: String?) -> com.nool.app.ui.graph.GraphViewModel(get(), initialNotePath) }
    viewModel { ShareViewModel(get(), get()) }
    viewModel { com.nool.app.ui.calendar.CalendarViewModel(get(), get(), get(), androidContext()) }
}
