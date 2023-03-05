package com.example.locketwidget.data

import androidx.annotation.StringRes
import com.example.locketwidget.R

sealed class ScreenItem(
    val route: String,
    @StringRes val title: Int?
) {
    companion object {

        const val HISTORY_ARG = "history_model"
        const val PREVIEW_ARG = "preview_photo"
        const val PREVIEW_TYPE_ARG = "preview_type_arg"
        const val APP_GRAPH = "app_graph"
        const val LOGIN_GRAPH = "login_graph"
        const val CONNECTION_USER_ARG = "user"
        const val CONNECTION_FRIEND_ARG = "friend"
        const val WIDGET_DETAILS_ARG = "widget"
        const val WIDGET_PHOTO_ARG = "widget_photo"
        const val PREMIUM_FEATURE_SCROLL_ARG = "feature_position"
        const val GROUP_ARG = "group_arg"
        const val HISTORY_CONTACTS_ARG = "history_contacts"
        const val EMOJIS_ARG = "emojis"

        fun getRouteForHistory(historyModel: HistoryModel): String {
            return "${History.route}?$HISTORY_ARG=${historyModel.toJson()}"
        }
    }

    object OnboardingAddFriends : ScreenItem("onboarding_add_friends", null), Onboarding
    object OnboardingAddWidget : ScreenItem("onboarding_add_widget", null), Onboarding
    object OnboardingKeepInTouch : ScreenItem("onboarding_in_touch", null), Onboarding
    object OnboardingReceivePhoto : ScreenItem("onboarding_receive", null), Onboarding
    object OnboardingSendPhoto : ScreenItem("onboarding_send", null), Onboarding

    object CameraPermission : ScreenItem("camera_permission", null), Onboarding
    object AudioPermission : ScreenItem("audio_permission", null), Onboarding

    object WidgetOnboardingGuide : ScreenItem("widget_onboarding_guide", null), NoTopBar
    object WidgetSettingsGuide : ScreenItem("widget_settings_guide", null), NoTopBar
    object Premium : ScreenItem("premium", null), NoTopBar
    object Contacts : ScreenItem("contacts", null), NoTitle
    object FriendPicker : ScreenItem("friend_picker", null), NoTitle

    object Authentication : ScreenItem("auth", R.string.auth), NoTopBar
    object Connection : ScreenItem("friend_connected", R.string.connected_screen), NoTopBar
    object UserProfileCreation : ScreenItem("auth_user_settings", R.string.auth_title), NoTopBar
    object Invitation : ScreenItem("invitation", R.string.invitation_screen), NoTopBar
    object Splash : ScreenItem("splash", R.string.auth), NoTopBar

    object Locket : ScreenItem("camera", R.string.locket_screen_title)
    object TextMoments : ScreenItem("text_moments", R.string.create_image)

    object PhotoPreview : ScreenItem("photo_preview", R.string.screen_preview_title)
    object VideoPreview : ScreenItem("video_preview", R.string.video_preview_title)
    object LivePhotoPreview : ScreenItem("live_photo_preview", R.string.live_preview_title)

    object History : ScreenItem("history_details", R.string.history_screen_title)
    object HistoryList : ScreenItem("history_list", R.string.hsitory_list_screen_title)
    object Settings : ScreenItem("settings", R.string.settings_screen_title)
    object WidgetList : ScreenItem("widget_list", R.string.screen_widget_manager_title)
    object WidgetDetails : ScreenItem("widget_details", R.string.widget_details_title)
    object ProfileSettings : ScreenItem("profile_settings", R.string.settings_profile_title)
    object NewGroup : ScreenItem("new_group", R.string.create_new_group), CrossButton, NoTitle
    object HistoryContactsSelection : ScreenItem("history_contacts_selection", null), CrossButton, NoTitle

    object Drawing : ScreenItem("feature_drawing", R.string.drawing_screen_title)
    object DrawingMoment : ScreenItem("feature_drawing_model", R.string.drawing_screen_title)

    object SelectMoment : ScreenItem("select_moment", R.string.select_moment_title)
    object DownloadingMoments : ScreenItem("downloading_moments", null), NoTopBar

    //  object TimeLineEditor : ScreenItem("timeline_editor", R.string.timeline_editor_title)
    object SpeedSettings : ScreenItem("timeline_speed", R.string.timeline_speed_settings_title)
    object CreatingMoments : ScreenItem("creating_moment", null), NoTopBar
    object ShareMood : ScreenItem("share_mood", R.string.share_mood_title)
}

interface CrossButton
interface NoTitle
interface NoTopBar
interface Onboarding : NoTopBar
