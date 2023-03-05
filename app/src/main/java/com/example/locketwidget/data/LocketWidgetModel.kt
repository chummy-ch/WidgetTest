package com.example.locketwidget.data

import android.os.Parcelable
import com.example.locketwidget.core.LocketGson
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class LocketWidgetModel(
    val id: Int,
    val name: String = "Bestie",
    val style: @RawValue WidgetStyle = WidgetShape(WidgetShapes.Rectangle, false),
    val friends: List<String> = listOf(),
    val isSenderInfoShown: Boolean = true
) : Parcelable {
    fun toJson(): String = LocketGson.gson.toJson(this, LocketWidgetModel::class.java)
}

data class NoUserInfoLocketWidgetModel(
    val id: Int,
    val name: String = "Bestie",
    val style: WidgetStyle = WidgetShape(WidgetShapes.Rectangle, false),
    val friends: List<String> = listOf()
) {
    fun migrateToLocketWidget(): LocketWidgetModel {
        return LocketWidgetModel(
            id = id,
            name = name,
            style = style,
            friends = friends,
            isSenderInfoShown = true
        )
    }
}

data class NoStyleLocketWidgetModel(
    val id: Int,
    val name: String = "Bestie",
    val shape: WidgetShapes = WidgetShapes.Rectangle,
    val friends: List<String> = listOf()
) {
    fun migrateToLocketWidget(): LocketWidgetModel {
        return LocketWidgetModel(
            id = id,
            name = name,
            style = WidgetShape(shape, false),
            friends = friends,
            isSenderInfoShown = true
        )
    }
}