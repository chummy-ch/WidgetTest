package com.example.locketwidget.core

import com.example.locketwidget.data.*
import com.example.locketwidget.widget.UserInfo

interface WidgetWorker {

    suspend fun proceedWidgetUpdate(
        history: HistoryResponse,
        widget: LocketWidgetModel,
        getUser: suspend (String) -> Result<FirestoreUserResponse>,
        updateWidget: suspend (history: HistoryModel, id: Int, style: WidgetStyle, userInfo: UserInfo?) -> Unit
    ) {
        val sender = history.sender

        if (sender != null && (widget.isSenderInfoShown || history.emojis != null)) {
            val senderResult = getUser(sender.id)
            if (senderResult is Result.Success && senderResult.data.photoLink != null) {
                updateWidget(
                    history.mapToHistoryModel(),
                    widget.id,
                    widget.style,
                    UserInfo(senderResult.data.photoLink, senderResult.data.name)
                )
                return
            }
        }
        updateWidget(history.mapToHistoryModel(), widget.id, widget.style, null)
    }
}