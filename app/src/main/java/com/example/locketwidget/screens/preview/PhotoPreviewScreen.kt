package com.example.locketwidget.screens.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.*
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocalPhotoScope
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.*
import com.example.locketwidget.local.WidgetTextModel
import com.example.locketwidget.screens.camera.PreviewCallback
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.getEditTextDefaultColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PhotoPreviewScreen(
    photoPath: String,
    isPhoto: Boolean,
    emojis: List<String>?,
    navController: NavController
) {
    val viewModel: PreviewViewModel = mavericksViewModel(
        LocalPhotoScope.current.apply { setScope(LocalLifecycleOwner.current) }
    ) { if (emojis == null) CameraOutput.Photo(photoPath) else CameraOutput.Mood(photoPath, emojis) }

    BackHandler {
        navController.clearBackStack(ScreenItem.TextMoments.route)
        viewModel.removePhoto()
    }

    val photoPathAsync = viewModel.collectAsState { it.cameraOutput }.value
    val updatedPhotoPathAsync = viewModel.collectAsState { it.drawedPhotoPath }.value
    val event = viewModel.collectAsState { it.event }.value
    val pickedFriendsAsync = viewModel.collectAsState { it.friends }.value
    val widgetTextModel = viewModel.collectAsState { it.widgetTextModel }.value
    val context = LocalContext.current
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(event) {
        handlePreviewEvents(
            event,
            onSendingFail = { LocketAnalytics.logFailSendPhoto(it.error.message) },
            onSendingSuccess = {
                if (isPhoto) {
                    LocketAnalytics.logSendPhoto()
                    navController.popBackStack()
                } else {
                    LocketAnalytics.logSendTextMoment()
                    navController.popBackStack()
                }
            },
            onNavigate = { route -> if (route == null) navController.popBackStack() },
        )
    }

    val previewCallback = rememberPreviewCallback(
        send = viewModel::send,
        removePhoto = viewModel::removePhoto,
        downloadImage = viewModel::downloadImage,
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
                screen = ScreenItem.PhotoPreview
            )
        },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        val path = if (updatedPhotoPathAsync is Success) {
            updatedPhotoPathAsync.invoke()
        } else {
            val output = photoPathAsync.invoke()
            (output as? CameraOutput.Photo)?.path ?: (output as? CameraOutput.Mood?)?.image
        }
        PreviewScreenBody(
            paddingValues = paddingValues,
            event = event,
            photoPath = path,
            pickedFriendsAsync = pickedFriendsAsync,
            navController = navController,
            widgetTextModel = widgetTextModel,
            viewModel = viewModel,
            isPhoto = isPhoto,
            previewCallback = previewCallback
        )
    }
}

@Composable
private fun PreviewScreenBody(
    paddingValues: PaddingValues,
    event: Event?,
    photoPath: String?,
    pickedFriendsAsync: Async<List<PickFriendsModel>>,
    navController: NavController,
    widgetTextModel: WidgetTextModel,
    viewModel: PreviewViewModel,
    isPhoto: Boolean,
    previewCallback: PreviewCallback
) {
    val pxSize = LocalDensity.current.run {
        LocalCameraSize.current.size.toPx()
    }

    LaunchedEffect(pxSize) {
        viewModel.setPreviewSize(pxSize)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    ) {

        Spacer(modifier = Modifier.weight(1f))

        PhotoPreviewBody(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 40.dp),
            photoPath = photoPath,
            widgetTextModel = widgetTextModel,
            changeText = viewModel::changeText,
            changeOffsetY = {
                if (event !is Event.PhotoSending || event.status != SendingStatus.Sending) {
                    viewModel.changeOffset(it.roundToInt())
                }
            },
            event = event,
            changeRectSize = viewModel::changeRectSize
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
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding),
                        top = 40.dp,
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isPhoto) {
                    RoundedCornerButton(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.button_size)),
                        drawableRes = if (widgetTextModel.isActive) R.drawable.ic_text_gradient else R.drawable.ic_text,
                    ) {
                        if (!widgetTextModel.isActive) {
                            viewModel.enableTextField()
                        } else {
                            viewModel.disableTextField()
                        }
                    }

                    RoundedCornerButton(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.button_size)),
                        drawableRes = R.drawable.ic_edit,
                    ) {
                        navController.navigate(ScreenItem.Drawing.route)
                    }
                }

                PickFriendsButton(
                    modifier = Modifier
                        .height(dimensionResource(R.dimen.button_size)), friendsCount = friendsCount
                ) {
                    if (photoPath != null) navigateToPicker(navController)
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
private fun PhotoPreviewBody(
    modifier: Modifier = Modifier,
    event: Event?,
    photoPath: String?,
    widgetTextModel: WidgetTextModel,
    changeText: (String) -> Unit,
    changeOffsetY: (Float) -> Unit,
    changeRectSize: (Int) -> Unit
) {
    val focusRequest = remember { FocusRequester() }
    val previewSize = LocalCameraSize.current.size

    LaunchedEffect(widgetTextModel.isActive) {
        if (widgetTextModel.isActive) {
            when {
                event is Event.PhotoSending && event.status == SendingStatus.Sending -> focusRequest.freeFocus()
                else -> focusRequest.requestFocus()
            }
        }
    }

    Box(
        modifier = modifier
            .size(previewSize)
    ) {
        PhotoPreview(
            photoPath = photoPath
        )

        if (widgetTextModel.isActive) {
            DraggableTextField(
                modifier = Modifier
                    .offset {
                        if (widgetTextModel.offsetY >= 0) {
                            if (widgetTextModel.offsetY.dp > previewSize) {
                                IntOffset(0, previewSize.value.roundToInt())
                            } else {
                                IntOffset(0, widgetTextModel.offsetY)
                            }
                        } else {
                            if (abs(widgetTextModel.offsetY).dp > previewSize) {
                                IntOffset(0, -previewSize.value.roundToInt())
                            } else {
                                IntOffset(0, widgetTextModel.offsetY)
                            }
                        }
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { y ->
                            when {
                                y > 0 && widgetTextModel.offsetY.dp < previewSize -> changeOffsetY(y)
                                y < 0 && widgetTextModel.offsetY.dp > -previewSize -> changeOffsetY(y)
                            }
                        },
                    )
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .onSizeChanged { changeRectSize(it.height) },
                focusRequest = focusRequest,
                text = widgetTextModel.text,
                changeText = changeText,
                enabled = !(event is Event.PhotoSending && event.status == SendingStatus.Sending)
            )
        }
    }
}

@Composable
private fun DraggableTextField(
    modifier: Modifier = Modifier,
    focusRequest: FocusRequester,
    text: String,
    enabled: Boolean,
    changeText: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(colors = listOf(Color.Black, Color.Black)),
            alpha = 0.5f
        )
    ) {
        TextField(
            modifier = Modifier
                .align(Alignment.Center)
                .focusRequester(focusRequest),
            value = text,
            onValueChange = changeText,
            colors = getEditTextDefaultColors(),
            textStyle = TextStyle(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            ),
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = enabled
        )
    }
}

