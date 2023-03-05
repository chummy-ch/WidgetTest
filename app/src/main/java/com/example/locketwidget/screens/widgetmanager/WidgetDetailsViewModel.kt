package com.example.locketwidget.screens.widgetmanager

import com.airbnb.mvrx.*
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.LocketWidgetModel
import com.example.locketwidget.data.WidgetStyle
import com.example.locketwidget.local.LocalWidgetsRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.work.WidgetWorkUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class WidgetDetailsState(
    val widget: Async<LocketWidgetModel> = Uninitialized,
    val friends: Async<List<Pair<ContactResponse, Boolean>>> = Uninitialized,
    val style: List<WidgetStyle> = WidgetStyle.getList(),
    val event: Event? = null,
) : MavericksState {
    constructor(locketWidget: LocketWidgetModel) : this(widget = Success(locketWidget))
}

class WidgetDetailsViewModel(
    initState: WidgetDetailsState,
    private val firestoreDataSource: FirestoreDataSource,
    private val widgetsRepository: LocalWidgetsRepository,
    private val workerUseCase: WidgetWorkUseCase
) : MavericksViewModel<WidgetDetailsState>(initState) {
    init {
        viewModelScope.launch {
            val result = firestoreDataSource.getUserContacts(FirebaseAuth.getInstance().currentUser!!.uid)
            setState {
                if (result is Result.Success) {
                    val widget = withState(this@WidgetDetailsViewModel) { it.widget }.invoke()
                    if (widget == null) copy()
                    else {
                        val friends = result.data.map { contact ->
                            contact to if (widget.friends.isEmpty()) true
                            else widget.friends.any { it == contact.id }
                        }
                        copy(friends = Success(friends))
                    }
                } else copy(friends = Fail((result as Result.Error).exception))
            }
        }
    }

    fun selectWidgetStyle(style: WidgetStyle) {
        val widget = withState(this) { it.widget }.invoke() ?: return
        setState { copy(widget = Success(widget.copy(style = style))) }
    }

    fun changeSenderInfo(isShown: Boolean) {
        val widget = withState(this) { it.widget }.invoke() ?: return
        setState { copy(widget = Success(widget.copy(isSenderInfoShown = isShown))) }
    }

    fun save() {
        viewModelScope.launch {
            val widget = withState(this@WidgetDetailsViewModel) { it.widget.invoke() } ?: return@launch
            widgetsRepository.changeWidget(widget)
            workerUseCase.createScheduleWork()
            val event = Event.Navigate(null)
            setState { copy(event = event) }
        }
    }

    fun select(friendId: String, isSelected: Boolean) {
        val friends = withState(this) { it.friends }.invoke() ?: return
        val newFriendList = friends.map { pair ->
            if (pair.first.id != friendId) pair
            else {
                pair.first to isSelected
            }
        }
        val widget = withState(this) { it.widget.invoke() } ?: return
        val newFriendsIdList = newFriendList.filter { it.second }.map { it.first.id }
        setState { copy(friends = Success(newFriendList), widget = Success(widget.copy(friends = newFriendsIdList))) }
    }

    fun changeName(name: String) {
        val widget = withState(this) { it.widget }.invoke()?.copy(name = name) ?: return
        setState { copy(widget = Success(widget)) }
    }

    companion object : MavericksViewModelFactory<WidgetDetailsViewModel, WidgetDetailsState> {
        override fun create(viewModelContext: ViewModelContext, state: WidgetDetailsState): WidgetDetailsViewModel {
            val firestoreDataSource: FirestoreDataSource by viewModelContext.activity.inject()
            val widgetRep: LocalWidgetsRepository by viewModelContext.activity.inject()
            val worker: WidgetWorkUseCase by viewModelContext.activity.inject()
            return WidgetDetailsViewModel(state, firestoreDataSource, widgetRep, worker)
        }
    }
}