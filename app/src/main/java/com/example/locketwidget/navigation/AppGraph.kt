package com.example.locketwidget.navigation

import android.net.Uri
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.*
import com.example.locketwidget.features.drawing.DrawingMomentScreen
import com.example.locketwidget.features.drawing.DrawingScreen
import com.example.locketwidget.features.timeline.CreatingMomentsScreen
import com.example.locketwidget.features.timeline.DownloadingScreen
import com.example.locketwidget.features.timeline.SelectMomentScreen
import com.example.locketwidget.features.timeline.SpeedSettingsScree
import com.example.locketwidget.screens.ConnectionScreen
import com.example.locketwidget.screens.auth.InvitationScreen
import com.example.locketwidget.screens.auth.UserSettingsScreen
import com.example.locketwidget.screens.contacts.ContactsScreen
import com.example.locketwidget.screens.contacts_group.NewGroup
import com.example.locketwidget.screens.history.HistoryContactsSelectionScreen
import com.example.locketwidget.screens.history.HistoryListScreen
import com.example.locketwidget.screens.history.HistoryScreen
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.screens.main.MainScreen
import com.example.locketwidget.screens.mood.ShareMoodScreen
import com.example.locketwidget.screens.premium.PremiumScreen
import com.example.locketwidget.screens.preview.FriendsPickScreen
import com.example.locketwidget.screens.preview.LivePhotoPreviewScreen
import com.example.locketwidget.screens.preview.PhotoPreviewScreen
import com.example.locketwidget.screens.preview.VideoPreviewScreen
import com.example.locketwidget.screens.settings.SettingsScreen
import com.example.locketwidget.screens.textmoments.TextMomentsScreen
import com.example.locketwidget.screens.widgetguide.WidgetOnboardingGuideScreen
import com.example.locketwidget.screens.widgetguide.WidgetSettingsGuideScreen
import com.example.locketwidget.screens.widgetmanager.WidgetDetails
import com.example.locketwidget.screens.widgetmanager.WidgetManager

private fun getFirestoreUserFromJson(json: String) = LocketGson.gson.fromJson(json, FirestoreUserResponse::class.java)

private fun NavGraphBuilder.mainScreen(
    cropPhoto: (Uri) -> Unit,
    navController: NavController
) {
    composable(ScreenItem.Locket.route) {
        MainScreen(cropPhoto = cropPhoto, navController = navController)
    }
}

