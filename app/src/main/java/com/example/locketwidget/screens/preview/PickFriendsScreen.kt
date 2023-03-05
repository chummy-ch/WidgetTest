package com.example.locketwidget.screens.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalPhotoScope
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState


@OptIn(com.google.accompanist.pager.ExperimentalPagerApi::class)
@Composable
fun FriendsPickScreen(popBackStack: () -> Unit) {
    val viewModel: PreviewViewModel = mavericksViewModel(LocalPhotoScope.current)
    val friendsAsync = viewModel.collectAsState { it.friends }.value
    val pagerState = rememberPagerState()

    LocketScreen(topBar = { TopBar(currentScreenItem = ScreenItem.FriendPicker) { popBackStack() } }) { paddingValues ->
        if (friendsAsync is Success) {
            val friends = friendsAsync.invoke()
            Column(modifier = Modifier.padding(paddingValues)) {
                ContactsBody(
                    firstTitle = R.string.pick_friends,
                    secondTitle = R.string.pick_group,
                    pagerState = pagerState
                ) {
                    HorizontalPager(
                        count = 2,
                        state = pagerState,
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .padding(top = 16.dp)
                        ) {
                            if (page == 0) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                                ) {
                                    LazyTriangleLayout {
                                        friends.forEach { friendModel ->
                                            PickContactItem(
                                                contact = friendModel,
                                                pick = { id, pick ->
                                                    viewModel.pickFriend(id, pick)
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                                GroupPickerScreen(
                                    modifier = Modifier.weight(1f),
                                    groupsResult = viewModel.collectAsState { it.groups }.value,
                                    select = viewModel::selectGroup
                                )
                            }
                            GradientButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimensionResource(R.dimen.screen_top_padding))
                                    .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding)),
                                text = stringResource(R.string.send_button_text)
                            ) {
                                viewModel.send()
                                popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPickerScreen(
    groupsResult: Async<List<Pair<DataStoreGroupModel, Boolean>>>,
    select: (String, Boolean) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (groupsResult is Success) {
            LazyColumn {
                items(groupsResult.invoke()) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable {
                                select(item.first.id, !item.second)
                            }
                            .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlayLayout {
                            if (item.first.contacts.isNotEmpty())
                                com.example.locketwidget.screens.contacts.FriendItem(url = item.first.contacts[0].photoUrl)
                            if (item.first.contacts.size > 1)
                                com.example.locketwidget.screens.contacts.FriendItem(url = item.first.contacts[1].photoUrl)
                        }
                        if (item.first.contacts.size > 2)
                            Text(
                                text = stringResource(R.string.plus, item.first.contacts.size - 2),
                                style = MaterialTheme.typography.h2.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.primary
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        Text(
                            text = item.first.name,
                            style = MaterialTheme.typography.h2.copy(
                                fontWeight = FontWeight.W400,
                                color = if (item.second) MaterialTheme.colors.primary else MaterialTheme.colors.onPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PickContactItem(
    modifier: Modifier = Modifier,
    contact: PickFriendsModel,
    pick: (String, Boolean) -> Unit
) {
    ConstraintLayout(
        modifier = modifier
            .width(90.dp)
    ) {
        val (imageRef, nameRef) = createRefs()
        val borderColor = if (contact.isSelected) Brush.linearGradient(colors = listOf(Orange200, Orange300))
        else Brush.linearGradient(colors = listOf(Color.White, Color.White))

        Box(modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .border(
                width = 3.dp,
                brush = borderColor,
                shape = CircleShape
            )
            .constrainAs(imageRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = contact.friend.photoUrl,
                    error = painterResource(id = R.drawable.default_avatar),
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .clickable { pick(contact.friend.id, !contact.isSelected) },
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = contact.friend.name,
            modifier = Modifier.constrainAs(nameRef) {
                top.linkTo(imageRef.bottom, 10.dp)
                bottom.linkTo(parent.bottom, 10.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            style = MaterialTheme.typography.h2,
            textAlign = TextAlign.Center
        )
    }
}