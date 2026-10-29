package com.nool.app.di

import com.nool.app.data.files.FileManager
import com.nool.app.data.files.VaultRepositoryImpl
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitRepository
import com.nool.app.data.git.GitRepositoryImpl
import com.nool.app.data.index.IndexDao
import com.nool.app.data.index.IndexManager
import com.nool.app.data.index.NoolDatabase
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
    single { NoolDatabase.create(androidContext()) }
    single<IndexDao> { get<NoolDatabase>().indexDao() }
    single { IndexManager(get<FileManager>().vaultRoot, get()) }
    single { SettingsRepository(androidContext()) }
    single<VaultRepository> { VaultRepositoryImpl(get(), get()) }
    single { ShareManager(androidContext(), get()) }

    // Git & Sync Singletons
    single { GitCredentialsStore(androidContext()) }
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
    viewModel { (folder: String) -> NotesViewModel(get(), get(), folder) }
    viewModel { (notePath: String) -> NoteEditViewModel(get(), notePath) }
    viewModel { TrashViewModel(get()) }
    viewModel { (initialTag: String?) -> SearchViewModel(get(), initialTag) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { GitViewModel(get(), get(), get()) }
    viewModel { (initialNotePath: String?) -> com.nool.app.ui.graph.GraphViewModel(get(), initialNotePath) }
    viewModel { ShareViewModel(get(), get()) }
}
