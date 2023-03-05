package com.example.locketwidget.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DataStoreGroupModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "NEW GROUP",
    val contacts: List<ContactResponse> = listOf()
) : Parcelable

data class DataStoreGroupModels(
    val groupModels: List<DataStoreGroupModel>
)