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

class ScheduleUpdateWidgetWork(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), WidgetWorker {

    private val widgetManager: WidgetManager by inject(WidgetManager::class.java)
    private val firestoreDataSource: FirestoreDataSource by inject(FirestoreDataSource::class.java)
    private val widgetsRepository: LocalWidgetsRepository by inject(LocalWidgetsRepository::class.java)

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.failure()
        val widgets = widgetsRepository.getWidgets()
        val friendsResult = firestoreDataSource.getConnectionIds(user.uid)
        if (friendsResult is com.example.locketwidget.core.Result.Success) {
            val isFriendListEmpty = friendsResult.data.isEmpty()

            widgets.forEach { widget ->
                if (isFriendListEmpty) {
                    widgetManager.updateEmptyFriendsWidgetImage(widget.id)
                } else {
                    val history = firestoreDataSource.getLastPhotoFromSenders(user.uid, widget.friends)

                    if (history is com.example.locketwidget.core.Result.Success) {
                        proceedWidgetUpdate(history.data, widget, firestoreDataSource::getUser, widgetManager::updateWidgetImage)
                    } else {
                        widgetManager.updateEmptyPhotoWidgetImage(widget.id)
                    }
                }
            }
        }
        return Result.success()
    }
}
