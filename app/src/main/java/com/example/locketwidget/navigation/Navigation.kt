package com.example.locketwidget

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.*
import com.example.locketwidget.features.drawing.DrawingMomentScreen
import com.example.locketwidget.features.drawing.DrawingScreen
import com.example.locketwidget.features.timeline.CreatingMomentsScreen
import com.example.locketwidget.features.timeline.DownloadingScreen
import com.example.locketwidget.features.timeline.SelectMomentScreen
import com.example.locketwidget.features.timeline.SpeedSettingsScree
import com.example.locketwidget.navigation.appGraph
import com.example.locketwidget.navigation.loginGraph
import com.example.locketwidget.screens.AuthScreen
import com.example.locketwidget.screens.ConnectionScreen
import com.example.locketwidget.screens.SplashScreen
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
import com.example.locketwidget.screens.onboarding.*
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


@Composable
fun LocketNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    userAsync: Async<*>,
    logout: () -> Unit,
    cropPhoto: (Uri) -> Unit,
    loggingCallback: LoggingCallback
) {

    LaunchedEffect(Unit) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            LocketAnalytics.logScreen(destination.route ?: "error destination")
        }
    }

    var isSplashEnded by remember { mutableStateOf(false) }

    val startDestination = remember(userAsync) {
        derivedStateOf {
            when (userAsync) {
                is Success -> {
                    if (isSplashEnded) {
                        ScreenItem.APP_GRAPH
                    } else {
                        ScreenItem.Splash.route
                    }
                }
                is Uninitialized -> ScreenItem.Splash.route
                else -> ScreenItem.LOGIN_GRAPH
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination.value,
        modifier = modifier
    ) {
        composable(ScreenItem.Splash.route) { SplashScreen(closeSplash = { isSplashEnded = true }) }
        loginGraph(navController, loggingCallback)
        appGraph(navController, cropPhoto, logout)
    }
}


fun NavOptionsBuilder.removeFromBackStack() = popUpTo(0)

