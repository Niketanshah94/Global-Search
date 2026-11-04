package com.quickfind.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quickfind.app.R
import com.quickfind.feature.search.SearchActivity

/**
 * Home-screen widgets. They use classic RemoteViews (not Glance) because only RemoteViews
 * offers ViewFlipper, which lets the launcher animate Pal and the tips without waking the app.
 */
private fun openSearch(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun update(context: Context, manager: AppWidgetManager, ids: IntArray, layout: Int, clickable: IntArray) {
    val tap = openSearch(context)
    for (id in ids) {
        val views = RemoteViews(context.packageName, layout)
        clickable.forEach { views.setOnClickPendingIntent(it, tap) }
        manager.updateAppWidget(id, views)
    }
}

/** 4x1 search bar: Pal's face changes expression while tips slide by. */
class SearchWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        update(context, manager, ids, R.layout.widget_search, intArrayOf(R.id.widget_root, R.id.pal_flipper, R.id.hint_flipper))
}

/** 2x2 Pal: Pal cycles moods with a bounce under rotating speech bubbles. */
class PalWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        update(context, manager, ids, R.layout.widget_pal, intArrayOf(R.id.widget_root, R.id.pal_flipper, R.id.bubble_flipper))
}
