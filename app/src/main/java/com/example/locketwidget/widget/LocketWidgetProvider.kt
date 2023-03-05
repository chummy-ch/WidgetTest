package com.example.locketwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.locketwidget.MainActivity
import com.example.locketwidget.R
import com.example.locketwidget.work.WidgetWorkUseCase
import org.koin.java.KoinJavaComponent.inject

class LocketWidgetProvider : AppWidgetProvider() {

    private val workUseCase: WidgetWorkUseCase by inject(WidgetWorkUseCase::class.java)

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds?.forEach { id ->
            workUseCase.createInitWidgetWork(id)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds != null) {
            workUseCase.createRemoveWidgetIdWork(appWidgetIds)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.photo_widget
            ).apply {
                val intent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.root, intent)
            }
            setWidgetSize(context, appWidgetId, remoteViews)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setWidgetSize(context: Context, appWidgetId: Int, remoteViews: RemoteViews) {
        val size = WidgetSizeProvider(context.applicationContext).getWidgetsSize(appWidgetId)
        val height = size.height
        val width = size.width
        val min = minOf(height, width)
        remoteViews.setViewLayoutHeight(R.id.root, min.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        remoteViews.setViewLayoutWidth(R.id.root, min.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
    }
}

class WidgetSizeProvider(context: Context) {

    private val appContext = context.applicationContext

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    data class Size(
        val height: Int,
        val width: Int
    )

    fun getWidgetsSize(widgetId: Int): Size {
        val isPortrait = appContext.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val width = getWidgetWidth(isPortrait, widgetId)
        val height = getWidgetHeight(isPortrait, widgetId)
        return Size(appContext.dip(height), appContext.dip(width))
    }

    private fun getWidgetWidth(isPortrait: Boolean, widgetId: Int): Int =
        if (isPortrait) {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }

    private fun getWidgetHeight(isPortrait: Boolean, widgetId: Int): Int =
        if (isPortrait) {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        }

    private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
        appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

    private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}