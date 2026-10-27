package com.nool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.nool.app.data.files.FileManager
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.index.IndexManager
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.ThemeMode
import com.nool.app.sync.SyncEngine
import com.nool.app.ui.navigation.NoolNavGraph
import com.nool.app.ui.theme.NoolTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val fileManager: FileManager by inject()
    private val indexManager: IndexManager by inject()
    private val syncEngine: SyncEngine by inject()
    private val credentialsStore: GitCredentialsStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = true)
            val pureBlack by settingsRepository.pureBlack.collectAsState(initial = false)

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            // Post-first-frame background initialization on Dispatchers.IO
            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        fileManager.ensureVaultInitialized()
                        indexManager.syncIndex()
                        syncEngine.startFileObserver()
                        if (credentialsStore.getRemoteUrl().isNotBlank()) {
                            syncEngine.startAutoSync()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            NoolTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor,
                pureBlack = pureBlack
            ) {
                val navController = rememberNavController()
                NoolNavGraph(navController = navController)
            }
        }
    }
}
