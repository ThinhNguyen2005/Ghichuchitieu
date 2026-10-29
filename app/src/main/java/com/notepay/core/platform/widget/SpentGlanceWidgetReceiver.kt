package com.notepay.platform.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class SpentGlanceWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetUpdateHelper.updateSpentGlanceWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetConstants.ACTION_REFRESH_WIDGETS) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SpentGlanceWidgetReceiver::class.java),
            )
            if (ids.isNotEmpty()) {
                WidgetUpdateHelper.updateSpentGlanceWidgets(context, appWidgetManager, ids)
            }
        }
    }
}
