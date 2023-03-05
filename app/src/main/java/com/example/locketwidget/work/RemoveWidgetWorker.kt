package com.example.locketwidget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locketwidget.local.LocalWidgetsRepository
import org.koin.java.KoinJavaComponent.inject

class RemoveWidgetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        const val WIDGET_REMOVE_ID_KEY = "widget_remove_id"
    }

    private val widgetRepository: LocalWidgetsRepository by inject(LocalWidgetsRepository::class.java)

    override suspend fun doWork(): Result {
        val ids = inputData.getIntArray(WIDGET_REMOVE_ID_KEY) ?: return Result.failure()
        widgetRepository.remove(ids)
        return Result.success()
    }
}