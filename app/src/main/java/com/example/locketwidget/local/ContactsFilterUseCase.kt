package com.example.locketwidget.local

import androidx.compose.runtime.toMutableStateMap
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.screens.preview.PickFriendsModel

class ContactsFilterUseCase {
    fun deselectContacts(
        friends: List<PickFriendsModel>,
        groups: List<Pair<DataStoreGroupModel, Boolean>>,
        groupId: String
    ): List<PickFriendsModel> {
        val deselectedGroup = groups.first { it.first.id == groupId }.first
        val contactsToDeselect = mutableListOf<ContactResponse>()
        deselectedGroup.contacts.forEach { contact ->
            val isContactInSelectedGroups = groups.any {
                val user = it.first
                val isSelected = it.second
                user != deselectedGroup && isSelected && contact in user.contacts
            }
            if (!isContactInSelectedGroups) {
                contactsToDeselect.add(contact)
            }
        }
        return friends.map {
            if (it.friend in contactsToDeselect) it.copy(isSelected = false)
            else it
        }
    }

    fun selectContacts(
        friends: List<PickFriendsModel>,
        groups: List<Pair<DataStoreGroupModel, Boolean>>,
        groupId: String
    ): List<PickFriendsModel> {
        groups.forEach { group ->
            val user = group.first
            if (user.id == groupId) {
                return friends.map {
                    if (it.friend in user.contacts) it.copy(isSelected = true)
                    else it
                }
            }
        }
        return emptyList()
    }

    fun deselectGroups(
        friends: List<PickFriendsModel>,
        groups: List<Pair<DataStoreGroupModel, Boolean>>
    ): List<Pair<DataStoreGroupModel, Boolean>> {
        val newGroups = groups.toMutableStateMap()
        val selectedContacts = friends.filter { it.isSelected }.map { it.friend }
        groups.forEach { group ->
            if (!selectedContacts.containsAll(group.first.contacts)) {
                newGroups[group.first] = false
            }
        }
        return newGroups.toList()
    }

    fun selectGroups(
        friends: List<PickFriendsModel>,
        groups: List<Pair<DataStoreGroupModel, Boolean>>
    ): List<Pair<DataStoreGroupModel, Boolean>> {
        val newGroups = groups.toMutableStateMap()
        val selectedFriends = friends.filter { it.isSelected }.map { it.friend }
        groups.forEach { group ->
            val groupContacts = group.first.contacts
            groupContacts.forEach { friend ->
                if (selectedFriends.containsAll(group.first.contacts)) {
                    newGroups[group.first] = true
                }
            }
        }
        return newGroups.toList()
    }
}