package com.example.locketwidget.screens.history

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.AppVibrationUseCase
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.createShareIntent
import com.example.locketwidget.core.provideActionIfPremium
import com.example.locketwidget.data.*
import com.example.locketwidget.screens.main.BottomSheetListItem
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.example.locketwidget.screens.preview.LivePhoto
import com.example.locketwidget.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import kotlin.math.roundToInt

interface HistoryCallback {
    fun download()

    fun share()

    fun sendReaction(reaction: String)

    fun hide()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    historyModel: HistoryModel,
    navController: NavController
) {
    val premiumViewModel: PremiumViewModel = mavericksActivityViewModel()
    val isPremium = premiumViewModel.collectAsState { it.isPremium }.value

    val viewModel: HistoryViewModel = mavericksViewModel(argsFactory = { historyModel })
    val history = viewModel.collectAsState { it.historyModel!! }.value
    val activityViewModel: ActivityViewModel = mavericksActivityViewModel()
    val sender = viewModel.collectAsState { it.sender }.value
    val event = viewModel.collectAsState { it.event }.value
    val reactions = viewModel.collectAsState { it.reactions }.value

    val shareLinkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val currentBottomSheetAction: MutableState<() -> Unit> = remember { mutableStateOf({}) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        //Remove to avoid android 10 crush
        /*if (granted) {
            provideActionIfPremium(
                isPremium = isPremium.invoke() == true,
                action = {
                    viewModel.download(true)
                },
                nonPremiumAction = {
                    currentBottomSheetAction.value = { viewModel.download(false) }
                    scope.launch { bottomSheetState.show() }
                }
            )
        }*/
    }
    LaunchedEffect(event) {
        event?.handleEvent(
            onShare = { data ->
                val uri = data as? Uri
                if (uri != null) {
                    val fileType = if (history.mode is HistoryMode.Video) "video/mp4" else "image/*"
                    val intent = createShareIntent(uri, fileType = fileType)
                    shareLinkLauncher.launch(intent)
                    viewModel.clearEvent()
                }
            },
            onNavigate = { route ->
                if (route == null) {
                    navController.popBackStack()
                    activityViewModel.setMessageEvent(R.string.photo_removed)
                } else if (route == ScreenItem.History.route) {
                    activityViewModel.setMessageEvent(R.string.unknown_error)
                }
                viewModel.clearEvent()
            }
        )
    }

    val historyCallback = remember {
        object : HistoryCallback {
            override fun download() {
                when (PackageManager.PERMISSION_DENIED) {
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
                        provideActionIfPremium(
                            isPremium = isPremium.invoke() == true,
                            action = {
                                viewModel.download(true)
                                LocketAnalytics.logDownloadPhoto()
                            },
                            nonPremiumAction = {
                                currentBottomSheetAction.value = {
                                    viewModel.download(false)
                                    LocketAnalytics.logDownloadPhoto()
                                }
                                scope.launch { bottomSheetState.show() }
                            }
                        )
                    }
                }
            }

            override fun share() {
                provideActionIfPremium(
                    isPremium = isPremium.invoke() == true,
                    action = {
                        viewModel.share(true)
                        LocketAnalytics.logSharePhoto()
                    },
                    nonPremiumAction = {
                        currentBottomSheetAction.value = {
                            viewModel.share(false)
                            LocketAnalytics.logSharePhoto()
                        }
                        scope.launch { bottomSheetState.show() }
                    }
                )
            }

            override fun sendReaction(reaction: String) {
                viewModel.sendReaction(reaction)
            }

            override fun hide() {
                viewModel.hidePhoto()
            }
        }
    }

    val sidePadding = dimensionResource(R.dimen.screen_elements_padding)
    val topPadding = dimensionResource(R.dimen.top_screen_elements_padding)
    val zoomBackgroundColorState = remember { mutableStateOf(Black.copy(0.0f)) }

    LocketScreen(
        topBar = {
            HistoryTopBar(
                navController = navController,
                historyCallback = historyCallback,
                zoomBackgroundColorState = zoomBackgroundColorState
            )
        },
        message = event?.mapToErrorMessage()
    ) { paddingValues ->
        ConstraintLayout(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .zIndex(1f)
        ) {

            val (senderItem, photo, reactionCarousel, historyButtonsRow) = createRefs()

            Box(
                Modifier
                    .fillMaxSize()
                    .background(zoomBackgroundColorState.value)
                    .zIndex(1f)
            )

            val senderModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePadding)
                .constrainAs(senderItem) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top, margin = topPadding)
                }
            when (sender) {
                is Success -> SenderItem(
                    modifier = senderModifier,
                    user = sender.invoke(),
                    history = history
                )
                else -> ShimmerAnimation {
                    ShimmerSenderItem(
                        modifier = senderModifier.height(38.dp),
                        alpha = it
                    )
                }
            }
            val previewModifier = Modifier
                .padding(top = 40.dp)
                .size(LocalCameraSize.current.size)
                .zIndex(2f)
                .constrainAs(photo) {
                    top.linkTo(senderItem.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            when (history.mode) {
                null -> {
                    Photo(
                        modifier = previewModifier,
                        photoLink = history.photoLink,
                        event = event,
                        zoomBackgroundColorState = zoomBackgroundColorState
                    )
                }
                is HistoryMode.Video -> {
                    var isPlaying by rememberSaveable {
                        mutableStateOf(false)
                    }
                    val source = remember(history.mode.link) {
                        val mediaItem = MediaItem.Builder().setUri(history.mode.link).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                        HlsMediaSource.Factory(DefaultDataSource.Factory(context)).createMediaSource(mediaItem)
                    }

                    VideoPlayer(
                        modifier = previewModifier,
                        mediaSource = source,
                        isPlaying = isPlaying,
                        thumb = history.photoLink,
                        play = { isPlaying = true },
                        pause = { isPlaying = false }
                    )
                }
                is HistoryMode.Live -> {
                    val output = remember(history.mode) {
                        CameraOutput.Live(
                            mainPhotoPath = if (history.mode.isMain) history.mode.link else history.photoLink,
                            secondaryPhotoPath = if (history.mode.isMain) history.photoLink else history.mode.link,
                        )
                    }
                    LivePhoto(
                        output = output,
                        modifier = previewModifier,
                        changeMainPhoto = viewModel::changeMainLivePhoto
                    )
                }
                is HistoryMode.Mood -> {
                    Photo(
                        modifier = previewModifier,
                        photoLink = history.photoLink,
                        event = event,
                        zoomBackgroundColorState = zoomBackgroundColorState
                    )
                }
            }

            ReactionCarousel(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(reactionCarousel) {
                        top.linkTo(photo.bottom)
                        bottom.linkTo(historyButtonsRow.top)
                        start.linkTo(parent.start)
                    }
                    .padding(horizontal = sidePadding),
                reactionsAsync = reactions
            )

            HistoryButtonsRow(
                modifier = Modifier
                    .constrainAs(historyButtonsRow) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        top.linkTo(reactionCarousel.bottom)
                    },
                historyCallback = historyCallback,
                isPremium = isPremium.invoke() ?: false,
                navController = navController
            )
        }
    }
    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetContent = {
            DownloadingBottomSheetContent(
                sheetState = bottomSheetState,
                scope = scope,
                openPremiumScreen = { navController.navigate("${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}=3") },
                watermarkAction = currentBottomSheetAction.value
            )
        },
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = (Black.copy(alpha = 0.15f)),
    ) {}
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DownloadingBottomSheetContent(
    sheetState: ModalBottomSheetState,
    scope: CoroutineScope,
    openPremiumScreen: () -> Unit,
    watermarkAction: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(40.dp, 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Black.copy(alpha = 0.5f))
        )
        BottomSheetListItem(
            icon = R.drawable.ic_gallery_gradient,
            text = stringResource(R.string.photo_origin),
            onClick = {
                scope.launch {
                    sheetState.hide()
                }
                openPremiumScreen()
            })
        BottomSheetListItem(
            icon = R.drawable.ic_photo_watermark,
            text = stringResource(R.string.photo_with_watermark),
            onClick = {
                scope.launch {
                    sheetState.hide()
                }
                watermarkAction()
            })
    }
}

