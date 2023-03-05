package com.example.locketwidget.features.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.SubcomposeAsyncImage
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.HistoryPaging
import com.example.locketwidget.core.LocalTimelineScope
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.screens.history.HISTORY_DATE_PATTERN
import com.example.locketwidget.screens.history.HISTORY_SPAN_COUNT
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import java.util.*

@Composable
fun SelectMomentScreen(
    navController: NavController
) {
    val viewModel: MomentViewModel = mavericksViewModel(scope = LocalTimelineScope.current)
    val historyList = viewModel.collectAsState { it.historyFlow }.value
    val selectedPhotos = viewModel.collectAsState { it.selectedPhotos }.value
    val downloadedPhotos = viewModel.collectAsState { it.downloadedPhotos }.value
    val event = viewModel.collectAsState { it.event }.value

    LocketScreen(
        topBar = {
            TopBarWithCloseButton(screen = ScreenItem.SelectMoment) {
                navController.popBackStack()
            }
        },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        Column {
            MomentLazyGrid(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(start = dimensionResource(R.dimen.screen_elements_padding), end = 18.dp)
                    .weight(1f)
                    .fillMaxSize(),
                history = historyList.collectAsLazyPagingItems(),
                selectedPhotos = selectedPhotos,
                select = viewModel::selectPhoto,
                unselect = viewModel::unselectPhoto,
                navigateToPremiumScreen = { navController.navigate(ScreenItem.Premium.route) }
            )

            GradientButton(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = dimensionResource(R.dimen.screen_elements_padding))
                    .fillMaxWidth(),
                text = stringResource(R.string.photo_editor_save_button)
            ) {
                if (selectedPhotos.size < MomentViewModel.PHOTO_MIN_COUNT) {
                    viewModel.setPhotoNotEnoughEvent()
                } else {
                    val downloadedHistories = downloadedPhotos.map { it.first }
                    val isAllSelectedDownloaded = downloadedHistories.containsAll(selectedPhotos)

                    if (downloadedPhotos.size > selectedPhotos.size && isAllSelectedDownloaded) {
                        viewModel.filterDownloadedHistory()
                        navController.popBackStack()
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentLazyGrid(
    modifier: Modifier = Modifier,
    history: LazyPagingItems<HistoryPaging>,
    selectedPhotos: List<HistoryModel>,
    unselect: (HistoryModel) -> Unit,
    select: (HistoryModel) -> Unit,
    navigateToPremiumScreen: () -> Unit
) {
    val todayDate = remember { android.icu.text.SimpleDateFormat(HISTORY_DATE_PATTERN, Locale.US).format(Date()) }

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

                    Row(modifier = Modifier.fillMaxWidth()) {
                        it.forEach { history ->
                            val isSelected = selectedPhotos.any { it == history }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .aspectRatio(1f)
                            ) {
                                SelectablePhotoItem(
                                    modifier = Modifier
                                        .padding(top = 14.dp, end = 14.dp)
                                        .fillMaxSize(),
                                    photoUrl = history.photoLink,
                                    unselect = { unselect(history) },
                                    select = { select(history) },
                                    isSelected = isSelected
                                )
                            }
                        }

                        for (i in 0 until (HISTORY_SPAN_COUNT - it.size)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            } else if (historyPaging is HistoryPaging.NonPremiumButton) {
                GradientButton(
                    text = stringResource(R.string.history_show_more_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, end = 18.dp)
                ) {
                    navigateToPremiumScreen()
                }
            }
        }
    }
}

@Composable
fun SelectablePhotoItem(
    modifier: Modifier = Modifier,
    photoUrl: String,
    isSelected: Boolean,
    unselect: () -> Unit,
    select: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = modifier.run {
                if (isSelected) {
                    border(
                        width = dimensionResource(R.dimen.history_moment_border_width),
                        brush = Brush.verticalGradient(
                            listOf(
                                Orange200, Orange300
                            )
                        ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.image_corner_radius))
                    )
                } else {
                    this
                }
            }
        ) {
            SubcomposeAsyncImage(
                model = photoUrl,
                loading = {
                    ShimmerAnimation {
                        ShimmeringItem(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)), alpha = it
                        )
                    }
                },
                success = {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (isSelected) unselect()
                                else select()
                            }
                    )
                },
                modifier = Modifier.padding(6.dp),
                contentDescription = null
            )
        }

        if (isSelected) {
            Image(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .align(Alignment.TopEnd)
                    .background(brush = Brush.linearGradient(listOf(Orange200, Orange300))),
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null
            )
        }
    }
}