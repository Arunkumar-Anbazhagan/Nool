package com.nool.app.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nool.app.domain.repository.VaultRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TaskReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val vaultRepository: VaultRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val tasksResult = vaultRepository.getAllTasks()
            if (tasksResult.isFailure) return Result.retry()

            val tasks = tasksResult.getOrThrow()
            val dueToday = tasks.filter { it.isDueToday && !it.isCompleted }
            val overdue = tasks.filter { it.isOverdue && !it.isCompleted }

            if (dueToday.isNotEmpty() || overdue.isNotEmpty()) {
                sendNotification(dueToday.size, overdue.size)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(dueTodayCount: Int, overdueCount: Int) {
        val channelId = "nool_task_reminders"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about pending tasks and due dates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = when {
            overdueCount > 0 && dueTodayCount > 0 -> "You have $dueTodayCount tasks due today ($overdueCount overdue)"
            overdueCount > 0 -> "You have $overdueCount overdue tasks"
            else -> "You have $dueTodayCount tasks due today"
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nool Tasks")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
