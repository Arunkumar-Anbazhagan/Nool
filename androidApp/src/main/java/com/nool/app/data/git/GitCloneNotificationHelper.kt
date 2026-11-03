package com.nool.app.data.git

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nool.app.MainActivity

class GitCloneNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Git Clone Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress while cloning Git repositories"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun showProgress(phase: String, progress: Float) {
        try {
            val max = 100
            val current = (progress * 100).toInt().coerceIn(0, 100)
            val isIndeterminate = progress <= 0f && (phase.isBlank() || phase.contains("remote", ignoreCase = true) || phase.contains("connect", ignoreCase = true))

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Cloning Git Repository")
                .setContentText(if (phase.isBlank()) "Downloading objects..." else phase)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(getContentIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(max, current, isIndeterminate)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }

    fun showSuccess(repoName: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Clone Successful")
                .setContentText("Repository '$repoName' cloned and ready.")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(getContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }

    fun showError(errorMsg: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Clone Failed")
                .setContentText(errorMsg)
                .setStyle(NotificationCompat.BigTextStyle().bigText(errorMsg))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentIntent(getContentIntent())
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }

    fun cancel() {
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    companion object {
        private const val CHANNEL_ID = "nool_git_clone_channel"
        private const val NOTIFICATION_ID = 2001
    }
}
