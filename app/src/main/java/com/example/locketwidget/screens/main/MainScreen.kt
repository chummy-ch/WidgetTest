package com.example.locketwidget.screens.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.*
import com.example.locketwidget.screens.camera.BestieCamera
import com.example.locketwidget.screens.camera.CameraCallback
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.LocketTopBar
import com.example.locketwidget.ui.ShimmerAnimation
import com.example.locketwidget.ui.ShimmeringItem
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    cropPhoto: (Uri) -> Unit,
    navController: NavController
) {
    val viewModel: LocketViewModel = mavericksViewModel()
    val event = viewModel.collectAsState { it.event }.value
    val activityViewModel: ActivityViewModel = mavericksActivityViewModel()
    val activityEvent = activityViewModel.collectAsState { it.event }.value
    val cameraOutput = viewModel.collectAsState { it.currentCameraOuput }.value
    val history = viewModel.collectAsState(LocketState::historyPhotos).value
    val context = LocalContext.current
    val modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    BackHandler {
        if (modalBottomSheetState.isVisible) {
            scope.launch {
                modalBottomSheetState.hide()
            }
        } else
            (context as Activity).finish()
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) cropPhoto(uri)
    }

    val cameraCapture = object : CameraCallback {
        override fun takePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean) {
            viewModel.takePhoto(context, imageCapture, isReversedHorizontal)
        }

        override fun pickPhotoFromGallery() {
            photoPicker.launch("image/*")
        }

        override fun finalizeVideoRecording(path: String) {
            viewModel.takeVideo(path)
        }

        override suspend fun takeLivePhoto(
            context: Context,
            imageCapture: ImageCapture,
            isReversedHorizontal: Boolean,
            onFinish: suspend () -> Unit
        ) {
            viewModel.takeLivePhoto(context, imageCapture, isReversedHorizontal, onFinish)
        }
    }

    LaunchedEffect(event) {
        if (event is Event.Navigate && cameraOutput is Success) {
            val output = cameraOutput.invoke()
            when (output) {
                is CameraOutput.Photo -> {
                    navigateTo(navController, ScreenItem.PhotoPreview, PhotoPreviewType(output.path))
                }
                is CameraOutput.Live -> {
                    if (output.secondaryPhotoPath != null) {
                        val json = LocketGson.gson.toJson(output)
                        val path = "${ScreenItem.LivePhotoPreview.route}?${ScreenItem.PREVIEW_ARG}=$json"
                        navController.navigate(path)
                    }
                }
                is CameraOutput.Video -> {
                    navController.navigate("${ScreenItem.VideoPreview.route}?${ScreenItem.PREVIEW_ARG}=${output.path}")
                }
                is CameraOutput.Mood -> {
                    navigateTo(navController, ScreenItem.PhotoPreview, PhotoPreviewType(output.image, output.emojis))
                }
            }
            viewModel.clearPhoto()
        }
    }

    LaunchedEffect(Unit) {
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            navController.navigate(ScreenItem.CameraPermission.route)
        }
    }

    LocketScreen(
        topBar = { LocketTopBar(navController = navController) },
        message = activityEvent?.mapToErrorMessage(),
        disposeEvent = activityViewModel::clearEvent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            HistoryCarousel(
                modifier = Modifier.padding(
                    top = 24.dp,
                ),
                historyAsync = history,
                navigateTo = { screenItem, arg ->
                    navigateTo(navController, screenItem, arg)
                },
            )

            BestieCamera(
                modifier = Modifier.fillMaxSize(),
                cameraCallback = cameraCapture,
                openBottomSheet = {
                    scope.launch { modalBottomSheetState.show() }
                },
                openAudioPermissionScreen = {
                    navController.navigate(ScreenItem.AudioPermission.route)
                }
            )
        }
    }

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetContent = {
            BottomSheetContent(cameraCapture, navController, modalBottomSheetState, scope)
        },
        sheetState = modalBottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = (Color.Black.copy(alpha = 0.15f)),
    ) {}
}

