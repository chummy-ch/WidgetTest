package com.example.locketwidget.screens.widgetmanager

import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.provideActionIfPremium
import com.example.locketwidget.data.*
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.DarkGrey
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.example.locketwidget.ui.theme.Yellow100
import com.example.locketwidget.widget.WidgetTransformation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WidgetDetails(
    locketWidget: LocketWidgetModel,
    photoLink: String?,
    navController: NavController
) {
    val premiumViewModel: PremiumViewModel = mavericksActivityViewModel()
    val isPremium = premiumViewModel.collectAsState { it.isPremium }.value.invoke()
    val viewModel: WidgetDetailsViewModel = mavericksViewModel(argsFactory = { locketWidget })
    val widget = viewModel.collectAsState { it.widget }.value.invoke() ?: return
    val friends = viewModel.collectAsState { it.friends }.value
    val styles = viewModel.collectAsState { it.style }.value
    val event = viewModel.collectAsState { it.event }.value

    val scope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    LaunchedEffect(event) {
        if (event is Event.Navigate) {
            navController.popBackStack()
        }
    }

    LocketScreen(
        topBar = {
            TopBar(
                currentScreenItem = ScreenItem.WidgetDetails
            ) { navController.popBackStack() }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        top = 24.dp,
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding)
                    )
            ) {
                EditNameTextField(hint = stringResource(id = R.string.widget_name), name = widget.name, changeName = viewModel::changeName)

                ShapeCarousel(
                    modifier = Modifier.padding(top = 24.dp),
                    selectedStyle = widget.style,
                    photoLink = photoLink,
                    styles = styles,
                    select = { style ->
                        if (style.isPremium) {
                            provideActionIfPremium(
                                isPremium = isPremium,
                                action = { viewModel.selectWidgetStyle(style) },
                                nonPremiumAction = {
                                    navController.navigate("${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}=0")
                                }
                            )
                        } else {
                            if (style is WidgetShape && style.shape == WidgetShapes.Rectangle) {
                                scope.launch {
                                    modalSheetState.animateTo(ModalBottomSheetValue.Expanded)
                                }
                            } else {
                                viewModel.selectWidgetStyle(style)
                            }
                        }
                    }
                )

                if (friends is Success) {

                    FriendsList(
                        modifier = Modifier
                            .padding(top = 26.dp)
                            .weight(1f),
                        friends = friends.invoke(),
                        title = R.string.selected_friends_list_title,
                        select = viewModel::select
                    )

                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.screen_top_padding)))

                if (friends is Success) {
                    val selectedFriends = friends.invoke().filter {
                        it.second
                    }
                    SenderInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        selectedFriends = selectedFriends,
                        widget = widget,
                        changeSenderInfo = viewModel::changeSenderInfo
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                GradientButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.button_size)),
                    text = stringResource(R.string.save_button)
                ) {
                    viewModel.save()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            BestieBottomSheet(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(), state = modalSheetState
            ) {
                WidgetColorCustomization(
                    photoLink = photoLink,
                    selectedColor = if (widget.style is WidgetShape && widget.style.shape == WidgetShapes.Rectangle) {
                        widget.style.stroke
                    } else {
                        StrokeGradientModel(listOf(Yellow100.toArgb(), Blue.toArgb()), GradientDirection.TOP)
                    },
                    applyColor = {
                        scope.launch {
                            modalSheetState.hide()
                        }
                        viewModel.selectWidgetStyle(WidgetShape(WidgetShapes.Rectangle, false, it))
                    }
                )
            }
        }
    }
}

@Composable
fun WidgetColorCustomization(
    photoLink: String?,
    selectedColor: StrokeGradientModel,
    applyColor: (StrokeGradientModel) -> Unit
) {
    var currentColor by remember {
        mutableStateOf(selectedColor)
    }

    Spacer(modifier = Modifier.height(24.dp))

    val imageSize = LocalCameraSize.current.size
    AsyncImage(
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
            .size(imageSize)
            .clip(RoundedCornerShape(30.dp))
            .gradientBorder(currentColor, imageSize, 30.dp, dimensionResource(R.dimen.widget_custom_stroke_width)),
        model = photoLink,
        error = painterResource(WidgetShapes.Rectangle.backgroundRes),
        contentDescription = null
    )

    LazyRow(modifier = Modifier.padding(top = 24.dp)) {
        item { Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding))) }
        item {
            Image(
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen.widget_stroke_color_item_size))
                    .size(dimensionResource(R.dimen.widget_stroke_color_item_size))
                    .clickable {
                        currentColor = StrokeGradientModel(listOf(Color.Transparent.toArgb()))
                    },
                painter = painterResource(R.drawable.ic_no),
                contentDescription = null
            )
        }
        items(WidgetStrokeColors) { color ->
            val itemSize = dimensionResource(R.dimen.widget_stroke_color_item_size)
            Box(
                modifier = Modifier
                    .padding(end = itemSize)
                    .size(dimensionResource(R.dimen.widget_stroke_color_item_size))
                    .clip(CircleShape)
                    .run {
                        when (color.direction) {
                            GradientDirection.NONE -> background(color = Color(color.colors.first()))
                            GradientDirection.TOP -> background(brush = Brush.verticalGradient(color.colors.map { Color(it) }))
                            GradientDirection.START -> background(brush = Brush.horizontalGradient(color.colors.map { Color(it) }))
                            GradientDirection.DIAGONAL -> background(
                                brush = Brush.linearGradient(
                                    color.colors.map { Color(it) },
                                    start = Offset(0f, itemSize.value), end = Offset(itemSize.value, 0f)
                                )
                            )
                        }
                    }
                    .clickable {
                        currentColor = color
                    }
            )
        }
    }
    GradientButton(
        modifier = Modifier
            .padding(dimensionResource(R.dimen.screen_elements_padding))
            .fillMaxWidth(),
        text = stringResource(R.string.stroke_save)
    ) {
        applyColor(currentColor)
    }
}

