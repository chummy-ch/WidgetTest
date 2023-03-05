package com.example.locketwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.locketwidget.MainActivity
import com.example.locketwidget.R
import com.example.locketwidget.data.*
import com.example.locketwidget.local.PhotoEditor
import com.example.locketwidget.network.FileRepository
import java.util.*
import kotlin.math.roundToInt


data class UserInfo(
    val photo: String,
    val name: String
)

class WidgetManager(private val context: Context, private val photoEditor: PhotoEditor) {

    private val widgetManager = AppWidgetManager.getInstance(context)
    private val imageLoader = ImageLoader.Builder(context).diskCache {
        DiskCache.Builder().directory(context.cacheDir.resolve("image_cache")).build()
    }.build()
    private val imageRequest
        get() = ImageRequest.Builder(context).allowConversionToBitmap(true).bitmapConfig(Bitmap.Config.ARGB_8888)

    suspend fun updateWidgetImage(
        historyModel: HistoryModel,
        id: Int,
        style: WidgetStyle,
        userInfo: UserInfo? = null
    ) {
        val intent = PendingIntent.getActivity(
            context,
            Date().time.toInt(),
            Intent(context, MainActivity::class.java).apply { putExtra(MainActivity.HISTORY_DETAILS_KEY, historyModel) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remoteViews = RemoteViews(context.packageName, R.layout.photo_widget).apply {
            setOnClickPendingIntent(R.id.root, intent)
        }
        remoteViews.removeAllViews(R.id.flipper)
        remoteViews.removeAllViews(R.id.live_flipper)
        remoteViews.setViewVisibility(R.id.photo_image_view, View.VISIBLE)

        val isUserInfoVisible = userInfo != null && style is WidgetShape && style.shape == WidgetShapes.Rectangle
                || historyModel.mode is HistoryMode.Mood
        bindUserInfoVisibility(remoteViews, isUserInfoVisible)
        bindPlayButtonVisibility(remoteViews, historyModel.mode is HistoryMode.Video)

        val photoLink = if (historyModel.mode is HistoryMode.Video) {
            FileRepository.getPhotoThumbLinkById(historyModel.photoId)
        } else {
            historyModel.photoLink
        }

        if (historyModel.mode !is HistoryMode.Mood) {
            when (style) {
                is WidgetShape -> bindShapedWidget(photoLink, style, remoteViews, id, userInfo, historyModel.mode)
                is WidgetForeground -> bindForegroundedWidget(photoLink, style, remoteViews, id, historyModel.mode)
            }
        } else {
            require(userInfo != null) { "User info must not be null for the mood history" }
            bindMoodWidget(photoLink, remoteViews, id, userInfo)
        }
    }

    fun updateEmptyPhotoWidgetImage(widgetId: Int) {
        val intent = PendingIntent.getActivity(
            context,
            Date().time.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val remoteViews = RemoteViews(context.packageName, R.layout.photo_widget).apply {
            setOnClickPendingIntent(R.id.root, intent)
        }
        remoteViews.removeAllViews(R.id.flipper)

        bindUserInfoVisibility(remoteViews, false)
        bindPlayButtonVisibility(remoteViews, false)

        val drawable = ContextCompat.getDrawable(context, R.drawable.widget_no_photo)

        if (drawable != null) {
            remoteViews.setImageViewBitmap(R.id.photo_image_view, drawable.toBitmap())
            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    fun updateEmptyFriendsWidgetImage(widgetId: Int) {
        val intent = PendingIntent.getActivity(
            context,
            Date().time.toInt(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.INVITE_FRIENDS_KEY, MainActivity.INVITE_FRIENDS_KEY)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val remoteViews = RemoteViews(context.packageName, R.layout.photo_widget)
        remoteViews.setOnClickPendingIntent(R.id.root, intent)

        bindUserInfoVisibility(remoteViews, false)
        bindPlayButtonVisibility(remoteViews, false)

        val drawable = ContextCompat.getDrawable(context, R.drawable.widget_no_friends)

        if (drawable != null) {
            remoteViews.setImageViewBitmap(R.id.photo_image_view, drawable.toBitmap())
            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    private suspend fun bindMoodWidget(
        photo: String,
        remoteViews: RemoteViews,
        widgetId: Int,
        userInfo: UserInfo
    ) {
        val userInfoRequest = imageRequest
            .data(userInfo.photo)
            .transformations(CircleCropTransformation())
            .target(
                onSuccess = { result ->
                    remoteViews.setImageViewBitmap(R.id.user_photo_image_view, result.toBitmap())
                    remoteViews.setTextViewText(R.id.user_name_text_view, userInfo.name)
                    widgetManager.updateAppWidget(widgetId, remoteViews)
                }
            ).build()
        val photoRequest = imageRequest.data(photo).target(
            onSuccess = { result ->
                remoteViews.setImageViewBitmap(R.id.photo_image_view, result.toBitmap())
                widgetManager.updateAppWidget(widgetId, remoteViews)
            }
        ).build()

        imageLoader.enqueue(photoRequest)
        imageLoader.enqueue(userInfoRequest)
    }

    private fun bindPlayButtonVisibility(remoteViews: RemoteViews, isVisible: Boolean) {
        if (isVisible) {
            remoteViews.setViewVisibility(R.id.play_button, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.play_button, View.GONE)
        }
    }

    private fun bindUserInfoVisibility(remoteViews: RemoteViews, isVisible: Boolean) {
        if (isVisible) {
            remoteViews.setViewVisibility(R.id.nested_view, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.nested_view, View.GONE)
        }
    }

    private suspend fun bindForegroundedWidget(
        photo: String,
        foreground: WidgetForeground,
        remoteViews: RemoteViews,
        widgetId: Int,
        historyMode: HistoryMode?
    ) {
        for (i in 0..foreground.frames) {
            val uri = "@drawable/x_${foreground.asset}_frame_$i"
            val resource = context.resources.getIdentifier(uri, null, context.packageName)
            val imageView = RemoteViews(context.packageName, R.layout.widget_frame).apply {
                setImageViewResource(R.id.frame_image_view, resource)
            }
            remoteViews.addView(R.id.flipper, imageView)
        }
        val padding = (context.resources.displayMetrics.density * foreground.padding).roundToInt()
        if (historyMode is HistoryMode.Live) {
            remoteViews.setViewPadding(R.id.live_flipper, padding, padding, padding, padding)
        } else {
            remoteViews.setViewPadding(R.id.photo_image_view, padding, padding, padding, padding)
        }
        remoteViews.setInt(R.id.flipper, "setFlipInterval", foreground.interval)
        val request = imageRequest.data(photo)


        if (historyMode is HistoryMode.Live) {
            handleLiveMode(historyMode, remoteViews, request, widgetId)
        } else {
            imageLoader.enqueue(
                request.target(
                    onSuccess = { result ->
                        remoteViews.setImageViewBitmap(R.id.photo_image_view, result.toBitmap())
                        widgetManager.updateAppWidget(widgetId, remoteViews)
                    }
                ).build()

            )
        }
    }

    private suspend fun bindShapedWidget(
        photo: String,
        shape: WidgetShape,
        remoteViews: RemoteViews,
        widgetId: Int,
        userInfo: UserInfo? = null,
        mode: HistoryMode?
    ) {
        if (userInfo != null) {
            val userInfoRequest = imageRequest
                .data(userInfo.photo)
                .transformations(CircleCropTransformation())
                .target(
                    onSuccess = { result ->
                        remoteViews.setImageViewBitmap(R.id.user_photo_image_view, result.toBitmap())
                        remoteViews.setTextViewText(R.id.user_name_text_view, userInfo.name)
                        widgetManager.updateAppWidget(widgetId, remoteViews)
                    }
                ).build()
            imageLoader.enqueue(userInfoRequest)
        }

        val request = imageRequest.data(photo).transformations(WidgetTransformation(shape.shape))

        if (mode is HistoryMode.Live) {
            handleLiveMode(mode, remoteViews, request, widgetId, shape)
        } else {
            imageLoader.enqueue(
                request.target(
                    onSuccess = { result ->
                        val bitmap =
                            if (shape.shape == WidgetShapes.Rectangle && shape.stroke.colors.first() != Color.Transparent.toArgb()) {
                                photoEditor.addCustomStroke(result.toBitmap(), shape.stroke)
                            } else {
                                result.toBitmap()
                            }

                        remoteViews.setImageViewBitmap(R.id.photo_image_view, bitmap)
                        widgetManager.updateAppWidget(widgetId, remoteViews)
                    }
                ).build()
            )
        }
    }

    private suspend fun handleLiveMode(
        live: HistoryMode.Live,
        remoteViews: RemoteViews,
        photoRequest: ImageRequest.Builder,
        widgetId: Int,
        shape: WidgetShape = WidgetShape(WidgetShapes.Rectangle, false)
    ) {
        remoteViews.setViewVisibility(R.id.photo_image_view, View.GONE)

        val mainBitmap = imageLoader.execute(photoRequest.build()).drawable?.handleShape(shape)

        if (mainBitmap != null) {
            val liveRequest = imageRequest.data(live.link)
                .transformations(WidgetTransformation(shape.shape))
                .build()
            val liveBitmap = imageLoader.execute(liveRequest).drawable?.handleShape(shape)
            liveBitmap?.let { bitmap ->
                val livePhoto = photoEditor.createLivePhoto(mainBitmap, bitmap)
                val livePhotoVariation = photoEditor.createLivePhoto(bitmap, mainBitmap)
                val firstFrameView = RemoteViews(context.packageName, R.layout.widget_frame).apply {
                    setImageViewBitmap(R.id.frame_image_view, livePhoto)
                }
                remoteViews.addView(R.id.live_flipper, firstFrameView)
                val secondFrameView = RemoteViews(context.packageName, R.layout.widget_frame).apply {
                    setImageViewBitmap(R.id.frame_image_view, livePhotoVariation)
                }
                remoteViews.addView(R.id.live_flipper, secondFrameView)
                widgetManager.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

    private fun Drawable.handleShape(shape: WidgetShape) = run {
        if (shape.stroke.direction != GradientDirection.NONE) {
            photoEditor.addCustomStroke(toBitmap(), shape.stroke)
        } else {
            toBitmap()
        }
    }
}