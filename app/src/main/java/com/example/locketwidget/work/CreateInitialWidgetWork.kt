package com.example.locketwidget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locketwidget.core.WidgetWorker
import com.example.locketwidget.local.LocalWidgetsRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.widget.WidgetManager
import com.google.firebase.auth.FirebaseAuth
import org.koin.java.KoinJavaComponent.inject

class CreateInitialWidgetWork(context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params), WidgetWorker {

    private val widgetsRepository: LocalWidgetsRepository by inject(LocalWidgetsRepository::class.java)
    private val firestoreDataSource: FirestoreDataSource by inject(FirestoreDataSource::class.java)
    private val widgetManager: WidgetManager by inject(WidgetManager::class.java)

    override suspend fun doWork(): Result {
        val widgetId = params.inputData.getInt(WidgetWorkUseCase.WIDGET_ID_PARAM, -1)
        if (widgetId == -1) return Result.failure()
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.failure()

        val widgetInfo = widgetsRepository.getWidgetByIdOrCreate(widgetId)

        val friendsResult = firestoreDataSource.getConnectionIds(user.uid)
        return if (friendsResult is com.example.locketwidget.core.Result.Success) {
            val history = firestoreDataSource.getLastPhotoFromSenders(user.uid, widgetInfo.friends)
            val isFriendListEmpty = friendsResult.data.isEmpty()
            if (isFriendListEmpty) {
                widgetManager.updateEmptyFriendsWidgetImage(widgetInfo.id)
            } else if (history is com.example.locketwidget.core.Result.Success) {
                proceedWidgetUpdate(history.data, widgetInfo, firestoreDataSource::getUser, widgetManager::updateWidgetImage)
            } else {
                widgetManager.updateEmptyPhotoWidgetImage(widgetInfo.id)
            }
            Result.success()
        } else Result.retry()
    }
}