@Composable
fun PickFriendsButton(
    modifier: Modifier = Modifier,
    friendsCount: Int,
    pickFriend: () -> Unit,
) {
    Button(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius))),
        onClick = { pickFriend() },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
    ) {
        Text(
            text = stringResource(R.string.pick_friends_button, friendsCount),
            style = MaterialTheme.typography.button.copy(color = Color.Black, fontSize = 14.sp)
        )
    }
}


@Composable
fun PhotoPreview(
    modifier: Modifier = Modifier,
    photoPath: String?
) {
    Image(
        modifier = modifier
            .size(LocalCameraSize.current.size)
            .clip(RoundedCornerShape(20.dp))
            .border(6.dp, color = Color.LightGray, RoundedCornerShape(20.dp)),
        painter = rememberAsyncImagePainter(photoPath),
        contentScale = ContentScale.Crop,
        contentDescription = null
    )
}

@Composable
fun PreviewButtons(
    modifier: Modifier = Modifier,
    previewCallback: PreviewCallback
) {

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundedCornerButtonWithTint(
            modifier = Modifier.size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_download
        ) { previewCallback.download() }

        GradientImageButton(
            modifier = Modifier
                .padding(
                    start = dimensionResource(R.dimen.screen_elements_padding),
                )
                .size(dimensionResource(R.dimen.picture_button_size)),
            drawableRes = R.drawable.ic_send
        ) { previewCallback.sendPhoto() }

        RoundedCornerButtonWithTint(
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.screen_elements_padding))
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_remove
        ) { previewCallback.remove() }
    }
}

@Composable
fun SendingAnimation(
    modifier: Modifier = Modifier,
    @RawRes rawRes: Int
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        modifier = modifier,
        progress = progress
    )
}

fun navigateToPicker(navController: NavController) {
    navController.navigate(ScreenItem.FriendPicker.route)
}

suspend fun handlePreviewEvents(
    event: Event?,
    onSendingFail: (SendingStatus.Fail) -> Unit,
    onSendingSuccess: () -> Unit,
    onNavigate: (route: String?) -> Unit
) {
    event?.handleEvent(
        onPhotoSending = { status ->
            if (status is SendingStatus.Fail) onSendingFail(status)
            else if (status is SendingStatus.Success) onSendingSuccess()
        },
        onNavigate = onNavigate
    )
}

@Composable
fun rememberPreviewCallback(
    removePhoto: () -> Unit,
    send: () -> Unit,
    downloadImage: () -> Unit,
    permissionRequest: ManagedActivityResultLauncher<String, Boolean>,
    context: Context
): PreviewCallback = remember(permissionRequest, context) {
    object : PreviewCallback {
        override fun remove() = removePhoto()

        override fun sendPhoto() = send()

        override fun download() = when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                permissionRequest.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                permissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                downloadImage()
                LocketAnalytics.logDownloadPhoto()
            }
        }
    }
}