package com.mytools.sleepsnore.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mytools.sleepsnore.MainActivity
import com.mytools.sleepsnore.R
import com.mytools.sleepsnore.data.db.SleepDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 桌面极简打鼾与睡眠监测小组件 Provider
 */
class SleepAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SleepAppWidgetProvider::class.java)
            val allIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_sleep_summary)

            // 点击小组件启动 MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_action, pendingIntent)

            // 异步读取最近一次睡眠记录
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = SleepDatabase.getInstance(context)
                    val latest = db.sleepDao().getLatestSession()

                    if (latest != null) {
                        views.setTextViewText(R.id.widget_score_value, latest.snoreScore.toInt().toString())
                        val totalH = latest.sessionDurationSeconds / 3600
                        val totalM = (latest.sessionDurationSeconds % 3600) / 60
                        views.setTextViewText(R.id.widget_duration, "在床: ${totalH}h ${totalM}m")

                        val snoreM = latest.snoringDurationSeconds / 60
                        views.setTextViewText(R.id.widget_snore_time, "打鼾: ${snoreM}分钟")

                        val ahiText = if (latest.estimatedAhi < 5f) "AHI 正常" else "AHI ${"%.1f".format(latest.estimatedAhi)}"
                        views.setTextViewText(R.id.widget_ahi_badge, ahiText)
                    } else {
                        views.setTextViewText(R.id.widget_score_value, "--")
                        views.setTextViewText(R.id.widget_duration, "在床: 暂无数据")
                        views.setTextViewText(R.id.widget_snore_time, "打鼾: --")
                        views.setTextViewText(R.id.widget_ahi_badge, "待监测")
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