@Composable
private fun HistoryTopBar(navController: NavController, historyCallback: HistoryCallback, zoomBackgroundColorState: MutableState<Color>) {
    Box(
        Modifier
            .wrapContentSize()
            .background(zoomBackgroundColorState.value)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null
                    )
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(
                        onClick = { historyCallback.download() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = { navigateToList(navController) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gallery),
                            contentDescription = null
                        )
                    }
                }
            }

            ScreenHeader(
                text = stringResource(ScreenItem.History.title!!),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_elements_padding))
            )
        }
    }
}

@Composable
private fun ReactionCarousel(
    modifier: Modifier = Modifier,
    reactionsAsync: Async<List<Async<ReactionModel>>>
) {
    if (reactionsAsync is Success && reactionsAsync.invoke().isNotEmpty()) {
        LazyRow(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            items(reactionsAsync.invoke()) { reactionAsync ->
                val reaction = reactionAsync.invoke()
                reaction?.let {
                    val reactionAnimation = Reactions.getReaction(reaction.emoji)
                    when (reactionAsync) {
                        is Success -> {
                            if (reactionAnimation != null) {
                                ReactionItem(
                                    modifier = Modifier
                                        .padding(start = 14.dp),
                                    senderPhoto = reaction.sender.photoLink,
                                    animation = reactionAnimation
                                )
                            }
                        }
                        is Loading -> {
                            if (reactionAnimation != null) {
                                ShimmerAnimation {
                                    ReactionItem(
                                        modifier = Modifier
                                            .padding(start = 14.dp)
                                            .alpha(it),
                                        senderPhoto = reaction.sender.photoLink,
                                        animation = reactionAnimation
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.empty_reaction),
                style = MaterialTheme.typography.h2.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W400,
                    textAlign = TextAlign.Center
                ),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun HistoryButtonsRow(
    modifier: Modifier = Modifier,
    historyCallback: HistoryCallback,
    isPremium: Boolean,
    navController: NavController
) {
    var reactionMenuState by remember { mutableStateOf(MultiFabState.COLLAPSED) }
    val changeState = remember {
        {
            reactionMenuState = if (reactionMenuState == MultiFabState.COLLAPSED) {
                MultiFabState.EXPANDED
            } else {
                MultiFabState.COLLAPSED
            }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val position = remember { mutableStateOf(IntOffset.Zero) }

    if (reactionMenuState == MultiFabState.EXPANDED) {
        ReactionDialog(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    changeState()
                },
            position = position.value,
            changeState = {
                changeState()
            },
            sendReaction = { reaction ->
                historyCallback.sendReaction(reaction)
                changeState()
            },
            isPremium = isPremium,
            navController = navController
        )
    }

    val density = LocalDensity.current
    val reactionButtonSize = dimensionResource(R.dimen.main_button_size)
    val menuPadding = dimensionResource(R.dimen.reaction_menu_padding)

    Row(
        modifier = modifier.onGloballyPositioned { layoutCoordinates ->
            val x = 0
            val padding = (reactionButtonSize.value + menuPadding.value) * density.density
            val y = layoutCoordinates.positionInRoot().y - padding
            position.value = IntOffset(x, y.roundToInt())
        }
    ) {
        val isRemoveDialogVisible = remember { mutableStateOf(false) }
        if (isRemoveDialogVisible.value) {
            HistoryHideDialog(
                onPositiveClick = {
                    isRemoveDialogVisible.value = false
                    historyCallback.hide()
                },
                hideDialog = {
                    isRemoveDialogVisible.value = false
                }
            )
        }
        RoundedCornerButtonWithTint(
            drawableRes = R.drawable.ic_remove,
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size))
                .align(Alignment.CenterVertically)
        ) {
            isRemoveDialogVisible.value = true
        }

        GradientImageButton(
            drawableRes = R.drawable.ic_reaction,
            modifier = Modifier
                .padding(start = 24.dp)
                .size(dimensionResource(R.dimen.main_button_size))
        ) {
            changeState()
        }

        RoundedCornerButtonWithTint(
            drawableRes = R.drawable.ic_share,
            modifier = Modifier
                .padding(start = 24.dp)
                .size(dimensionResource(R.dimen.button_size))
                .align(Alignment.CenterVertically)
        ) {
            historyCallback.share()
        }
    }
}

@Composable
private fun ReactionDialog(
    modifier: Modifier = Modifier,
    position: IntOffset,
    changeState: () -> Unit,
    sendReaction: (String) -> Unit,
    isPremium: Boolean,
    navController: NavController
) {

    Dialog(
        onDismissRequest = {
            changeState()
        }
    ) {
        Box(
            modifier = modifier
        ) {
            FloatingReactionActionMenu(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 200.dp)
                    .offset {
                        position
                    }
                    .fillMaxWidth()
                    .height(64.dp)
            ) { reaction ->
                provideActionIfPremium(
                    isPremium = reaction.IsPremium && !isPremium,
                    action = {
                        navController.navigate(ScreenItem.Premium.route)
                    },
                    nonPremiumAction = {
                        sendReaction(reaction.name)
                    }
                )
            }
        }
    }
}

@Composable
fun ShimmerSenderItem(
    modifier: Modifier,
    alpha: Float
) {
    val color = Color.LightGray.copy(alpha = alpha)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(20.dp))
            .padding(1.dp)
            .background(color)
    )
}

@Composable
fun SenderItem(
    modifier: Modifier = Modifier,
    user: FirestoreUserResponse,
    history: HistoryModel
) {
    Row(
        modifier = modifier
            .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(user.photoLink),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Text(
            text = user.name,
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
            style = MaterialTheme.typography.h2
        )
        Text(
            text = history.date.getTimeSpanString(),
            style = MaterialTheme.typography.h2.copy(fontSize = 10.sp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(end = 16.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun Photo(
    modifier: Modifier = Modifier,
    photoLink: String,
    event: Event?,
    zoomBackgroundColorState: MutableState<Color>
) {
    var animationState by remember {
        mutableStateOf(false)
    }
    val vibrationUseCase = get(AppVibrationUseCase::class.java)

    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    Box(modifier = modifier
        .pointerInputDetectTransformGestures(
            isTransformInProgressChanged = { isInProgress ->
                if (!isInProgress) {
                    zoomOffset = Offset.Zero
                    globalOffset = Offset.Zero
                    zoom = 1f
                    zoomBackgroundColorState.value = Black.copy(0.0f)
                }
            },
            onGesture = { centroid, pan, gestureZoom ->
                if (gestureZoom != 1f) {
                    zoom = when {
                        zoom < 1f -> 1f
                        zoom > 4f -> 4f
                        else -> {
                            val newScale = zoom * gestureZoom
                            zoomOffset = (zoomOffset + centroid / zoom) - (centroid / newScale + pan / zoom)
                            newScale
                        }
                    }
                    globalOffset += pan
                    zoomBackgroundColorState.value = Black.copy(0.5f)
                }
            }
        )
        .offset { IntOffset((globalOffset.x).roundToInt(), (globalOffset.y).roundToInt()) }
        .graphicsLayer {
            translationX = -zoomOffset.x * zoom
            translationY = -zoomOffset.y * zoom
            scaleX = zoom
            scaleY = zoom
            transformOrigin = TransformOrigin(0f, 0f)
        }) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
            painter = rememberAsyncImagePainter(photoLink),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        if (event is Event.Reaction) {
            val animatedSize by animateDpAsState(
                targetValue = if (!animationState) Reactions.START_SIZE_ANIMATION else Reactions.END_SIZE_ANIMATION,
                animationSpec = tween(Reactions.ANIMATION_DURATION_MILLS.toInt())
            )
            val reactionAnimation = Reactions.getReaction(event.emoji)
            if (reactionAnimation != null) {
                ReactionAnimation(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(animatedSize),
                    animation = reactionAnimation
                )
            }

            LaunchedEffect(Unit) {
                vibrationUseCase.vibrate()
                animationState = true
                delay(Reactions.REACTION_DURATION_MILLS - Reactions.ANIMATION_DURATION_MILLS)
                animationState = false
            }
        }
    }
}

fun Modifier.pointerInputDetectTransformGestures(
    isTransformInProgressChanged: (Boolean) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
): Modifier {
    return pointerInput(Unit) {
        detectTransformGestures(
            onGesture = { centroid, pan, gestureZoom, _ ->
                isTransformInProgressChanged(true)
                onGesture(centroid, pan, gestureZoom)
            }
        )
    }.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.any { it.consumed.positionChange }
                } while (!canceled && event.changes.any { it.pressed })
                isTransformInProgressChanged(false)
            }
        }
    }
}

private fun navigateToList(navController: NavController) {
    val listScreen = navController.backQueue.firstOrNull {
        it.destination.route == ScreenItem.HistoryList.route
    }
    if (listScreen != null) {
        navController.popBackStack()
    } else {
        navController.navigate(ScreenItem.HistoryList.route)
    }
}

@Composable
fun HistoryHideDialog(
    onPositiveClick: () -> Unit,
    hideDialog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = hideDialog,
        title = {
            Text(
                text = stringResource(R.string.remove_photo),
                style = MaterialTheme.typography.h1, color = Black
            )
        },
        text = {
            Text(
                text = stringResource(R.string.remove_photo_text),
                fontWeight = FontWeight.Bold
            )
        },
        buttons = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    modifier = Modifier
                        .weight(1f),
                    onClick = hideDialog
                )
                {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Black,
                        modifier = Modifier.padding(dimensionResource(R.dimen.history_dialog_padding))
                    )
                }
                TextButton(
                    modifier = Modifier
                        .weight(1f),
                    onClick = onPositiveClick
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = Black,
                        modifier = Modifier.padding(dimensionResource(R.dimen.history_dialog_padding))
                    )
                }
            }
        }
    )
}
