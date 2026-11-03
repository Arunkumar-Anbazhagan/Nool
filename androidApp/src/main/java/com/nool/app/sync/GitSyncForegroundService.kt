package com.nool.app.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nool.app.MainActivity
import com.nool.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class GitSyncForegroundService : Service() {

    private val syncEngine: SyncEngine by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var statusCollectorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "nool_git_sync_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.nool.app.sync.START"
        const val ACTION_STOP = "com.nool.app.sync.STOP"
        const val ACTION_SYNC_NOW = "com.nool.app.sync.SYNC_NOW"

        fun start(context: Context) {
            val intent = Intent(context, GitSyncForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GitSyncForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_SYNC_NOW -> {
                serviceScope.launch {
                    syncEngine.triggerSyncNow()
                }
            }
            else -> {
                startForegroundWithNotification()
                syncEngine.startAutoSync()
                observeSyncStatus()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val initialNotification = buildNotification("Auto-sync active", "Monitoring notes repository")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    private fun observeSyncStatus() {
        statusCollectorJob?.cancel()
        statusCollectorJob = serviceScope.launch {
            syncEngine.syncStatus.collectLatest { status ->
                val (title, content) = when (status) {
                    is SyncStatus.Idle -> "Synced" to "Up to date"
                    is SyncStatus.Syncing -> "Syncing" to status.phase
                    is SyncStatus.Synced -> "Synced" to "Repository is up to date"
                    is SyncStatus.Offline -> "Offline" to "Waiting for internet connection"
                    is SyncStatus.Conflict -> "Conflict Detected" to "${status.conflictedFiles.size} conflict(s)"
                    is SyncStatus.AuthError -> "Authentication Required" to status.message
                    is SyncStatus.Error -> "Sync Error" to status.message
                }
                updateNotification(title, content)
            }
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val syncNowIntent = Intent(this, GitSyncForegroundService::class.java).apply {
            action = ACTION_SYNC_NOW
        }
        val pendingSyncNow = PendingIntent.getService(
            this, 1, syncNowIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_popup_sync, "Sync Now", pendingSyncNow)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Git Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of notes Git synchronization"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Android 15 dataSync timeout handler:
     * When the cumulative ~6h limit is reached, gracefully stop foreground and schedule WorkManager.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        scheduleWorkManagerFallback()
        stopForegroundService()
    }

    private fun scheduleWorkManagerFallback() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<GitSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "nool_git_sync_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    private fun stopForegroundService() {
        statusCollectorJob?.cancel()
        syncEngine.stopAutoSync()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        statusCollectorJob?.cancel()
        syncEngine.stopAutoSync()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
