package com.example.locketwidget.screens.preview

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocalPhotoScope
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.SendingStatus
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.screens.camera.PreviewCallback
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.PreviewTopBar

@Composable
fun LivePhotoPreviewScreen(liveOutput: CameraOutput.Live, navController: NavController) {

    val viewModel: PreviewViewModel = mavericksViewModel(
        LocalPhotoScope.current.apply { setScope(LocalLifecycleOwner.current) }
    ) { liveOutput }

    BackHandler {
        viewModel.removePhoto()
    }

    val output = viewModel.collectAsState { it.cameraOutput }.value
    val event = viewModel.collectAsState { it.event }.value
    val pickedFriendsAsync = viewModel.collectAsState { it.friends }.value
    val context = LocalContext.current
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(event) {
        handlePreviewEvents(
            event,
            onSendingFail = { LocketAnalytics.logFailSendPhoto(it.error.message) },
            onSendingSuccess = {
                LocketAnalytics.logLivePhotoSend()
                navController.popBackStack()
            },
            onNavigate = { route -> if (route == null) navController.popBackStack() },
        )
    }

    val previewCallback = rememberPreviewCallback(
        send = viewModel::sendLivePhoto,
        removePhoto = viewModel::removePhoto,
        downloadImage = viewModel::downloadLivePhoto,
        permissionRequest = permissionRequest,
        context = context
    )

    LocketScreen(
        topBar = {
            PreviewTopBar(
                event = event,
                friends = pickedFriendsAsync.invoke(),
                navController = navController,
                removePhoto = viewModel::removePhoto,
                screen = ScreenItem.LivePhotoPreview
            )
        },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        val live = output.invoke() as? CameraOutput.Live
        if (live != null) {
            PreviewScreenBody(
                paddingValues = paddingValues,
                event = event,
                output = live,
                pickedFriendsAsync = pickedFriendsAsync,
                navController = navController,
                previewCallback = previewCallback,
                changeMainPhoto = viewModel::changeLiveMainPhoto
            )
        }
    }
}

@Composable
private fun PreviewScreenBody(
    paddingValues: PaddingValues,
    event: Event?,
    output: CameraOutput.Live,
    pickedFriendsAsync: Async<List<PickFriendsModel>>,
    navController: NavController,
    previewCallback: PreviewCallback,
    changeMainPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    ) {

        Spacer(modifier = Modifier.weight(1f))

        LivePhoto(
            output = output,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(LocalCameraSize.current.size),
            changeMainPhoto = changeMainPhoto
        )

        Spacer(modifier = Modifier.weight(1f))

        if (event is Event.PhotoSending && event.status == SendingStatus.Sending) {
            SendingAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 62.dp, end = 62.dp, top = 12.dp, bottom = 58.dp),
                rawRes = R.raw.loading
            )
        } else {
            val friendsCount = when (pickedFriendsAsync) {
                is Success -> {
                    val contacts = pickedFriendsAsync.invoke()
                    val selectedContacts = contacts.filter { it.isSelected }
                    if (selectedContacts.isEmpty()) contacts.size
                    else selectedContacts.size
                }
                else -> 0
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        start = dimensionResource(com.example.locketwidget.R.dimen.screen_elements_padding),
                        end = dimensionResource(com.example.locketwidget.R.dimen.screen_elements_padding),
                        top = 40.dp,
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                PickFriendsButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.button_size)),
                    friendsCount = friendsCount
                ) {
                    navigateToPicker(navController)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PreviewButtons(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .align(Alignment.CenterHorizontally),
                previewCallback = previewCallback
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun LivePhoto(
    modifier: Modifier = Modifier,
    output: CameraOutput.Live,
    changeMainPhoto: () -> Unit
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .border(6.dp, color = Color.LightGray, RoundedCornerShape(20.dp)),
            painter = rememberAsyncImagePainter(output.mainPhotoPath),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        Image(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.TopEnd)
                .size(LocalCameraSize.current.size / integerResource(R.integer.live_photo_size_ratio))
                .clip(RoundedCornerShape(16.dp))
                .clickable { changeMainPhoto() },
            painter = rememberAsyncImagePainter(output.secondaryPhotoPath),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}