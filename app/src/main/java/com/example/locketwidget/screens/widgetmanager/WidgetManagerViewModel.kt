package com.example.locketwidget.screens.widgetmanager

import com.airbnb.mvrx.*
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.LocketWidgetModel
import com.example.locketwidget.local.LocalWidgetsRepository
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.work.WidgetWorkUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class WidgetManagerState(
    val widgets: Async<List<Pair<LocketWidgetModel, Async<String>>>> = Uninitialized,
    val event: Event? = null
) : MavericksState

class WidgetManagerViewModel(
    initState: WidgetManagerState,
    private val widgetsRepository: LocalWidgetsRepository,
    private val firestoreDataSource: FirestoreDataSource,
    private val workUseCase: WidgetWorkUseCase
) : MavericksViewModel<WidgetManagerState>(initState) {
    init {
        viewModelScope.launch {
            val widgetsFlow = widgetsRepository.getWidgetsFlow()
            widgetsFlow.collect { set ->
                LocketAnalytics.setWidgetCount(set.size)
                val widgets = set.map {
                    LocketGson.widgetFromJson(it)
                }
                val currentWidgets = withState(this@WidgetManagerViewModel) { it.widgets }.invoke()
                if (currentWidgets == null) {
                    loadWidgetPhoto(widgets)
                    val widgetsMap = widgets.map {
                        it to Loading<String>()
                    }
                    setState { copy(widgets = Success(widgetsMap)) }
                } else {
                    val oldWidgets = currentWidgets.filter { pair ->
                        widgets.any { it == pair.first }
                    }
                    val newWidgets = widgets.filterNot { widget ->
                        oldWidgets.any { it.first.id == widget.id }
                    }
                    val newWidgetState = oldWidgets.plus(newWidgets.map { it to Loading<String>() })
                    val widgetStateOrdered = widgets.map { model ->
                        newWidgetState.first { it.first == model }
                    }
                    setState { copy(widgets = Success(widgetStateOrdered)) }
                    loadWidgetPhoto(newWidgets)
                }
            }
        }
    }

    private fun loadWidgetPhoto(widgets: List<LocketWidgetModel>) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        widgets.forEach { widget ->
            viewModelScope.launch {
                val photoResult = firestoreDataSource.getLastPhotoFromSenders(userId, widget.friends)
                if (photoResult is Result.Success) {
                    withState { state ->
                        val currentWidgets = state.widgets.invoke() ?: return@withState
                        val newWidgetList = currentWidgets.map {
                            if (it.first.id != widget.id) it
                            else {
                                widget to Success<String>(FileRepository.getPhotoLinkById(photoResult.data.photo))
                            }
                        }
                        setState { copy(widgets = Success(newWidgetList)) }
                    }
                }
            }
        }
    }

    fun remove(id: Int) {
        workUseCase.createRemoveWidgetIdWork(intArrayOf(id))
    }

    fun clearEvent() = setState { copy(event = null) }

    fun setWidgetAddedEvent(isSuccess: Boolean) {
        if (!isSuccess) {
            val event = Event.Message(R.string.fail_widget_added)
            setState { copy(event = event) }
        }
    }

    companion object : MavericksViewModelFactory<WidgetManagerViewModel, WidgetManagerState> {

        override fun create(viewModelContext: ViewModelContext, state: WidgetManagerState): WidgetManagerViewModel {
            val widgetsRepository: LocalWidgetsRepository by viewModelContext.activity.inject()
            val firestore: FirestoreDataSource by viewModelContext.activity.inject()
            val widgetWorkUseCase: WidgetWorkUseCase by viewModelContext.activity.inject()
            return WidgetManagerViewModel(
                initState = state,
                widgetsRepository = widgetsRepository,
                firestoreDataSource = firestore,
                workUseCase = widgetWorkUseCase
            )
        }
    }
}
