package com.example.locketwidget.data

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.util.*

sealed class SendingStatus {
    object Sending : SendingStatus()
    object Success : SendingStatus()
    data class Fail(val error: Throwable) : SendingStatus()
}

sealed class Event {
    data class Share<T>(val data: T) : Event()
    data class Navigate(val route: String?, val timestamp: Long = Date().time) : Event()
    data class PhotoSending(val status: SendingStatus, val timestamp: Long = Date().time) : Event()
    data class ConnectionCreated(
        val user: FirestoreUserResponse,
        val friend: FirestoreUserResponse,
        val isInvitationSender: Boolean
    ) : Event()

    data class ErrorMessage(val message: String, val timestamp: Long = Date().time) : Event()
    data class Message(@StringRes val message: Int, val timestamp: Long = Date().time) : Event()
    object AppReview : Event()
    data class Reaction(val emoji: String, val timestamp: Long = Date().time) : Event()
}

fun Event.handleEvent(
    onShare: (Any?) -> Unit = {},
    onNavigate: (String?) -> Unit = {},
    onPhotoSending: (status: SendingStatus) -> Unit = {},
    onConnectionCreated: (user: FirestoreUserResponse, friend: FirestoreUserResponse, isInvitationSender: Boolean) -> Unit = { _, _, _ -> },
    onErrorMessage: (String) -> Unit = {},
    onMessage: (message: Int) -> Unit = {},
    onAppReview: () -> Unit = {},
    onReaction: (emoji: String) -> Unit = {}
) {
    when (this) {
        is Event.Share<*> -> onShare(this.data)
        is Event.Navigate -> onNavigate(this.route)
        is Event.PhotoSending -> onPhotoSending(this.status)
        is Event.ConnectionCreated -> onConnectionCreated(this.user, this.friend, this.isInvitationSender)
        is Event.ErrorMessage -> onErrorMessage(this.message)
        is Event.Message -> onMessage(this.message)
        is Event.AppReview -> onAppReview()
        is Event.Reaction -> onReaction(this.emoji)
    }
}

@Composable
fun Event.mapToErrorMessage(): Event.ErrorMessage? {
    return (this as? Event.Message)?.run { Event.ErrorMessage(stringResource(message), timestamp) }
}