@Composable
fun HistoryCarousel(
    modifier: Modifier = Modifier,
    historyAsync: Async<List<HistoryModel>>,
    navigateTo: (ScreenItem, NavigationType) -> Unit
) {
    if (historyAsync is Success && historyAsync.invoke().isNotEmpty()) {
        LazyRow(modifier = modifier) {
            item {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding)))
            }
            items(historyAsync.invoke(), key = { it.photoLink + it.date }) {
                HistoryItem(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(dimensionResource(R.dimen.history_size)),
                    historyModel = it,
                    navigateTo = {
                        navigateTo(ScreenItem.History, it)
                    }
                )
            }
        }
    } else {
        LazyRow(modifier = modifier) {
            item {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding)))
            }
            items(10) {
                ShimmerAnimation {
                    ShimmeringItem(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(dimensionResource(R.dimen.history_size))
                            .clip(RoundedCornerShape(16.dp)),
                        alpha = it
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    historyModel: HistoryModel,
    navigateTo: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                navigateTo()
            },
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    Brush.horizontalGradient(colors = listOf(Orange200, Orange300)),
                    RoundedCornerShape(16.dp)
                ),
            painter = rememberAsyncImagePainter(historyModel.photoLink),
            contentDescription = "history",
            contentScale = ContentScale.Crop,
        )

        if (historyModel.mode is HistoryMode.Video) {
            Image(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(dimensionResource(R.dimen.history_carousel_play_button_size))
            )
        } else if (historyModel.mode is HistoryMode.Live) {
            Image(
                painter = rememberAsyncImagePainter(historyModel.mode.link),
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
}

private fun navigateTo(navController: NavController, screen: ScreenItem, arg: NavigationType) {
    val path = when (screen) {
        ScreenItem.History -> {
            val history = arg as HistoryModel
            ScreenItem.getRouteForHistory(history)
        }
        ScreenItem.PhotoPreview -> {
            val link = (arg as PhotoPreviewType).path
            val isPhoto = true
            val route = "${ScreenItem.PhotoPreview.route}?${ScreenItem.PREVIEW_ARG}=$link,${ScreenItem.PREVIEW_TYPE_ARG}=$isPhoto"
            if (arg.emojis == null) route + ",${ScreenItem.EMOJIS_ARG}=${null}"
            else route + ",${ScreenItem.EMOJIS_ARG}=${null}"
        }
        else -> throw IllegalAccessException()
    }
    navController.navigate(path)
}

@Composable
fun BottomSheetListItem(icon: Int, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick() })
            .padding(vertical = 19.dp)
            .padding(start = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.h3)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetContent(
    cameraCallback: CameraCallback,
    navController: NavController,
    sheetState: ModalBottomSheetState,
    scope: CoroutineScope
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(40.dp, 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
        )
        BottomSheetListItem(
            icon = R.drawable.ic_gallery_gradient,
            text = stringResource(R.string.add_photo_from_gallery),
            onClick = {
                scope.launch {
                    sheetState.hide()
                    cameraCallback.pickPhotoFromGallery()
                }
            })
        BottomSheetListItem(
            icon = R.drawable.ic_text_gradient,
            text = stringResource(R.string.create_image),
            onClick = {
                scope.launch {
                    sheetState.hide()
                    navController.navigate(ScreenItem.TextMoments.route)
                }
            })

        BottomSheetListItem(
            icon = R.drawable.ic_editor_gradient,
            text = stringResource(R.string.drawing_screen_title),
            onClick = {
                scope.launch {
                    sheetState.hide()
                    navController.navigate(ScreenItem.DrawingMoment.route)
                }
            })

        // TODO: Timeline
        /*BottomSheetListItem(
            icon = R.drawable.ic_camera_reel,
            text = stringResource(R.string.create_timeline),
            onClick = {
                scope.launch {
                    sheetState.hide()
                    navController.navigate(ScreenItem.TimeLineEditor.route)
                }
            })*/
        BottomSheetListItem(
            icon = R.drawable.ic_emoji,
            text = stringResource(R.string.share_mood_title),
            onClick = {
                scope.launch {
                    sheetState.hide()
                    navController.navigate(ScreenItem.ShareMood.route)
                }
            }
        )
    }
}