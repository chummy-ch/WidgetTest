package com.example.locketwidget.work

import android.content.Context
import androidx.work.*
import com.example.locketwidget.data.HistoryResponse
import com.example.locketwidget.work.UpdateFMCTokenWork.Companion.TOKEN_KEY
import com.example.locketwidget.work.UpdateWidgetWork.Companion.WIDGET_PHOTO_ID_KEY
import java.util.concurrent.TimeUnit

class WidgetWorkUseCase(private val context: Context) {
    companion object {
        const val TAG = "widget_update_work"
        private const val WORK_MANAGER_DELAY = 15L
        private const val WORK_MANAGER_FLEX_PERIOD = 5L
        private val WORK_MANAGER_TIME_UNIT = TimeUnit.MINUTES
        const val WIDGET_ID_PARAM = "widget_id"
    }

    fun createScheduleWork() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<ScheduleUpdateWidgetWork>(WORK_MANAGER_DELAY, WORK_MANAGER_TIME_UNIT)
            .setConstraints(constraints)
            .addTag(TAG)
            .build()
        workManager.enqueue(work)
    }

    fun createInitWidgetWork(widgetId: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val data = Data.Builder().apply {
            putInt(WIDGET_ID_PARAM, widgetId)
        }.build()
        val work = OneTimeWorkRequestBuilder<CreateInitialWidgetWork>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.MINUTES)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    fun createRemoveWidgetIdWork(widgetIds: IntArray) {
        val data = Data.Builder().apply { putIntArray(RemoveWidgetWorker.WIDGET_REMOVE_ID_KEY, widgetIds) }.build()
        val work = OneTimeWorkRequestBuilder<RemoveWidgetWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    fun createUpdateWidgetWork(historyResponse: HistoryResponse) {
        val data = Data.Builder().apply {
            putString(WIDGET_PHOTO_ID_KEY, historyResponse.mapToHistoryModel().toJson())
        }.build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<UpdateWidgetWork>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    fun createUpdateTokenWork(token: String) {
        val data = Data.Builder().apply {
            putString(TOKEN_KEY, token)
        }.build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<UpdateFMCTokenWork>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }
}