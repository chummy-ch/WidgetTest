package com.example.locketwidget.screens.textmoments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.createShareIntent
import com.example.locketwidget.data.PhotoPreviewType
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.handleEvent
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.Grey
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.example.locketwidget.ui.theme.getEditTextDefaultColors
import java.io.File


interface TextMomentsCallback {

    fun share()

    fun download()

    fun save()
}

@Composable
fun TextMomentsScreen(
    navController: NavController
) {
    val viewModel: TextMomentsViewModel = mavericksViewModel()
    val patterns = viewModel.collectAsState { it.patterns }.value
    val chosenPattern = viewModel.collectAsState { it.chosenPattern }.value
    val event = viewModel.collectAsState { it.event }.value
    val imagePath = viewModel.collectAsState { it.imagePath }.value
    val shareLinkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.downloadPhoto()
    }

    val textMomentsCallback = object : TextMomentsCallback {
        override fun share() {
            viewModel.sharePhoto()
            LocketAnalytics.logSharePhoto()
        }

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
                    viewModel.downloadPhoto()
                    LocketAnalytics.logDownloadPhoto()
                }
            }
        }

        override fun save() = viewModel.saveImage()
    }

    LaunchedEffect(event) {
        event?.run {
            handleEvent(
                onNavigate = { route ->
                    if (route == ScreenItem.PhotoPreview.route && imagePath is Success) {
                        val link = (PhotoPreviewType(imagePath.invoke())).path
                        val isPhoto = false
                        val path = "${ScreenItem.PhotoPreview.route}?${ScreenItem.PREVIEW_ARG}=$link," +
                                "${ScreenItem.PREVIEW_TYPE_ARG}=$isPhoto," +
                                "${ScreenItem.EMOJIS_ARG}=${null}"
                        navController.popBackStack()
                        navController.navigate(path)
                    }
                },
                onShare = { data ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName,
                        File(data as String)
                    )
                    if (uri != null) {
                        val intent = createShareIntent(uri, fileType = "image/*")
                        shareLinkLauncher.launch(intent)
                    }
                }
            )
            viewModel.clearEvent()
        }
    }

    LocketScreen(
        topBar = { TopBar(currentScreenItem = ScreenItem.TextMoments) { navController.popBackStack() } },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    focusManager.clearFocus(true)
                }
        ) {

            PatternCarousel(
                modifier = Modifier.padding(
                    top = 24.dp,
                ),
                patterns = patterns,
                chosenPattern = chosenPattern,
                textMomentsViewModel = viewModel,
                context = context
            )
            ImagePreviewBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 22.dp),
                textMomentsCallback = textMomentsCallback,
                chosenPattern = chosenPattern,
                viewModel = viewModel,
                context = context,
                focusManager = focusManager
            )
        }
    }
}

@Composable
fun PatternCarousel(
    modifier: Modifier = Modifier,
    patterns: List<TextMomentBackground>,
    chosenPattern: TextMomentBackground?,
    textMomentsViewModel: TextMomentsViewModel,
    context: Context
) {
    LazyRow(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding)))
        }
        items(patterns) {
            val itemModifier = Modifier
                .padding(end = 10.dp)
                .size(dimensionResource(R.dimen.history_size))
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    LocketAnalytics.logTextBackgroundSelected(context.resources.getResourceEntryName(it.backgroundId))
                    textMomentsViewModel.resetChosenPattern(it)
                }
            if (it == chosenPattern)
                PatternItem(
                    modifier = itemModifier.border(
                        1.dp,
                        Brush.horizontalGradient(colors = listOf(Orange200, Orange300)),
                        RoundedCornerShape(16.dp)
                    ),
                    patternId = it.backgroundId
                )
            else
                PatternItem(
                    modifier = itemModifier,
                    patternId = it.backgroundId
                )

        }
    }
}

@Composable
fun PatternItem(
    modifier: Modifier,
    patternId: Int
) {
    Image(
        modifier = modifier,
        painter = painterResource(patternId),
        contentDescription = "pattern"
    )
}

