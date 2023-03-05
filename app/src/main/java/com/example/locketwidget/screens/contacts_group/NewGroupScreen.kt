package com.example.locketwidget.screens.contacts_group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.ui.*

@Composable
fun NewGroup(
    groupModel: DataStoreGroupModel?,
    navController: NavController
) {
    val viewModel: GroupViewModel = mavericksViewModel(argsFactory = { groupModel ?: DataStoreGroupModel() })
    val contacts = viewModel.collectAsState { it.contacts }.value
    val group = viewModel.collectAsState { it.groupModel }.value
    val event = viewModel.collectAsState { it.event }.value
    val shortNameError = stringResource(R.string.short_name_error)
    val message = remember { mutableStateOf<Event.ErrorMessage?>(null) }
    LaunchedEffect(event) {
        if (event is Event.Navigate) {
            navController.popBackStack()
            viewModel.clearEvent()
        }
    }

    LocketScreen(
        topBar = {
            TopBar(
                currentScreenItem = ScreenItem.NewGroup
            ) { navController.popBackStack() }
        },
        message = message.value
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding)
                )
        ) {
            val textId = if (groupModel == null) R.string.update_group else R.string.create_new_group
            Text(
                text = stringResource(id = textId),
                style = MaterialTheme.typography.h1.copy(color = MaterialTheme.colors.onPrimary),
                color = MaterialTheme.colors.onPrimary
            )

            if (group is Success) {
                EditNameTextField(
                    modifier = Modifier.padding(top = 24.dp),
                    hint = stringResource(id = R.string.group_name),
                    name = group.invoke().name,
                    changeName = viewModel::changeName
                )
            }
            if (contacts is Success) {
                FriendsList(
                    modifier = Modifier
                        .padding(top = 26.dp)
                        .weight(1f),
                    friends = contacts.invoke(),
                    title = R.string.select_friends,
                    select = viewModel::select
                )
            }

            GradientButton(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_size)),
                text = stringResource(R.string.save_button)
            ) {
                group.invoke()?.let {
                    if (it.name.length > 1) {
                        viewModel.save()
                    } else {
                        message.value = Event.ErrorMessage(shortNameError)
                    }
                }
            }
        }
    }
}