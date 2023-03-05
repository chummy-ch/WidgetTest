package com.example.locketwidget.messaging

import android.accounts.NetworkErrorException
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.media.RingtoneManager
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.example.locketwidget.MainActivity
import com.example.locketwidget.R
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.HistoryMode
import com.example.locketwidget.data.HistoryResponse
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.work.WidgetWorkUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Parcelize
data class HistoryMessageModel(
    val date: Long,
    val photoId: String,
    val mode: HistoryMode?
) : Parcelable

class LocketMessagingService : FirebaseMessagingService() {
    companion object {
        const val REACTION_CHANNEL_ID = "reaction"
        const val EVENT_PHOTO = "photo"
        const val REACTION_HISTORY_KEY = "historyModel"
        const val SENDER_ID_KEY = "userId"
        const val EVENT_CONNECTION = "connection"
        const val EVENT_TIMELINE = "timeline"
        const val EVENT_FRIENDS = "friends"
        const val EVENT_WIDGETS = "widgets"
        private const val NOTIFICATION_ID = 0
    }

    private val widgetWorkUseCase: WidgetWorkUseCase by inject()
    private val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isEmpty()) return

        val event = message.data["event"]
        if (event != null) {
            when (event) {
                REACTION_CHANNEL_ID -> handleReactionMessage(message.data)
                EVENT_PHOTO -> handleUpdateWidgetMessage(message.data)
                EVENT_CONNECTION -> handleConnectionMessage(message.data)
                EVENT_TIMELINE -> handleTimelineMessage(message.data)
                EVENT_FRIENDS -> handleAddFriendsMessage(message.data)
                EVENT_WIDGETS -> handleAddWidgetMessage(message.data)
            }
        }
    }

    private fun handleConnectionMessage(data: Map<String, String>) {
        val title = data["title"] ?: ""
        val body = data["body"] ?: ""
        val senderId = data["senderId"] ?: return
        val senderPhoto = data["senderPhoto"] ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(SENDER_ID_KEY, senderId)
        }
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, EVENT_CONNECTION)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_profile)
            .setContentIntent(resultPendingIntent)
            .setSound(sound)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        val imageLoader = Coil.imageLoader(this)

        val photoImageRequest = ImageRequest.Builder(this).data(senderPhoto).target(
            onSuccess = {
                builder.setLargeIcon(it.toBitmap())
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
            }
        ).build()

        imageLoader.enqueue(photoImageRequest)
    }

    private fun handleTimelineMessage(data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EVENT_TIMELINE, EVENT_TIMELINE)
        }
        handleMessage(data = data, icon = R.drawable.ic_camera_reel, intent = intent)
    }

    private fun handleAddFriendsMessage(data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EVENT_FRIENDS, EVENT_FRIENDS)
        }
        handleMessage(data = data, icon = R.drawable.ic_profile, intent = intent)
    }

    private fun handleAddWidgetMessage(data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EVENT_WIDGETS, EVENT_WIDGETS)
        }
        handleMessage(data = data, icon = R.drawable.ic_menu, intent = intent)
    }


    private fun handleMessage(data: Map<String, String>, @DrawableRes icon: Int, intent: Intent) {
        val title = data["title"] ?: ""
        val body = data["body"] ?: ""

        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, REACTION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(icon)
            .setContentIntent(resultPendingIntent)
            .setSound(sound)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
    }

    private fun handleReactionMessage(data: Map<String, String>) {
        val title = data["title"] ?: ""
        val body = data["body"] ?: ""
        val iconUrl = data["senderPhoto"]
        val photoUrl = data["photoURL"]
        val photoId = data["photoId"] ?: ""
        val dateSec = data["photoCreatedTimestamp"] ?: ""
        val video = data["video"]
        val live = data["live"]
        val mode = when {
            video != null -> HistoryMode.Video(video, FileRepository.getVideoLinkById(video))
            live != null -> HistoryMode.Live(live)
            else -> null
        }
        val historyModel = HistoryMessageModel(dateSec.toLong(), photoId, mode)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(REACTION_HISTORY_KEY, historyModel)
        }
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, REACTION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_reaction)
            .setContentIntent(resultPendingIntent)
            .setSound(sound)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        val imageLoader = Coil.imageLoader(this)

        val photoImageRequest = ImageRequest.Builder(this).data(photoUrl).target(
            onSuccess = {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(it.toBitmap())
                )
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
            }
        ).build()

        val iconImageRequest = ImageRequest.Builder(this).data(iconUrl).target(
            onSuccess = {
                builder.setLargeIcon(it.toBitmap())
                imageLoader.enqueue(photoImageRequest)
            }
        ).build()

        imageLoader.enqueue(iconImageRequest)

    }

    private fun handleUpdateWidgetMessage(data: Map<String, String>) {
        val photoId = data["photo"] ?: return
        val senderId = data["sender"] ?: return
        val video = data["video"]
        val time = data["photoCreatedTimestamp"]
        val emojis = data["emojis"]?.run { split(',') }

        val historyModel = HistoryResponse(
            date = Timestamp(
                time?.let { Date(it.toLong() * 1000) } ?: Date()
            ),
            photo = photoId,
            sender = FirebaseFirestore.getInstance().collection(FirestoreDataSource.USER_COLLECTION).document(senderId),
            video = video,
            emojis = emojis
        )

        if (FirebaseAuth.getInstance().currentUser?.uid != senderId) {
            widgetWorkUseCase.createUpdateWidgetWork(historyModel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        widgetWorkUseCase.createUpdateTokenWork(token)
    }

    suspend fun updateToken() = suspendCoroutine<Result<String>> { con ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                con.resume(Result.Success(token))
            } else {
                val exception = task.exception ?: NetworkErrorException()
                con.resume(Result.Error(exception))
            }
        }
    }
}
