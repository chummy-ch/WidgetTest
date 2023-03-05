package com.example.locketwidget.core

import com.example.locketwidget.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class LocketGson {
    companion object {
        private val factory = RuntimeTypeAdapterFactory.of(WidgetStyle::class.java, "widget_style").apply {
            registerSubtype(WidgetForeground::class.java, "foreground")
            registerSubtype(WidgetShape::class.java, "shape")
        }
        private val historyModeFactory = RuntimeTypeAdapterFactory.of(HistoryMode::class.java, "history_mode").apply {
            registerSubtype(HistoryMode.Video::class.java, "video")
            registerSubtype(HistoryMode.Live::class.java, "live")
            registerSubtype(HistoryMode.Mood::class.java, "mood")
        }
        val gson: Gson by lazy { GsonBuilder().registerTypeAdapterFactory(factory).registerTypeAdapterFactory(historyModeFactory).create() }

        @Suppress("SENSELESS_COMPARISON")
        fun widgetFromJson(json: String): LocketWidgetModel {
            return if (json.contains("isSenderInfoShown")) {
                val widget = gson.fromJson(json, LocketWidgetModel::class.java)
                if (widget.style is WidgetShape && widget.style.stroke == null) {
                    return widget.copy(style = WidgetShape(WidgetShapes.Rectangle, false))
                }
                widget
            } else {
                if (json.contains("style")) gson.fromJson(json, NoUserInfoLocketWidgetModel::class.java).migrateToLocketWidget()
                else Gson().fromJson(json, NoStyleLocketWidgetModel::class.java).migrateToLocketWidget()
            }
        }
    }
}