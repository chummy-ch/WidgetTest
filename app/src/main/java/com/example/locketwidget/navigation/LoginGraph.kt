package com.example.locketwidget.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.LoggingCallback
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.removeFromBackStack
import com.example.locketwidget.screens.AuthScreen
import com.example.locketwidget.screens.auth.UserSettingsScreen
import com.example.locketwidget.screens.onboarding.*

private fun NavGraphBuilder.addFriendsScreen(
    navController: NavController
) {
    composable(ScreenItem.OnboardingAddFriends.route) {
        OnboardingAddFriendsScreen {
            navController.navigate(ScreenItem.OnboardingAddWidget.route) { removeFromBackStack() }
        }
    }
}

private fun NavGraphBuilder.addWidgetScreen(
    navController: NavController
) {
    composable(ScreenItem.OnboardingAddWidget.route) {
        OnboardingAddWidgetScreen {
            navController.navigate(ScreenItem.OnboardingSendPhoto.route) {
                removeFromBackStack()
            }
        }
    }
}

private fun NavGraphBuilder.sendPhotoScreen(
    navController: NavController
) {
    composable(ScreenItem.OnboardingSendPhoto.route) {
        OnboardingSendPhotoScreen {
            navController.navigate(ScreenItem.OnboardingReceivePhoto.route) {
                removeFromBackStack()
            }
        }
    }
}

private fun NavGraphBuilder.receivePhotoScreen(
    navController: NavController
) {
    composable(ScreenItem.OnboardingReceivePhoto.route) {
        OnboardingReceivePhotoScreen {
            navController.navigate(ScreenItem.OnboardingKeepInTouch.route) {
                removeFromBackStack()
            }
        }
    }
}

private fun NavGraphBuilder.keepInTouchScreen(
    navController: NavController
) {
    composable(ScreenItem.OnboardingKeepInTouch.route) {
        OnboardingKeepInTouchScreen {
            navController.navigate(ScreenItem.Authentication.route) { removeFromBackStack() }
            LocketAnalytics.logOnboardingFinished()
        }
    }
}

private fun NavGraphBuilder.authScreen(
    navController: NavController,
    loggingCallback: LoggingCallback
) {
    composable(ScreenItem.Authentication.route) {
        AuthScreen(loggingCallback) { navController.navigate(ScreenItem.UserProfileCreation.route) }
    }
}

private fun NavGraphBuilder.userSettingsScreen() {
    composable(ScreenItem.UserProfileCreation.route) {
        UserSettingsScreen {}
    }
}

private fun NavGraphBuilder.cameraPermission(
    navController: NavController
) {
    composable(ScreenItem.CameraPermission.route) {
        CameraPermissionScreen {
            navController.popBackStack()
        }
    }
}

private fun NavGraphBuilder.audioPermissionScreen(
    navController: NavController
) {
    composable(ScreenItem.AudioPermission.route) {
        AudioPermissionScreen {
            navController.popBackStack()
        }
    }
}

fun NavGraphBuilder.loginGraph(
    navController: NavController,
    loggingCallback: LoggingCallback,
) {
    navigation(startDestination = ScreenItem.OnboardingAddFriends.route, route = ScreenItem.LOGIN_GRAPH) {
        addFriendsScreen(navController)

        addWidgetScreen(navController)

        sendPhotoScreen(navController)

        receivePhotoScreen(navController)

        keepInTouchScreen(navController)

        authScreen(navController, loggingCallback)

        userSettingsScreen()

        cameraPermission(navController)

        audioPermissionScreen(navController)
    }
}