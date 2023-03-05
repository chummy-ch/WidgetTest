package com.example.locketwidget.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.locketwidget.R
import com.example.locketwidget.data.*
import com.example.locketwidget.screens.preview.PickFriendsModel

@Composable
fun TopBar(
    currentScreenItem: ScreenItem,
    popBackStack: () -> Unit
) {
    if (currentScreenItem !is NoTopBar) {
        Column(modifier = Modifier.fillMaxWidth()) {

            if (currentScreenItem !is CrossButton)
                TopBarWithBackButton(popBackStack = popBackStack)
            else
                TopBarWithCrossButton(popBackStack = popBackStack)

            if (currentScreenItem !is NoTitle) {
                ScreenHeader(
                    text = stringResource(currentScreenItem.title!!),
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding))
                )
            }
        }
    }
}

@Composable
fun PreviewTopBar(
    event: Event?,
    friends: List<PickFriendsModel>?,
    navController: NavController,
    screen: ScreenItem,
    removePhoto: () -> Unit
) {
    if (event is Event.PhotoSending && event.status == SendingStatus.Sending) {
        val contacts = if (friends != null && friends.any { it.isSelected }) {
            friends.mapNotNull { pickedFriendsModel ->
                if (pickedFriendsModel.isSelected) pickedFriendsModel.friend
                else null
            }
        } else {
            friends?.map { it.friend }
        }
        PreviewTopBar(
            modifier = Modifier.padding(bottom = 24.dp),
            contacts = contacts,
            navController = navController,
        )
    } else {
        TopBar(currentScreenItem = screen) {
            removePhoto()
        }
    }
}

@Composable
private fun PreviewTopBar(
    modifier: Modifier = Modifier,
    contacts: List<ContactResponse>?,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = dimensionResource(R.dimen.screen_elements_padding))
    ) {
        TopBarWithBackButton { navController.popBackStack() }
        ScreenHeader(
            text = stringResource(R.string.preview_screen_sending_title),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding))
        )
        if (contacts != null) {
            SendingFriendsRow(
                contacts = contacts,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding)),
            )
        }
    }
}

@Composable
fun SendingFriendsRow(
    modifier: Modifier = Modifier,
    contacts: List<ContactResponse>
) {
    OverlayLayout(modifier = modifier) {
        contacts.forEach { contact ->
            ContactImageWithBorder(
                modifier = Modifier.size(46.dp),
                painter = rememberAsyncImagePainter(contact.photoUrl),
                borderBrush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colors.background,
                        MaterialTheme.colors.background
                    )
                ),
                borderPadding = 0.dp,
                borderSize = 4.dp
            )
        }
    }
}

@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.h1.copy(color = MaterialTheme.colors.onPrimary),
        color = MaterialTheme.colors.onPrimary
    )
}

@Composable
fun TopBarWithCloseButton(
    screen: ScreenItem,
    close: () -> Unit
) {
    require(screen.title != null) { "Screen item title must not be null" }

    Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding))) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { close() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_remove),
                    contentDescription = null
                )
            }
        }

        Text(text = stringResource(screen.title), style = MaterialTheme.typography.h1.copy(fontWeight = FontWeight.W700))
    }
}

@Composable
fun TopBarWithBackButton(
    popBackStack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = { popBackStack() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = null
            )
        }
    }
}

@Composable
fun TopBarWithCrossButton(
    popBackStack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { popBackStack() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_big_remove),
                contentDescription = null
            )
        }
    }
}

@Composable
fun LocketTopBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = { navController.navigate(ScreenItem.Contacts.route) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = null
            )
        }

        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { navController.navigate(ScreenItem.Settings.route) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null
            )
        }
    }
}
