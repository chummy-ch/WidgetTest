package com.example.locketwidget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.HistoryMode
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.WidgetShape
import com.example.locketwidget.data.WidgetShapes
import com.example.locketwidget.local.LocalWidgetsRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.widget.UserInfo
import com.example.locketwidget.widget.WidgetManager
import org.koin.java.KoinJavaComponent.inject

class UpdateWidgetWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val WIDGET_PHOTO_ID_KEY = "widget_photo_id"
    }

    private val firestoreDataSource: FirestoreDataSource by inject(FirestoreDataSource::class.java)
    private val widgetManager: WidgetManager by inject(WidgetManager::class.java)
    private val widgetsRepository: LocalWidgetsRepository by inject(LocalWidgetsRepository::class.java)

    override suspend fun doWork(): Result {
        val historyJson = inputData.getString(WIDGET_PHOTO_ID_KEY) ?: return Result.failure()
        val history = LocketGson.gson.fromJson(historyJson, HistoryModel::class.java)

        val widgets = widgetsRepository.getWidgets().filter { it.friends.contains(history.senderId) || it.friends.isEmpty() }

        val isSenderShown = widgets.any {
            it.style is WidgetShape && it.style.shape == WidgetShapes.Rectangle && it.isSenderInfoShown || history.mode is HistoryMode.Mood
        }
        val userInfo = if (isSenderShown) {
            val senderResult = firestoreDataSource.getUser(history.senderId)
            if (senderResult is com.example.locketwidget.core.Result.Success && senderResult.data.photoLink != null) {
                UserInfo(senderResult.data.photoLink, senderResult.data.name)
            } else null
        } else null

        widgets.forEach { widgetModel ->
            widgetManager.updateWidgetImage(history, widgetModel.id, widgetModel.style, userInfo)
        }

        return Result.success()
    }
}