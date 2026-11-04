package com.nool.app.ui.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nool.app.MainActivity
import com.nool.core.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object CalendarReminderScheduler {

    private fun getWorkTag(eventId: String) = "calendar_event_reminder_$eventId"

    fun scheduleReminder(context: Context, event: CalendarEvent) {
        if (event.reminderMinutes <= 0) {
            cancelReminder(context, event.id)
            return
        }

        val triggerTime = event.startTime - (event.reminderMinutes * 60 * 1000L)
        val delayMs = triggerTime - System.currentTimeMillis()

        if (delayMs <= 0) {
            // Event reminder time has already passed
            return
        }

        val inputData = Data.Builder()
            .putString("event_id", event.id)
            .putString("event_title", event.title)
            .putLong("event_start", event.startTime)
            .putString("linked_note", event.linkedNotePath ?: "")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CalendarReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkTag(event.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkTag(eventId))
    }
}

class CalendarReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val eventTitle = inputData.getString("event_title") ?: "Upcoming Event"
        val eventStart = inputData.getLong("event_start", 0L)
        val eventId = inputData.getString("event_id") ?: ""

        val timeStr = if (eventStart > 0) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(eventStart))
        } else ""

        val channelId = "nool_calendar_reminders"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Calendar Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies about scheduled calendar events"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(eventTitle)
            .setContentText(if (timeStr.isNotEmpty()) "Starts at $timeStr" else "Scheduled event starting soon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)

        return Result.success()
    }
}
