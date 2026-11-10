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
                // Self-healing: Ensure all core tables and indices exist
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        relativePath TEXT NOT NULL UNIQUE,
                        title TEXT NOT NULL,
                        folderPath TEXT NOT NULL,
                        content TEXT NOT NULL,
                        excerpt TEXT NOT NULL,
                        lastModified INTEGER NOT NULL,
                        createdTimestamp INTEGER NOT NULL,
                        size INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        tags TEXT NOT NULL DEFAULT '',
                        aliases TEXT NOT NULL DEFAULT '',
                        noteType TEXT NOT NULL DEFAULT 'Note'
                    );
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sourcePath TEXT NOT NULL,
                        rawTarget TEXT NOT NULL,
                        targetTitle TEXT NOT NULL,
                        targetHeading TEXT,
                        alias TEXT,
                        isEmbed INTEGER NOT NULL DEFAULT 0,
                        resolvedPath TEXT
                    );
                    """.trimIndent()
                )
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
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_notes_folder ON notes(folderPath);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_notes_last_modified ON notes(lastModified);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_links_source ON links(sourcePath);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_links_target ON links(targetTitle);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_links_resolved ON links(resolvedPath);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_start_end ON calendar_events(startTime, endTime);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_calendar ON calendar_events(calendarId);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_note ON calendar_events(linkedNotePath);")

                try {
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS note_fts USING fts5(
                            title,
                            content,
                            tags,
                            folderPath,
                            content='notes',
                            content_rowid='id',
                            prefix='2 3',
                            tokenize='unicode61'
                        );
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS notes_ai AFTER INSERT ON notes BEGIN
                            INSERT INTO note_fts(rowid, title, content, tags, folderPath)
                            VALUES (new.id, new.title, new.content, new.tags, new.folderPath);
                        END;
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS notes_ad AFTER DELETE ON notes BEGIN
                            INSERT INTO note_fts(note_fts, rowid, title, content, tags, folderPath)
                            VALUES ('delete', old.id, old.title, old.content, old.tags, old.folderPath);
                        END;
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS notes_au AFTER UPDATE ON notes BEGIN
                            INSERT INTO note_fts(note_fts, rowid, title, content, tags, folderPath)
                            VALUES ('delete', old.id, old.title, old.content, old.tags, old.folderPath);
                            INSERT INTO note_fts(rowid, title, content, tags, folderPath)
                            VALUES (new.id, new.title, new.content, new.tags, new.folderPath);
                        END;
                        """.trimIndent()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val driver = AndroidSqliteDriver(
            schema = schema,
            context = androidContext(),
            name = "nool_index_v5.db",
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
    single { com.nool.app.data.tasks.TaskManager(get(), get()) }

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
    viewModel { com.nool.app.ui.tasks.TasksViewModel(get()) }
}
