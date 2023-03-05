package com.example.locketwidget.data

import android.os.Parcelable
import android.text.format.DateUtils
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.network.FileRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import kotlinx.parcelize.Parcelize
import java.util.*

data class HistoryResponse(
    val date: Timestamp = Timestamp(0L, 0),
    val photo: String = "",
    val sender: DocumentReference? = null,
    val video: String? = null,
    val live: String? = null,
    val emojis: List<String>? = null
) {
    fun mapToHistoryModel(): HistoryModel {
        return HistoryModel(
            date = this.date,
            photoLink = FileRepository.getPhotoLinkById(photo),
            photoId = this.photo,
            senderId = this.sender!!.id,
            mode = when {
                video != null -> HistoryMode.Video(this.video, FileRepository.getVideoLinkById(this.video))
                live != null -> HistoryMode.Live(FileRepository.getPhotoLinkById(this.live))
                emojis != null -> HistoryMode.Mood(emojis)
                else -> null
            }
        )
    }
}

@Parcelize
sealed class HistoryMode : Parcelable {
    data class Video(val id: String, val link: String) : HistoryMode()
    data class Live(val link: String, val isMain: Boolean = false) : HistoryMode()
    data class Mood(val emoji: List<String>) : HistoryMode()
}

@Parcelize
data class HistoryModel(
    val date: Timestamp,
    val photoLink: String,
    val photoId: String,
    val senderId: String,
    val mode: HistoryMode?
) : Parcelable, NavigationType {
    fun toJson(): String = LocketGson.gson.toJson(this)
}

fun Timestamp.getTimeSpanString(): String {
    val string = DateUtils.getRelativeTimeSpanString(
        this.toDate().time,
        Calendar.getInstance().timeInMillis,
        DateUtils.MINUTE_IN_MILLIS
    )
    return string.toString()
}