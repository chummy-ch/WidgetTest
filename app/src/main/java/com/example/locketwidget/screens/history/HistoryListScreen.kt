package com.example.locketwidget.screens.history

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.HistoryPaging
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.HistoryMode
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.preview.PickFriendsModel
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import java.util.*

const val HISTORY_SPAN_COUNT = 3
const val HISTORY_DATE_PATTERN = "MMMM dd"

@Composable
fun HistoryListScreen(navController: NavController) {
    val currDate = rememberSaveable { Date().time }
    val viewModel: HistoryListViewModel = mavericksActivityViewModel(keyFactory = { currDate.toString() })
    val historyList = viewModel.collectAsState { it.historyFlow }.value.collectAsLazyPagingItems()
    val filteredContacts = viewModel.collectAsState { it.filteredContacts }
    val allFriends = viewModel.collectAsState { it.friends }

    LocketScreen(topBar = {
        HistoryListTopBar(
            iconOnclick = {
                navController.navigate("${ScreenItem.HistoryContactsSelection.route}/$currDate")
            },
            backButtonOnclick = { navController.popBackStack() },
            selectedFriendsState = filteredContacts,
            allFriendsState = allFriends
        )
    }) { paddingValues ->
        PhotosLazyGrid(
            history = historyList,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding),
                    bottom = dimensionResource(R.dimen.screen_elements_padding)
                ),
            navigate = { historyModel ->
                navigateToDetails(historyModel, navController)
            },
            navigateToPremiumScreen = {
                navController.navigate("${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}=2")
            }
        )
    }
}

@Composable
fun PhotosLazyGrid(
    modifier: Modifier = Modifier,
    history: LazyPagingItems<HistoryPaging>,
    navigate: (HistoryModel) -> Unit,
    navigateToPremiumScreen: () -> Unit
) {
    val todayDate = remember {
        SimpleDateFormat(HISTORY_DATE_PATTERN, Locale.US).format(Date())
    }

    LazyColumn(modifier = modifier) {
        items(items = history, key = { item -> item.toString() }) { historyPaging ->

            if (historyPaging is HistoryPaging.Page) {
                val text = if (historyPaging.day != todayDate) historyPaging.day else stringResource(R.string.gallery_today)
                Text(
                    text = text,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Medium),
                    lineHeight = 22.sp
                )

                historyPaging.history.chunked(HISTORY_SPAN_COUNT).forEach {

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        it.forEach { history ->
                            PhotoItem(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(dimensionResource(R.dimen.history_item_size)),
                                photoUrl = history.photoLink,
                                navigate = { navigate(history) },
                                mode = history.mode
                            )
                        }

                        for (i in 0 until (HISTORY_SPAN_COUNT - it.size)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(dimensionResource(R.dimen.history_item_size))
                            ) {

                            }
                        }
                    }
                }
            } else if (historyPaging is HistoryPaging.NonPremiumButton) {
                GradientButton(
                    text = stringResource(R.string.history_show_more_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    navigateToPremiumScreen()
                }
            }
        }
    }
}

@Composable
fun PhotoItem(
    modifier: Modifier = Modifier,
    photoUrl: String,
    mode: HistoryMode?,
    navigate: () -> Unit
) {
    SubcomposeAsyncImage(
        model = photoUrl,
        loading = {
            ShimmerAnimation {
                ShimmeringItem(modifier = Modifier.fillMaxSize(), alpha = it)
            }
        },
        success = {
            BoxWithConstraints {
                Image(
                    painter = this@SubcomposeAsyncImage.painter,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable { navigate() }
                        .fillMaxSize()
                )

                if (mode is HistoryMode.Video) {
                    Image(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(dimensionResource(R.dimen.history_carousel_play_button_size))
                    )
                } else if (mode is HistoryMode.Live) {
                    Image(
                        painter = rememberAsyncImagePainter(mode.link),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .size(maxWidth / integerResource(R.integer.live_photo_size_ratio))
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(colors = listOf(Orange200, Orange300)),
                                RoundedCornerShape(4.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Fit,
    )
}

private fun navigateToDetails(historyModel: HistoryModel, navController: NavController) {
    val path = "${ScreenItem.History.route}?${ScreenItem.HISTORY_ARG}=${historyModel.toJson()}"
    navController.navigate(path)
}

@Composable
private fun HistoryListTopBar(
    iconOnclick: () -> Unit,
    backButtonOnclick: () -> Unit,
    selectedFriendsState: State<Async<List<ContactResponse>>>,
    allFriendsState: State<Async<List<PickFriendsModel>>>
) {
    Box(
        Modifier.wrapContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = backButtonOnclick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = iconOnclick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_contacts_filter),
                        contentDescription = null
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(id = ScreenItem.HistoryList.title!!),
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding)),
                    style = MaterialTheme.typography.h1.copy(color = MaterialTheme.colors.onPrimary),
                    color = MaterialTheme.colors.onPrimary
                )
                val selectedFriends = selectedFriendsState.value
                if (selectedFriends is Success) {
                    val friends = if (selectedFriends.invoke().isEmpty()) {
                        allFriendsState.value.invoke()?.map { it.friend } ?: emptyList()
                    } else selectedFriends.invoke()
                    val listSize = friends.size
                    val modifier = Modifier
                        .size(dimensionResource(R.dimen.history_filter_contact_image_size))
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colors.background, CircleShape)
                    if (listSize <= 5) {
                        OverlayLayout(Modifier.padding(end = 32.dp)) {
                            friends.subList(0, listSize).forEach { contact ->
                                AsyncImage(
                                    model = contact.photoUrl,
                                    modifier = modifier,
                                    contentDescription = null
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 32.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OverlayLayout {
                                friends.subList(0, 4).forEach { contact ->
                                    AsyncImage(
                                        model = contact.photoUrl,
                                        modifier = modifier,
                                        contentDescription = null
                                    )
                                }
                            }
                            Text(
                                text = stringResource(id = R.string.plus, listSize - 4),
                                style = MaterialTheme.typography.h2.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.primary
                                ),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}