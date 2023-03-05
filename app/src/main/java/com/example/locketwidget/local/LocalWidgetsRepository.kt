package com.example.locketwidget.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.LocketWidgetModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalWidgetsRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val WIDGETS_KEY = stringSetPreferencesKey("widgets")
    }

    private var localWidgets: MutableList<LocketWidgetModel> = mutableListOf()

    fun getWidgetsFlow() = dataStore.data.map { pref ->
        pref[WIDGETS_KEY] ?: setOf()
    }

    suspend fun getWidgetByIdOrCreate(id: Int): LocketWidgetModel {
        val widgets = getWidgets()
        val widget = widgets.firstOrNull { it.id == id }
        return if (widget != null) {
            LocketAnalytics.setWidgetCount(widgets.size)
            widget
        } else {
            val tempWidget = LocketWidgetModel(id = id)
            addWidget(tempWidget)
            return tempWidget
        }
    }

    suspend fun getWidgets(): List<LocketWidgetModel> {
        if (localWidgets.isEmpty()) loadLocalWidgets()
        return localWidgets.toList()
    }

    suspend fun changeWidget(widget: LocketWidgetModel) {
        if (localWidgets.isEmpty()) loadLocalWidgets()

        localWidgets = localWidgets.map {
            if (it.id != widget.id) it
            else widget
        }.toMutableList()
        saveWidgets()
    }

    private suspend fun addWidget(widget: LocketWidgetModel) {
        if (localWidgets.isEmpty()) loadLocalWidgets()

        if (!localWidgets.any { widget.id == it.id }) {
            localWidgets.add(widget)
            saveWidgets()
        }

        LocketAnalytics.setWidgetCount(localWidgets.size)
    }

    suspend fun remove(id: Int) {
        if (localWidgets.isEmpty()) loadLocalWidgets()
        localWidgets = localWidgets.filter { it.id != id }.toMutableList()
        saveWidgets()
    }

    suspend fun remove(ids: IntArray) {
        if (localWidgets.isEmpty()) loadLocalWidgets()
        localWidgets = localWidgets.filter { widget ->
            ids.any { widget.id != it }
        }.toMutableList()
        saveWidgets()

        LocketAnalytics.setWidgetCount(localWidgets.size)
    }

    private suspend fun saveWidgets() {
        dataStore.edit { pref ->
            pref[WIDGETS_KEY] = localWidgets.map { it.toJson() }.toSet()
        }
    }

    private suspend fun loadLocalWidgets() {
        val stringSet = dataStore.data.first()[WIDGETS_KEY] ?: setOf()
        localWidgets = stringSet.map { LocketGson.widgetFromJson(it) }.toMutableList()
    }
}