private fun NavGraphBuilder.photoPreviewScreen(
    navController: NavController
) {
    composable(
        "${ScreenItem.PhotoPreview.route}?${ScreenItem.PREVIEW_ARG}={${ScreenItem.PREVIEW_ARG}}," +
                "${ScreenItem.PREVIEW_TYPE_ARG}={${ScreenItem.PREVIEW_TYPE_ARG}}," +
                "${ScreenItem.EMOJIS_ARG}={${ScreenItem.EMOJIS_ARG}}",
        arguments = listOf(
            navArgument(ScreenItem.PREVIEW_ARG) { type = NavType.StringType },
            navArgument(ScreenItem.PREVIEW_TYPE_ARG) { type = NavType.BoolType },
            navArgument(ScreenItem.EMOJIS_ARG) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) { backStack ->
        val photoPath = backStack.arguments?.getString(ScreenItem.PREVIEW_ARG)!!
        val isPhoto = backStack.arguments?.getBoolean(ScreenItem.PREVIEW_TYPE_ARG, true)!!
        val emojisJson = backStack.arguments?.getString(ScreenItem.EMOJIS_ARG)
        val emojis: List<String>? = LocketGson.gson.fromJson(emojisJson, List::class.java) as? List<String>
        PhotoPreviewScreen(
            photoPath = photoPath,
            isPhoto = isPhoto,
            emojis = emojis,
            navController = navController
        )
    }
}

private fun NavGraphBuilder.videoPreviewScreen(
    navController: NavController
) {
    composable(
        route = "${ScreenItem.VideoPreview.route}?${ScreenItem.PREVIEW_ARG}={${ScreenItem.PREVIEW_ARG}}",
        arguments = listOf(
            navArgument(ScreenItem.PREVIEW_ARG) { type = NavType.StringType }
        )
    ) { backStack ->
        val photoPath = backStack.arguments?.getString(ScreenItem.PREVIEW_ARG)!!
        VideoPreviewScreen(
            videoPath = photoPath,
            navController = navController
        )
    }
}

private fun NavGraphBuilder.livePhotoPreviewScreen(
    navController: NavController
) {
    composable(
        route = "${ScreenItem.LivePhotoPreview.route}?${ScreenItem.PREVIEW_ARG}={${ScreenItem.PREVIEW_ARG}}",
        arguments = listOf(navArgument(ScreenItem.PREVIEW_ARG) { type = NavType.StringType })
    ) { backStack ->
        val outputString = backStack.arguments?.getString(ScreenItem.PREVIEW_ARG)!!
        val output = LocketGson.gson.fromJson(outputString, CameraOutput.Live::class.java)
        LivePhotoPreviewScreen(liveOutput = output, navController = navController)
    }
}

private fun NavGraphBuilder.textMomentsScreen(
    navController: NavController
) {
    composable(ScreenItem.TextMoments.route) {
        TextMomentsScreen(navController = navController)
    }
}

private fun NavGraphBuilder.friendPickerScreen(
    navController: NavController
) {
    composable(ScreenItem.FriendPicker.route) {
        FriendsPickScreen {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.contactsScreen(
    navController: NavController
) {
    composable(ScreenItem.Contacts.route) {
        ContactsScreen(navController = navController)
    }
}

private fun NavGraphBuilder.settingsScreen(
    navController: NavController,
    logout: () -> Unit
) {
    composable(ScreenItem.Settings.route) {
        SettingsScreen(navController, logout)
    }
}

private fun NavGraphBuilder.historyListScreen(
    navController: NavController
) {
    composable(ScreenItem.HistoryList.route) {
        HistoryListScreen(navController = navController)
    }
}

private fun NavGraphBuilder.newGroupScreen(
    navController: NavController
) {
    composable(
        route = "${ScreenItem.NewGroup.route}?${ScreenItem.GROUP_ARG}={${ScreenItem.GROUP_ARG}}",
        arguments = listOf(navArgument(ScreenItem.GROUP_ARG) {
            type = NavType.StringType
            nullable = true
        })
    ) { backEntry ->
        val json = backEntry.arguments?.getString(ScreenItem.GROUP_ARG)
        val group = LocketGson.gson.fromJson(json, DataStoreGroupModel::class.java)
        NewGroup(groupModel = group, navController = navController)
    }
}

private fun NavGraphBuilder.historyScreen(
    navController: NavController
) {
    composable(
        route = "${ScreenItem.History.route}?${ScreenItem.HISTORY_ARG}={${ScreenItem.HISTORY_ARG}}",
        arguments = listOf(navArgument(ScreenItem.HISTORY_ARG) { type = NavType.StringType })
    ) { backEntry ->
        val historyJson = backEntry.arguments?.getString(ScreenItem.HISTORY_ARG)
        val history = LocketGson.gson.fromJson(historyJson, HistoryModel::class.java)

        HistoryScreen(historyModel = history, navController = navController)
    }
}

private fun NavGraphBuilder.connectionScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.Connection.route + "?${ScreenItem.CONNECTION_USER_ARG}={${ScreenItem.CONNECTION_USER_ARG}}," +
                "${ScreenItem.CONNECTION_FRIEND_ARG}={${ScreenItem.CONNECTION_FRIEND_ARG}}",
        arguments = listOf(
            navArgument(ScreenItem.CONNECTION_USER_ARG) { type = NavType.StringType },
            navArgument(ScreenItem.CONNECTION_FRIEND_ARG) { type = NavType.StringType }
        )
    ) { backStack ->
        val userJson = backStack.arguments?.getString(ScreenItem.CONNECTION_USER_ARG) ?: return@composable
        val friendJson = backStack.arguments?.getString(ScreenItem.CONNECTION_FRIEND_ARG) ?: return@composable

        ConnectionScreen(
            user = getFirestoreUserFromJson(userJson),
            friend = getFirestoreUserFromJson(friendJson)
        ) {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.widgetManagerScreen(
    navController: NavController
) {
    composable(route = ScreenItem.WidgetList.route) { backstack ->
        WidgetManager(navController = navController)
    }
}

private fun NavGraphBuilder.widgetDetailsScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.WidgetDetails.route + "/{${ScreenItem.WIDGET_DETAILS_ARG}}?${ScreenItem.WIDGET_PHOTO_ARG}={${ScreenItem.WIDGET_PHOTO_ARG}}",
        arguments = listOf(
            navArgument(ScreenItem.WIDGET_DETAILS_ARG) { type = NavType.StringType },
            navArgument(ScreenItem.WIDGET_PHOTO_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        val widget = it.arguments?.getString(ScreenItem.WIDGET_DETAILS_ARG)!!
        val photo = it.arguments?.getString(ScreenItem.WIDGET_PHOTO_ARG)

        WidgetDetails(
            locketWidget = LocketGson.gson.fromJson(widget, LocketWidgetModel::class.java),
            photoLink = photo,
            navController = navController
        )
    }
}

private fun NavGraphBuilder.profileSettingsScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.ProfileSettings.route
    ) {
        UserSettingsScreen {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.invitationScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.Invitation.route
    ) {
        InvitationScreen {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.widgetOnboardingGuideScreen(
    navController: NavController
) {
    composable(route = ScreenItem.WidgetOnboardingGuide.route) {
        WidgetOnboardingGuideScreen(navController = navController)
    }
}

private fun NavGraphBuilder.widgetSettingsScreen(
    navController: NavController
) {
    composable(route = ScreenItem.WidgetSettingsGuide.route) {
        WidgetSettingsGuideScreen(navController = navController)
    }
}

private fun NavGraphBuilder.historyContactsSelectionsScreen(
    navController: NavController
) {
    composable(
        route = "${ScreenItem.HistoryContactsSelection.route}/{${ScreenItem.HISTORY_CONTACTS_ARG}}",
        arguments = listOf(navArgument(ScreenItem.HISTORY_CONTACTS_ARG) { type = NavType.LongType })
    ) {
        val key = it.arguments?.getLong(ScreenItem.HISTORY_CONTACTS_ARG, 0)!!
        HistoryContactsSelectionScreen(navController = navController, key = key) {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.premiumScreen(navController: NavController) {
    composable(
        route = "${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}={${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}}",
        arguments = listOf(navArgument(ScreenItem.PREMIUM_FEATURE_SCROLL_ARG) {
            type = NavType.IntType
            defaultValue = 0
        })
    ) { backStack ->
        val scrollPosition = backStack.arguments?.getInt(ScreenItem.PREMIUM_FEATURE_SCROLL_ARG, 0)
        PremiumScreen(navController = navController, selectedScreen = scrollPosition)
    }
}

private fun NavGraphBuilder.drawingScreen(navController: NavController) {
    composable(
        route = ScreenItem.Drawing.route
    ) {
        DrawingScreen(navController = navController)
    }
}

private fun NavGraphBuilder.drawingMoment(navController: NavController) {
    composable(
        route = ScreenItem.DrawingMoment.route
    ) {
        DrawingMomentScreen(navController = navController)
    }
}

private fun NavGraphBuilder.selectMomentScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.SelectMoment.route
    ) {
        SelectMomentScreen(navController = navController)
    }
}

private fun NavGraphBuilder.downloadingMomentsScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.DownloadingMoments.route
    ) {
        DownloadingScreen(navController = navController)
    }
}

private fun NavGraphBuilder.creatingMomentScreen(
    navController: NavController
) {
    composable(
        route = ScreenItem.CreatingMoments.route
    ) {
        CreatingMomentsScreen(navController = navController)
    }
}

private fun NavGraphBuilder.timelineEditorScreen(
    navController: NavController
) {

    /*composable(
        route = ScreenItem.TimeLineEditor.route
    ) {
        TimeLineEditorScreen(navController = navController)
    }*/
}

private fun NavGraphBuilder.speedSettingsScreen(navController: NavController) {
    composable(
        route = ScreenItem.SpeedSettings.route
    ) {
        SpeedSettingsScree(navController = navController)
    }
}

private fun NavGraphBuilder.shareMoodScreen(navController: NavController) {
    composable(route = ScreenItem.ShareMood.route) {
        ShareMoodScreen(navController = navController)
    }
}

fun NavGraphBuilder.appGraph(
    navController: NavController,
    cropPhoto: (Uri) -> Unit,
    logout: () -> Unit,
) {
    navigation(startDestination = ScreenItem.Locket.route, route = ScreenItem.APP_GRAPH) {
        mainScreen(cropPhoto, navController)

        photoPreviewScreen(navController)

        videoPreviewScreen(navController)

        livePhotoPreviewScreen(navController)

        textMomentsScreen(navController)

        friendPickerScreen(navController)

        contactsScreen(navController)

        settingsScreen(navController, logout)

        historyListScreen(navController)

        newGroupScreen(navController)

        historyListScreen(navController)

        connectionScreen(navController)

        widgetManagerScreen(navController)

        widgetDetailsScreen(navController)

        profileSettingsScreen(navController)

        invitationScreen(navController)

        widgetOnboardingGuideScreen(navController)

        widgetSettingsScreen(navController)

        historyContactsSelectionsScreen(navController)

        premiumScreen(navController)

        drawingScreen(navController)

        drawingMoment(navController)

        selectMomentScreen(navController)

        downloadingMomentsScreen(navController)

        creatingMomentScreen(navController)

        speedSettingsScreen(navController)

        shareMoodScreen(navController)

    }
}