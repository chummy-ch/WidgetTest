package com.example.locketwidget.screens.contacts_group

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.airbnb.mvrx.*
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.DataStoreGroupModels
import com.example.locketwidget.data.Event
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.network.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class GroupState(
    val event: Event? = null,
    val groupModel: Async<DataStoreGroupModel> = Uninitialized,
    val contacts: Async<List<Pair<ContactResponse, Boolean>>> = Uninitialized,
) : MavericksState {
    constructor(group: DataStoreGroupModel) : this(groupModel = Success(group))
}

class GroupViewModel(
    initialState: GroupState,
    private val dataStore: DataStore<Preferences>,
    private val firestoreDataSource: FirestoreDataSource
) : MavericksViewModel<GroupState>(initialState) {
    init {
        viewModelScope.launch {
            val contacts = firestoreDataSource.getUserContacts(FirebaseAuth.getInstance().currentUser!!.uid)
            val resultContacts = mutableListOf<Pair<ContactResponse, Boolean>>()
            val group = withState(this@GroupViewModel) { it.groupModel }.invoke()
            if (contacts is Result.Success && group != null) {
                if (group.contacts.isEmpty())
                    contacts.data.forEach { resultContacts.add((it to true)) }
                else
                    contacts.data.forEach { resultContacts.add(it to (it in group.contacts)) }
                setState { copy(contacts = Success(resultContacts)) }
            }
        }
    }

    fun changeName(name: String) {
        val group = withState(this) { it.groupModel }.invoke()?.copy(name = name) ?: return
        setState { copy(groupModel = Success(group)) }
    }

    fun select(id: String, isSelected: Boolean) {
        val contacts = withState(this) { it.contacts }.invoke() ?: return
        val newContacts = contacts.map { pair ->
            if (pair.first.id != id) pair
            else pair.first to isSelected
        }
        setState { copy(contacts = Success(newContacts)) }
    }

    fun clearEvent() {
        setState { copy(event = null) }
    }

    fun save() {
        viewModelScope.launch {
            val gson = dataStore.data.first()[CONTACTS_GROUP_KEY]
            val contacts = withState(this@GroupViewModel) { it.contacts }.invoke()
            var newGroup = withState(this@GroupViewModel) { it.groupModel }.invoke()
            if (contacts != null && newGroup != null) {
                val selectedContacts = contacts.filter { it.second }.map { it.first }
                newGroup = newGroup.copy(contacts = selectedContacts.ifEmpty { contacts.map { it.first } })
                saveGroupsToDataStore(gson, newGroup)
            }
            val event = Event.Navigate(null)
            setState { copy(event = event) }
        }
    }

    private suspend fun saveGroupsToDataStore(gson: String?, newGroup: DataStoreGroupModel) {
        if (gson != null) {
            val groups = LocketGson.gson.fromJson(gson, DataStoreGroupModels::class.java).groupModels.toMutableList()
            var isNewGroup = true
            val newGroups = groups.map {
                if (it.id == newGroup.id) {
                    isNewGroup = false
                    newGroup
                } else it
            }.toMutableList()
            if (isNewGroup) newGroups.add(newGroup)
            dataStore.edit {
                it[CONTACTS_GROUP_KEY] = LocketGson.gson.toJson(DataStoreGroupModels(newGroups))
            }
        } else {
            dataStore.edit {
                it[CONTACTS_GROUP_KEY] = LocketGson.gson.toJson(DataStoreGroupModels(listOf(newGroup)))
            }
        }
    }

    companion object : MavericksViewModelFactory<GroupViewModel, GroupState> {
        val CONTACTS_GROUP_KEY = stringPreferencesKey("contacts_group_key")
        override fun create(viewModelContext: ViewModelContext, state: GroupState): GroupViewModel {
            val dataStore: DataStore<Preferences> = viewModelContext.activity.applicationContext.dataStore
            val firestoreDataSource: FirestoreDataSource by viewModelContext.activity.inject()
            return GroupViewModel(state, dataStore, firestoreDataSource)
        }
    }
}