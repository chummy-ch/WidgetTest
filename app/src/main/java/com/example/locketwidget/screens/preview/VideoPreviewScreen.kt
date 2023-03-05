package com.example.locketwidget.screens.preview

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
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
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.PreviewTopBar
import com.example.locketwidget.ui.VideoPlayer
import java.io.File

@Composable
fun VideoPreviewScreen(
    videoPath: String,
    navController: NavController
) {
    val viewModel: PreviewViewModel = mavericksViewModel(scope = LocalPhotoScope.current.apply { setScope(LocalLifecycleOwner.current) }) {
        CameraOutput.Video(videoPath)
    }

    val photoPathAsync = viewModel.collectAsState { it.cameraOutput }.value
    val event = viewModel.collectAsState { it.event }.value
    val pickedFriendsAsync = viewModel.collectAsState { it.friends }.value
    val context = LocalContext.current
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(event) {
        handlePreviewEvents(
            event = event,
            onSendingSuccess = {
                LocketAnalytics.logSuccessVideoSend()
                navController.popBackStack()
            }, onSendingFail = {
                LocketAnalytics.logVideoSendFail()
            }, onNavigate = { route ->
                if (route == null) navController.popBackStack()
            }
        )
    }

    BackHandler {
        viewModel.removePhoto()
    }

    val previewCallback = rememberPreviewCallback(
        send = viewModel::sendVideo,
        removePhoto = viewModel::removePhoto,
        downloadImage = viewModel::downloadVideo,
        permissionRequest = permissionRequest,
        context = context
    )
    var isPlaying by remember { mutableStateOf(false) }

    LocketScreen(
        topBar = {
            PreviewTopBar(
                event = event,
                friends = pickedFriendsAsync.invoke(),
                navController = navController,
                removePhoto = viewModel::removePhoto,
                screen = ScreenItem.VideoPreview
            )
        },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val source = remember {
                val mediaItem = MediaItem.fromUri(
                    File(videoPath).toUri()
                )
                val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }

            VideoPlayer(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 40.dp)
                    .size(LocalCameraSize.current.size),
                mediaSource = source,
                isPlaying = isPlaying,
                play = { isPlaying = true },
                pause = { isPlaying = false }
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
                            .height(dimensionResource(com.example.locketwidget.R.dimen.button_size)),
                        friendsCount = friendsCount
                    ) {
                        if (photoPathAsync.invoke() != null) navigateToPicker(navController)
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
}