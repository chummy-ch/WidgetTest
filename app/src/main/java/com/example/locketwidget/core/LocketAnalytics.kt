package com.example.locketwidget.core

import androidx.camera.core.CameraSelector
import com.example.locketwidget.core.LocketAnalytics.Events.SELECT_MOMENTS
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

object LocketAnalytics {
    object Params {
        const val CAMERA = "camera"
        const val CAMERA_FRONT = "camera_front"
        const val CAMERA_BACK = "camera_back"
        const val FRONT_CAMERA_RESOLUTION = "front_resolution"
        const val BACK_CAMERA_RESOLUTION = "back_resolution"
        const val THEME = "theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val ERROR = "error"
        const val DEFAULT_ERROR = "unknown error"
        const val SHAPE = "shape"
        const val BACKGROUND_NAME = "background_name"
        const val SCREEN_NAME = "screen_name"
        const val MOMENT_CREATING_TIME = "moment_creating_time"
        const val WIDGETS_COUNT = "widgets_count"
        const val FRIENDS_COUNT = "friends_count"
    }

    object Events {
        const val ONBOARDING_FINISHED = "onboarding_finished"
        const val REGISTRATION_FINISHED = "registration_finished"
        const val FRIEND_LINK_SHARED = "friend_link_shared"
        const val TAKE_PHOTO = "take_photo"
        const val SEND_PHOTO = "send_photo"
        const val SEND_TEXT_MOMENT = "send_text_moment"
        const val TEXT_MOMENT_DONE = "text_moment_done"
        const val FAIL_SEND_PHOTO = "fail_send_photo"
        const val WIDGET_ADDED = "widget_added"
        const val WIDGET_SHAPE = "widget_shape"
        const val USER_PHOTO_SELECT = "user_photo_select"
        const val DOWNLOAD_PHOTO = "download_photo"
        const val SHARE_PHOTO = "share_photo"
        const val WIDGET_FRIENDS_CHANGE = "widget_friends_change"
        const val REVIEW_APP = "review_app"
        const val TEXT_BACKGROUND_SELECTED = "text_background_selected"
        const val SELECT_MOMENTS = "moments_select"
        const val EXPORT_MOMENTS = "moments_export"
        const val MOMENT_CREATED = "moment_created"
        const val OPEN_PREMIUM_SCREEN = "open_premium_screen"
        const val BUY_PREMIUM = "buy_premium"
        const val DOWNLOAD_VIDEO = "download_video"
        const val VIDEO_SENT = "video_sent"
        const val VIDEO_SENT_FAIL = "video_send_fail"
        const val LIVE_PHOTO_SEND = "live_photo_send"
    }

    var isFrontResolutionSet = false
    var isBackResolutionSet = false
    var currentCamera = Params.CAMERA_FRONT

    fun shouldLogResolution(): Boolean {
        return if (currentCamera == Params.CAMERA_FRONT) !isFrontResolutionSet
        else !isBackResolutionSet
    }

    private val analytics = Firebase.analytics.apply {
        setUserId(FirebaseAuth.getInstance().currentUser?.uid)
        setUserProperty(Params.CAMERA, Params.CAMERA_FRONT)
    }

    fun setThemeProperty(isDarkTheme: Boolean) {
        val theme = if (isDarkTheme) Params.THEME_DARK else Params.THEME_LIGHT
        analytics.setUserProperty(Params.THEME, theme)
    }

    fun setCameraProperty(camera: Int) {
        val cameraString =
            if (camera == CameraSelector.LENS_FACING_BACK) Params.CAMERA_BACK else Params.CAMERA_FRONT
        analytics.setUserProperty(Params.CAMERA, cameraString)
        currentCamera = cameraString
    }

    fun setCameraResolution(resolution: Int) {
        analytics.setUserProperty(currentCamera, resolution.toString())

        if (currentCamera == Params.CAMERA_FRONT) isFrontResolutionSet = true
        else isBackResolutionSet = true
    }

    fun setFriendsCount(count: Int) {
        analytics.setUserProperty(Params.FRIENDS_COUNT, count.toString())
    }

    fun setWidgetCount(count: Int) {
        analytics.setUserProperty(Params.WIDGETS_COUNT, count.toString())
    }

    fun logLivePhotoSend() {
        analytics.logEvent(Events.LIVE_PHOTO_SEND, null)
    }

    fun logVideoSendFail() {
        analytics.logEvent(Events.VIDEO_SENT_FAIL, null)
    }

    fun logSuccessVideoSend() {
        analytics.logEvent(Events.VIDEO_SENT, null)
    }

    fun logMomentSelection() {
        analytics.logEvent(SELECT_MOMENTS, null)
    }

    fun logSharingMoment() {
        analytics.logEvent(Events.EXPORT_MOMENTS, null)
    }

    fun logMomentCreated(time: Long) {
        analytics.logEvent(Events.MOMENT_CREATED) {
            param(Params.MOMENT_CREATING_TIME, time)
        }
    }

    fun logScreen(name: String) {
        val nameParam = if (!name.contains("?")) name
        else {
            name.substring(0, name.indexOf("?"))
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, nameParam)
        }
    }

    fun logTextBackgroundSelected(name: String) {
        analytics.logEvent(
            Events.TEXT_BACKGROUND_SELECTED
        ) {
            param(Params.BACKGROUND_NAME, name)
        }
    }

    fun logTextMomentDone(name: String) {
        analytics.logEvent(
            Events.TEXT_MOMENT_DONE
        ) {
            param(Params.BACKGROUND_NAME, name)
        }
    }

    fun logOnboardingFinished() {
        analytics.logEvent(Events.ONBOARDING_FINISHED, null)
    }

    fun logRegistrationFinished() = analytics.logEvent(Events.REGISTRATION_FINISHED, null)

    fun logFriendLinkCopy() = analytics.logEvent(Events.FRIEND_LINK_SHARED, null)

    fun logTakePhoto() = analytics.logEvent(Events.TAKE_PHOTO, null)

    fun logSendPhoto() = analytics.logEvent(Events.SEND_PHOTO, null)

    fun logSendTextMoment() = analytics.logEvent(Events.SEND_TEXT_MOMENT, null)

    fun logFailSendPhoto(error: String?) = analytics.logEvent(Events.FAIL_SEND_PHOTO) {
        param(Params.ERROR, error ?: Params.DEFAULT_ERROR)
    }

    fun logWidgetAdded() = analytics.logEvent(Events.WIDGET_ADDED, null)

    fun logWidgetShapeChange(shape: String) = analytics.logEvent(Events.WIDGET_SHAPE) {
        param(Params.SHAPE, shape)
    }

    fun logUserPhotoSelect() = analytics.logEvent(Events.USER_PHOTO_SELECT, null)

    fun logDownloadVideo() = analytics.logEvent(Events.DOWNLOAD_VIDEO, null)

    fun logDownloadPhoto() = analytics.logEvent(Events.DOWNLOAD_PHOTO, null)

    fun logSharePhoto() = analytics.logEvent(Events.SHARE_PHOTO, null)

    fun logWidgetFriendListChange() = analytics.logEvent(Events.WIDGET_FRIENDS_CHANGE, null)

    fun logReviewApp() = analytics.logEvent(Events.REVIEW_APP, null)

    fun logOpenPremium(screenName: String) = analytics.logEvent(Events.OPEN_PREMIUM_SCREEN) {
        param(Params.SCREEN_NAME, screenName)
    }

    fun logClickBuyPremiumButton() = analytics.logEvent(Events.BUY_PREMIUM, null)
}