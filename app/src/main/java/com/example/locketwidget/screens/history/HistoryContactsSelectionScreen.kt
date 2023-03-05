package com.example.locketwidget.screens.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.preview.GroupPickerScreen
import com.example.locketwidget.screens.preview.PickContactItem
import com.example.locketwidget.ui.*
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HistoryContactsSelectionScreen(navController: NavController, key: Long, popBackStack: () -> Unit) {
    val viewModel: HistoryListViewModel = mavericksActivityViewModel(keyFactory = { key.toString() })
    val friendsAsync = viewModel.collectAsState { it.friends }.value
    val groupAsync = viewModel.collectAsState { it.groups }.value
    val pagerState = rememberPagerState()
    LocketScreen(topBar = {
        TopBar(currentScreenItem = ScreenItem.HistoryContactsSelection)
        { popBackStack() }
    }) { paddingValues ->
        if (groupAsync is Success && friendsAsync is Success) {
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
                                        friendsAsync.invoke().forEach { friendModel ->
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
                                text = stringResource(R.string.apply)
                            ) {
                                viewModel.save()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}