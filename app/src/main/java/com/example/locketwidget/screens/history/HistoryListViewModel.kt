package com.example.locketwidget.screens.history

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.airbnb.mvrx.*
import com.example.locketwidget.core.HistoryPaging
import com.example.locketwidget.core.HistorySource
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.DataStoreGroupModels
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.local.ContactsFilterUseCase
import com.example.locketwidget.network.AdaptyRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.screens.contacts_group.GroupViewModel
import com.example.locketwidget.screens.preview.PickFriendsModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


data class HistoryListState(
    val historyFlow: Flow<PagingData<HistoryPaging>> = flowOf(),
    val filteredContacts: Async<List<ContactResponse>> = Uninitialized,
    val friends: Async<List<PickFriendsModel>> = Uninitialized,
    val groups: Async<List<Pair<DataStoreGroupModel, Boolean>>> = Uninitialized
) : MavericksState

class HistoryListViewModel(
    initialState: HistoryListState,
    private val firestoreDataSource: FirestoreDataSource,
    private val adaptyRepository: AdaptyRepository,
    private val dataStore: DataStore<Preferences>,
    private val contactsFilterUseCase: ContactsFilterUseCase
) : MavericksViewModel<HistoryListState>(initialState) {
    init {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid!!
            val senders = firestoreDataSource.getUserContactsFlow(userId).first().toMutableList()
            val userResult = firestoreDataSource.getUser(userId)
            if (userResult is Result.Success) {
                senders.add(ContactResponse(userResult.data.id, userResult.data.name, userResult.data.photoLink))
            }
            val groups = LocketGson.gson.fromJson(
                dataStore.data.first()[GroupViewModel.CONTACTS_GROUP_KEY],
                DataStoreGroupModels::class.java
            )?.groupModels ?: listOf()
            setState {
                copy(
                    friends = Success(senders.map { PickFriendsModel(it, false) }),
                    filteredContacts = Success(emptyList()),
                    groups = Success(groups.map { it to false })
                )
            }
            setHistoryFlow(emptyList())
        }
    }

    private suspend fun setHistoryFlow(senders: List<ContactResponse>) {
        val pageSize = FirestoreDataSource.HISTORY_LOAD_AMOUNT.toInt()
        val isPremium = adaptyRepository.isPremium()
        val flow = Pager(
            PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize / 2
            )
        ) {
            HistorySource(firestoreDataSource, isPremium, senders)
        }.flow.cachedIn(viewModelScope)
        setState { copy(historyFlow = flow) }
    }

    fun selectGroup(groupId: String, isSelected: Boolean) {
        val groups = withState(this@HistoryListViewModel) { it.groups }.invoke()
        val newGroups = groups?.map {
            if (it.first.id == groupId) it.first to isSelected
            else it
        }
        setState { copy(groups = Success(newGroups ?: listOf())) }
        filterContacts(groupId, isSelected)
    }

    private fun filterContacts(groupId: String, isSelected: Boolean) {
        val selectedGroups = withState(this@HistoryListViewModel) { it.groups }.invoke()
        val selectedFriends = withState(this@HistoryListViewModel) { it.friends }.invoke()
        if (selectedGroups != null && selectedFriends != null) {
            val newFriends = if (isSelected) {
                contactsFilterUseCase.selectContacts(selectedFriends, selectedGroups, groupId)
            } else {
                contactsFilterUseCase.deselectContacts(selectedFriends, selectedGroups, groupId)
            }
            setState { copy(friends = Success(newFriends)) }
        }
    }

    fun pickFriend(friendId: String, pick: Boolean) {
        withState { state ->
            viewModelScope.launch {
                val friend = state.friends.invoke()?.firstOrNull { it.friend.id == friendId } ?: return@launch
                val newFriendModel = friend.copy(isSelected = pick)
                val newSelectedFriends = state.friends.invoke()?.map {
                    if (it.friend.id == newFriendModel.friend.id) newFriendModel
                    else it
                } ?: return@launch
                setState { copy(friends = Success(newSelectedFriends)) }

                filterGroups(newSelectedFriends, pick)
            }
        }
    }

    private fun filterGroups(newSelectedFriends: List<PickFriendsModel>, pick: Boolean) {
        val selectedGroups = withState(this@HistoryListViewModel) { it.groups }.invoke()
        if (selectedGroups != null) {
            val newGroups = if (pick) {
                contactsFilterUseCase.selectGroups(newSelectedFriends, selectedGroups)
            } else {
                contactsFilterUseCase.deselectGroups(newSelectedFriends, selectedGroups)
            }
            setState { copy(groups = Success(newGroups)) }
        }
    }

    fun save() {
        viewModelScope.launch {
            val friends = withState(this@HistoryListViewModel) { it.friends }.invoke()
            if (friends != null) {
                val filteredContacts = friends.filter { it.isSelected }.map { it.friend }
                setHistoryFlow(filteredContacts)
                setState { copy(filteredContacts = Success(filteredContacts)) }
            }
        }
    }

    companion object : MavericksViewModelFactory<HistoryListViewModel, HistoryListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: HistoryListState
        ): HistoryListViewModel {
            val firestore: FirestoreDataSource by viewModelContext.activity.inject()
            val adapty: AdaptyRepository by viewModelContext.activity.inject()
            val dataStore: DataStore<Preferences> = viewModelContext.activity.applicationContext.dataStore
            val contactsFilterUseCase: ContactsFilterUseCase by viewModelContext.activity.inject()
            return HistoryListViewModel(state, firestore, adapty, dataStore, contactsFilterUseCase)
        }
    }
}