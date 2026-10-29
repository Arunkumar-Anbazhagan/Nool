package com.nool.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.nool.app.data.files.FileManager
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.index.IndexManager
import com.nool.app.data.share.ShareManager
import com.nool.app.security.BiometricHelper
import com.nool.app.settings.SettingsRepository
import com.nool.app.settings.ThemeMode
import com.nool.app.sync.SyncEngine
import com.nool.app.ui.navigation.NoolNavGraph
import com.nool.app.ui.navigation.Screen
import com.nool.app.ui.share.ShareBottomSheet
import com.nool.app.ui.share.ShareEvent
import com.nool.app.ui.share.ShareViewModel
import com.nool.app.ui.theme.NoolTheme
import com.nool.app.widget.QuickNoteWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val fileManager: FileManager by inject()
    private val indexManager: IndexManager by inject()
    private val syncEngine: SyncEngine by inject()
    private val credentialsStore: GitCredentialsStore by inject()
    private val shareManager: ShareManager by inject()
    private val shareViewModel: ShareViewModel by inject()

    private var backgroundTimestampMs = 0L
    private val isUnlockedState = mutableStateOf(true)
    private var isBiometricConfigured = false
    private val pendingQuickNoteState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        handleQuickNoteIntent(intent)
        enableEdgeToEdge()

        // Check if biometric lock is active at launch
        CoroutineScope(Dispatchers.Main).launch {
            val biometricEnabled = settingsRepository.biometricLock.first()
            isBiometricConfigured = biometricEnabled
            if (biometricEnabled) {
                isUnlockedState.value = false
                triggerBiometricPrompt()
            } else {
                isUnlockedState.value = true
            }
        }

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = true)
            val pureBlack by settingsRepository.pureBlack.collectAsState(initial = false)
            val biometricLockEnabled by settingsRepository.biometricLock.collectAsState(initial = false)
            val shareUiState by shareViewModel.uiState.collectAsState()
            val isUnlocked by isUnlockedState

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
                val pendingQuickNote by pendingQuickNoteState

                LaunchedEffect(pendingQuickNote, isUnlocked) {
                    if (pendingQuickNote && isUnlocked) {
                        navController.navigate(Screen.NoteDetail.createRoute(""))
                        pendingQuickNoteState.value = false
                    }
                }

                LaunchedEffect(Unit) {
                    shareViewModel.events.collectLatest { event ->
                        when (event) {
                            is ShareEvent.Success -> {
                                Toast.makeText(this@MainActivity, "Saved to notes", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.NoteDetail.createRoute(event.notePath))
                            }
                            is ShareEvent.Error -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                if (biometricLockEnabled && !isUnlocked) {
                    // Biometric Lock Screen Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Nool is Locked",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Confirm your biometric credential to access your notes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { triggerBiometricPrompt() },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock")
                            }
                        }
                    }
                } else {
                    NoolNavGraph(navController = navController)

                    if (shareUiState.sharedContent != null) {
                        ShareBottomSheet(
                            uiState = shareUiState,
                            onDismiss = { shareViewModel.dismiss() },
                            onSave = { shareViewModel.save() },
                            onAppendModeChanged = { shareViewModel.setAppendMode(it) },
                            onTitleChanged = { shareViewModel.setNoteTitle(it) },
                            onFolderSelected = { shareViewModel.setSelectedFolder(it) },
                            onNoteSelected = { shareViewModel.setSelectedNotePath(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestampMs = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundTimestampMs > 0L) {
            CoroutineScope(Dispatchers.Main).launch {
                val biometricEnabled = settingsRepository.biometricLock.first()
                if (BiometricHelper.shouldLockOnForeground(biometricEnabled, backgroundTimestampMs)) {
                    isUnlockedState.value = false
                    triggerBiometricPrompt()
                }
                backgroundTimestampMs = 0L
            }
        }
    }

    private fun triggerBiometricPrompt() {
        try {
            BiometricHelper.authenticate(
                activity = this,
                onSuccess = {
                    isUnlockedState.value = true
                },
                onError = { _, _ ->
                    // Keep locked; user can tap "Unlock" button to retry
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
        handleQuickNoteIntent(intent)
    }

    private fun handleQuickNoteIntent(intent: Intent?) {
        if (intent == null) return
        val isQuickNote = intent.getBooleanExtra(QuickNoteWidgetProvider.EXTRA_CREATE_QUICK_NOTE, false) ||
                intent.action == QuickNoteWidgetProvider.ACTION_QUICK_NOTE
        if (isQuickNote) {
            pendingQuickNoteState.value = true
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val content = shareManager.extractSharedContent(intent)
        if (content != null) {
            shareViewModel.setSharedContent(content)
        }
    }
}
