package com.nool.app.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nool.app.MainActivity
import com.nool.app.data.git.CloneJobState
import com.nool.app.data.git.CloneJobStatus
import com.nool.app.data.git.CloneStrategy
import com.nool.app.data.git.GitAuth
import com.nool.app.data.git.GitCloneNotificationHelper
import com.nool.app.data.git.GitCredentialsStore
import com.nool.app.data.git.GitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GitCloneForegroundService : Service() {

    private val gitRepository: GitRepository by inject()
    private val credentialsStore: GitCredentialsStore by inject()
    private val cloneManager: GitCloneManager by inject()
    private val notificationHelper: GitCloneNotificationHelper by inject()
    private val networkMonitor: NetworkMonitor by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "nool_git_clone_channel"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_SUBFOLDER = "extra_subfolder"
        const val EXTRA_AUTH_TYPE = "extra_auth_type"
        const val EXTRA_STRATEGY = "extra_strategy"

        fun start(
            context: Context,
            url: String,
            subfolder: String,
            authType: String,
            strategy: CloneStrategy
        ) {
            val intent = Intent(context, GitCloneForegroundService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_SUBFOLDER, subfolder)
                putExtra(EXTRA_AUTH_TYPE, authType)
                putExtra(EXTRA_STRATEGY, strategy.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: credentialsStore.getCloneJobState().url
        val subfolder = intent?.getStringExtra(EXTRA_SUBFOLDER) ?: credentialsStore.getCloneJobState().subfolder
        val authType = intent?.getStringExtra(EXTRA_AUTH_TYPE) ?: credentialsStore.getCloneJobState().authType
        val strategyStr = intent?.getStringExtra(EXTRA_STRATEGY) ?: credentialsStore.getCloneJobState().strategy.name
        val strategy = try {
            CloneStrategy.valueOf(strategyStr)
        } catch (_: Exception) {
            CloneStrategy.FAIL_IF_NOT_EMPTY
        }

        if (url.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification("Starting clone...", 0f)

        serviceScope.launch {
            acquireWakeLock()
            try {
                executeCloneWithRetry(url, subfolder, authType, strategy)
            } finally {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "nool:GitCloneWakeLock"
            )?.apply {
                acquire(15 * 60 * 1000L) // Safe 15 min limit
            }
        } catch (_: Throwable) {}
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (_: Throwable) {}
        wakeLock = null
    }

    private fun startForegroundNotification(phase: String, progress: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Git Clone Progress",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cloning Git Repository")
            .setContentText(phase)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, (progress * 100).toInt(), progress <= 0f)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun executeCloneWithRetry(
        url: String,
        subfolder: String,
        authType: String,
        strategy: CloneStrategy
    ) {
        val auth = when (authType) {
            "HTTPS" -> {
                val token = credentialsStore.getHttpsToken()
                val user = credentialsStore.getHttpsUser()
                if (!token.isNullOrBlank()) GitAuth.Token(token, user) else GitAuth.None
            }
            "SSH" -> {
                val priv = credentialsStore.getSshPrivateKey() ?: ""
                val pass = credentialsStore.getSshPassphrase()
                GitAuth.Ssh(priv, pass)
            }
            else -> GitAuth.None
        }

        var attempt = 0
        val maxAttempts = 3
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            attempt++
            if (!networkMonitor.networkState.value.isConnected) {
                cloneManager.updateState(
                    CloneJobState(
                        url = url,
                        subfolder = subfolder,
                        authType = authType,
                        strategy = strategy,
                        status = CloneJobStatus.RUNNING,
                        phase = "Waiting for network connectivity...",
                        progress = 0f
                    )
                )
                notificationHelper.showProgress("Waiting for network...", 0f)
                delay(3000L)
            }

            cloneManager.updateState(
                CloneJobState(
                    url = url,
                    subfolder = subfolder,
                    authType = authType,
                    strategy = strategy,
                    status = CloneJobStatus.RUNNING,
                    phase = if (attempt > 1) "Retrying clone (attempt $attempt/$maxAttempts)..." else "Connecting to remote...",
                    progress = 0f
                )
            )

            val result = gitRepository.clone(
                remoteUrl = url,
                targetSubfolder = subfolder,
                auth = auth,
                strategy = strategy,
                onProgress = { phase, progress ->
                    cloneManager.updateState(
                        CloneJobState(
                            url = url,
                            subfolder = subfolder,
                            authType = authType,
                            strategy = strategy,
                            status = CloneJobStatus.RUNNING,
                            phase = phase,
                            progress = progress
                        )
                    )
                    notificationHelper.showProgress(phase, progress)
                }
            )

            if (result.isSuccess) {
                val repoDisplayName = subfolder.ifBlank {
                    url.trim().removeSuffix("/").removeSuffix(".git").substringAfterLast("/").ifBlank { "repository" }
                }
                cloneManager.updateState(
                    CloneJobState(
                        url = url,
                        subfolder = subfolder,
                        authType = authType,
                        strategy = strategy,
                        status = CloneJobStatus.SUCCESS,
                        phase = "Completed",
                        progress = 1f
                    )
                )
                notificationHelper.showSuccess(repoDisplayName)
                return
            } else {
                lastError = result.exceptionOrNull()
                val isNetworkError = lastError?.message?.contains("network", ignoreCase = true) == true ||
                        lastError?.message?.contains("connection", ignoreCase = true) == true ||
                        lastError?.message?.contains("timed out", ignoreCase = true) == true ||
                        lastError?.message?.contains("transport", ignoreCase = true) == true

                if (isNetworkError && attempt < maxAttempts) {
                    val backoff = 1000L * (1 shl (attempt - 1))
                    cloneManager.updateState(
                        CloneJobState(
                            url = url,
                            subfolder = subfolder,
                            authType = authType,
                            strategy = strategy,
                            status = CloneJobStatus.RUNNING,
                            phase = "Network connection lost. Retrying in ${backoff / 1000}s...",
                            progress = 0f
                        )
                    )
                    notificationHelper.showProgress("Retrying in ${backoff / 1000}s...", 0f)
                    delay(backoff)
                } else {
                    break
                }
            }
        }

        val errorMsg = lastError?.message ?: "Clone failed"
        cloneManager.updateState(
            CloneJobState(
                url = url,
                subfolder = subfolder,
                authType = authType,
                strategy = strategy,
                status = CloneJobStatus.FAILED,
                phase = "Failed",
                progress = 0f,
                errorMessage = errorMsg
            )
        )
        notificationHelper.showError(errorMsg)
    }
}
