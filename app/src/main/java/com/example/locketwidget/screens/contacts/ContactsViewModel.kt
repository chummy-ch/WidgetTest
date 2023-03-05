package com.example.locketwidget.screens.contacts

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.airbnb.mvrx.*
import com.example.locketwidget.DeepLinkUseCase
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.DataStoreGroupModels
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.network.FirebaseFunctionsManager
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.screens.contacts_group.GroupViewModel.Companion.CONTACTS_GROUP_KEY
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class ContactsState(
    val user: FirebaseUser = FirebaseAuth.getInstance().currentUser!!,
    val contacts: Async<List<ContactResponse>> = Uninitialized,
    val groups: Async<List<DataStoreGroupModel>> = Uninitialized,
    val link: Async<Uri> = Uninitialized
) : MavericksState

class ContactsViewModel(
    initialState: ContactsState,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseFunctions: FirebaseFunctionsManager,
    private val dataStore: DataStore<Preferences>,
    private val deepLinkUseCase: DeepLinkUseCase
) : MavericksViewModel<ContactsState>(initialState) {
    init {
        viewModelScope.launch {
            val contactsFlow = firestoreDataSource.getUserContactsFlow(FirebaseAuth.getInstance().currentUser!!.uid)
            contactsFlow.collectIndexed { _, value ->
                setState { copy(contacts = Success(value)) }
                LocketAnalytics.setFriendsCount(value.size)
            }
        }
        setGroups()
    }

    fun clearDeepLink() = setState { copy(link = Uninitialized) }

    fun createDeepLink() {
        viewModelScope.launch {
            val userId = withState(this@ContactsViewModel) { it.user.uid }
            val link = deepLinkUseCase.createLink(userId)
            setState {
                if (link != null) copy(link = Success(link))
                else copy(link = Fail(NullPointerException("Deeplink is null")))
            }
        }
    }

    fun removeConnection(id: String) {
        viewModelScope.launch {
            firebaseFunctions.removeConnection(id)
        }
    }

    private fun setGroups() {
        viewModelScope.launch {
            dataStore.data.collect { pref ->
                val groups = LocketGson.gson.fromJson(pref[CONTACTS_GROUP_KEY], DataStoreGroupModels::class.java)
                setState {
                    copy(groups = Success(groups?.groupModels ?: listOf()))
                }
            }
        }
    }

    companion object : MavericksViewModelFactory<ContactsViewModel, ContactsState> {
        override fun create(viewModelContext: ViewModelContext, state: ContactsState): ContactsViewModel {
            val firestoreDataSource: FirestoreDataSource by viewModelContext.activity.inject()
            val firebaseFunctions: FirebaseFunctionsManager by viewModelContext.activity.inject()
            val dataStore: DataStore<Preferences> = viewModelContext.activity.applicationContext.dataStore
            val deepLink: DeepLinkUseCase by viewModelContext.activity.inject()
            return ContactsViewModel(state, firestoreDataSource, firebaseFunctions, dataStore, deepLink)
        }
    }
}
