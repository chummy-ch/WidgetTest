package com.example.locketwidget.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactResponse(
    val id: String = "",
    val name: String = "",
    val photoUrl: String? = null
) : Parcelable
