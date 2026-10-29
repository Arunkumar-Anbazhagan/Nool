package com.nool.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nool.app.MainActivity
import com.nool.app.R

class QuickNoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_NOTE = "com.nool.app.action.QUICK_NOTE"
        const val EXTRA_CREATE_QUICK_NOTE = "extra_create_quick_note"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_NOTE
            putExtra(EXTRA_CREATE_QUICK_NOTE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_quick_note).apply {
            setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            setOnClickPendingIntent(R.id.btn_new_note, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