@Composable
private fun SenderInfoItem(
    modifier: Modifier,
    selectedFriends: List<Pair<ContactResponse, Boolean>>,
    widget: LocketWidgetModel,
    changeSenderInfo: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var isEnabled by remember { mutableStateOf(true) }
        if (selectedFriends.size > 1 &&
            (widget.style == WidgetShape(WidgetShapes.Rectangle, false) || widget.style == WidgetShape(WidgetShapes.Rectangle, true))
        ) {
            isEnabled = true
        } else {
            changeSenderInfo(false)
            isEnabled = false
        }
        Text(
            text = stringResource(R.string.show_friends_name_on_widget),
            color = if (isEnabled) Black else Black.copy(0.3f),
            style = MaterialTheme.typography.h2
        )
        Switch(
            checked = widget.isSenderInfoShown,
            onCheckedChange = {
                changeSenderInfo(it)
            },
            modifier = Modifier
                .scale(1.7f)
                .padding(end = 6.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary,
                disabledUncheckedThumbColor = White,
                disabledUncheckedTrackColor = DarkGrey
            ),
            enabled = isEnabled
        )
    }
}

@Composable
private fun ShapeCarousel(
    modifier: Modifier = Modifier,
    selectedStyle: WidgetStyle,
    photoLink: String?,
    styles: List<WidgetStyle>,
    select: (WidgetStyle) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.widget_details_shape), style = MaterialTheme.typography.h2)
        LazyRow(modifier = Modifier.padding(top = 8.dp)) {
            items(styles) { style ->
                when (style) {
                    is WidgetShape -> {
                        ShapeItem(
                            shape = style,
                            isSelected = (selectedStyle is WidgetShape && selectedStyle.shape == style.shape),
                            select = select,
                            photoLink = photoLink,
                            strokeColor = if (selectedStyle is WidgetShape && style.shape == WidgetShapes.Rectangle)
                                selectedStyle.stroke
                            else StrokeGradientModel(listOf(Color.Transparent.toArgb()))
                        )
                    }
                    is WidgetForeground -> {
                        ForegroundItem(
                            foreground = style,
                            isSelected = style == selectedStyle,
                            select = select,
                            photoLink = photoLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForegroundItem(
    modifier: Modifier = Modifier,
    foreground: WidgetForeground,
    isSelected: Boolean,
    photoLink: String?,
    select: (WidgetForeground) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .padding(end = 10.dp)
            .clickable {
                select(foreground)
                if (!isSelected) {
                    LocketAnalytics.logWidgetShapeChange(foreground.asset)
                }
            }
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(60.dp)
                .padding(6.dp),
            model = photoLink,
            contentDescription = null,
            error = {
                Image(
                    modifier = Modifier
                        .size(60.dp),
                    contentDescription = null,
                    painter = painterResource(R.drawable.default_avatar)
                )
            }
        )
        Image(
            modifier = Modifier
                .size(60.dp),
            painter = rememberAsyncImagePainter(
                model = foreground.preview,
                imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }
            ),
            contentDescription = null,
        )
        if (isSelected) {
            Image(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .background(brush = Brush.linearGradient(listOf(Orange200, Orange300))),
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ShapeItem(
    modifier: Modifier = Modifier,
    shape: WidgetShape,
    isSelected: Boolean,
    photoLink: String?,
    strokeColor: StrokeGradientModel,
    select: (WidgetShape) -> Unit
) {
    Box(
        modifier = modifier
            .padding(end = 10.dp)
            .clickable {
                select(shape)
                if (!isSelected) {
                    LocketAnalytics.logWidgetShapeChange(shape.shape.name)
                }
            }
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(60.dp)
                .gradientBorder(strokeColor, 60.dp, 10.dp, 4.dp),
            model = ImageRequest.Builder(LocalContext.current).data(photoLink).transformations(WidgetTransformation(shape.shape)).build(),
            contentDescription = null,
            error = {
                Image(
                    modifier = Modifier
                        .size(60.dp)
                        .gradientBorder(strokeColor, 60.dp, 10.dp, 4.dp),
                    bitmap = shape.shape.run {
                        toShape(BitmapFactory.decodeResource(LocalContext.current.resources, shape.shape.backgroundRes))
                    }.asImageBitmap(),
                    contentDescription = null
                )
            }
        )
        if (isSelected) {
            Image(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .background(brush = Brush.linearGradient(listOf(Orange200, Orange300))),
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null
            )
        }
    }
}