@Composable
fun ImagePreviewBlock(
    modifier: Modifier = Modifier,
    textMomentsCallback: TextMomentsCallback,
    chosenPattern: TextMomentBackground?,
    viewModel: TextMomentsViewModel,
    context: Context,
    focusManager: FocusManager
) {
    ConstraintLayout(modifier = modifier.fillMaxHeight()) {

        val (previewRef, okButtonRef, shareButtonRef, saveButtonRef) = createRefs()

        if (chosenPattern != null)
            ImagePreview(
                Modifier
                    .size(LocalCameraSize.current.size)
                    .clip(RoundedCornerShape(20.dp))
                    .border(6.dp, color = Color.LightGray, RoundedCornerShape(20.dp))
                    .padding(6.dp)
                    .constrainAs(previewRef) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top, 40.dp)
                    }, chosenPattern, focusManager, viewModel
            )
        else {
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

        GradientImageButton(
            drawableRes = R.drawable.ic_big_tick,
            modifier = Modifier
                .constrainAs(okButtonRef) {
                    start.linkTo(previewRef.start)
                    end.linkTo(previewRef.end)
                    top.linkTo(previewRef.bottom)
                    bottom.linkTo(parent.bottom)
                }
                .size(dimensionResource(R.dimen.main_button_size))
        ) {
            if (chosenPattern != null)
                LocketAnalytics.logTextMomentDone(context.resources.getResourceEntryName(chosenPattern.backgroundId))
            textMomentsCallback.save()
        }

        RoundedCornerButtonWithTint(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size))
                .constrainAs(shareButtonRef) {
                    top.linkTo(okButtonRef.top)
                    bottom.linkTo(okButtonRef.bottom)
                    start.linkTo(okButtonRef.end, margin = 26.dp)
                },
            drawableRes = R.drawable.ic_share, backgroundColor = Color.White
        ) {
            textMomentsCallback.share()
        }

        RoundedCornerButtonWithTint(
            modifier = Modifier
                .constrainAs(saveButtonRef) {
                    top.linkTo(okButtonRef.top)
                    bottom.linkTo(okButtonRef.bottom)
                    end.linkTo(okButtonRef.start, margin = 26.dp)
                }
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_download, backgroundColor = MaterialTheme.colors.secondary
        ) {
            textMomentsCallback.download()
        }
    }
}

@SuppressLint("ResourceType")
@Composable
fun ImagePreview(
    modifier: Modifier,
    chosenPattern: TextMomentBackground,
    focusManager: FocusManager,
    viewModel: TextMomentsViewModel
) {
    val maxTextSize = 48
    val textSizeState = remember { mutableStateOf(maxTextSize) }
    var maxSize by remember { mutableStateOf(0.dp) }
    var editText by remember { mutableStateOf("") }
    val currHeightState = remember { mutableStateOf(0.dp) }

    val pxSize = LocalDensity.current.run {
        LocalCameraSize.current.size.toPx()
    }

    LaunchedEffect(pxSize) {
        viewModel.setPreviewSize(pxSize)
    }
    ConstraintLayout(modifier) {
        val (box, textField) = createRefs()
        Image(painter = painterResource(chosenPattern.backgroundId), contentDescription = null)
        TextField(
            modifier = Modifier
                .constrainAs(textField) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .fillMaxWidth()
                .wrapContentHeight(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                }
            ),
            value = editText,
            onValueChange = {
                editText = it
                viewModel.changeText(it)
            },
            placeholder = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Grey),
                    text = stringResource(R.string.start_typing)
                )
            },
            textStyle = MaterialTheme.typography.subtitle1.copy(
                textAlign = TextAlign.Center,
                fontSize = textSizeState.value.sp,
                color = Color(ContextCompat.getColor(LocalContext.current, chosenPattern.textColor))
            ),
            colors = getEditTextDefaultColors()
        )
        BoxWithConstraints(modifier = Modifier
            .constrainAs(box) {
                height = Dimension.fillToConstraints
                start.linkTo(textField.start)
                end.linkTo(textField.end)
                top.linkTo(textField.top)
                bottom.linkTo(textField.bottom)
            }) {
            currHeightState.value = maxHeight
            maxSize = maxWidth
            setTextSize(
                currHeightState,
                maxSize,
                textSizeState,
                editText,
                maxTextSize
            ) { viewModel.changeTextSize(it) }
        }
    }
}

fun setTextSize(
    currHeightState: MutableState<Dp>,
    maxSize: Dp,
    textSizeState: MutableState<Int>,
    text: String,
    maxTextSize: Int,
    onSizeChange: (Int) -> Unit
) {
    if (currHeightState.value >= maxSize) {
        textSizeState.value -= 1
    } else if (text.isEmpty()) {
        textSizeState.value = maxTextSize
    }
    onSizeChange(textSizeState.value)
}