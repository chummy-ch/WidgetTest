package com.example.locketwidget.data

import android.os.Parcelable
import com.example.locketwidget.core.LocketGson
import kotlinx.parcelize.Parcelize

@Parcelize
data class FirestoreUserResponse(
    val id: String = "",
    val name: String,
    val photoLink: String?,
) : Parcelable {
    fun toJson() = LocketGson.gson.toJson(this)!!
}