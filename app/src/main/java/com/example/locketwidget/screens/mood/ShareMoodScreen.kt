package com.example.locketwidget.screens.mood

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.emoji_mashup.DefaultEmojiList
import com.example.emoji_mashup.EmojiModel
import com.example.emoji_mashup.ui.EmojiItem
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.handleEvent
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.ui.FloatingDialogActionMenu
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.TopBar
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import kotlin.math.roundToInt

@Composable
fun ShareMoodScreen(
    navController: NavController
) {
    val viewModel: MoodViewModel = mavericksViewModel()
    val mashup = viewModel.collectAsState { it.mashupEmoji }.value
    val firstEmoji = viewModel.collectAsState { it.firstEmoji }
    val secondEmoji = viewModel.collectAsState { it.secondEmoji }
    val event = viewModel.collectAsState { it.event }

    var isFloatingMenuVisible by remember { mutableStateOf(false) }
    val globalPickerPosition = remember {
        mutableStateOf(Offset.Zero)
    }
    val selectedIndex = remember {
        mutableStateOf(0)
    }

    LaunchedEffect(event.value) {
        event.value?.handleEvent(
            onNavigate = { route ->
                if (route != null) {
                    navController.navigate(route) { popUpTo(ScreenItem.Locket.route) }
                }
            }
        )
    }

    LocketScreen(
        topBar = { TopBar(currentScreenItem = ScreenItem.ShareMood, popBackStack = navController::popBackStack) },
        message = event.value?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                EmojiPreview(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(LocalCameraSize.current.size),
                    emoji = mashup
                )
                EmojiMashuper(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .onGloballyPositioned {
                            globalPickerPosition.value = it.positionInRoot()
                        },
                    firstEmoji = firstEmoji.value,
                    secondEmojiModel = secondEmoji.value,
                    openMenu = { index ->
                        selectedIndex.value = index
                        isFloatingMenuVisible = true
                    }
                )

                GradientButton(
                    text = stringResource(R.string.save_button),
                    modifier = Modifier.fillMaxWidth(),
                    action = viewModel::saveMood
                )
            }

            val floatingMenuSize = remember {
                mutableStateOf(IntSize.Zero)
            }
            val bottomMenuPadding = LocalDensity.current.run { 16.dp.toPx() }.roundToInt()

            if (isFloatingMenuVisible) {
                EmojiFloatingMenu(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { floatingMenuSize.value = it }
                        .offset {
                            IntOffset(0, globalPickerPosition.value.y.roundToInt() - floatingMenuSize.value.height - bottomMenuPadding)
                        },
                    emojiList = DefaultEmojiList,
                    select = { emoji ->
                        viewModel.selectEmoji(selectedIndex.value, emoji)
                        isFloatingMenuVisible = false
                    },
                    dismiss = { isFloatingMenuVisible = false },
                    selectedEmoji = if (selectedIndex.value == 0) firstEmoji.value else secondEmoji.value
                )
            }
        }
    }
}

private const val EMOJI_PER_ROW = 6

@Composable
private fun EmojiFloatingMenu(
    modifier: Modifier,
    emojiList: List<EmojiModel>,
    select: (EmojiModel) -> Unit,
    selectedEmoji: EmojiModel?,
    dismiss: () -> Unit
) {
    FloatingDialogActionMenu(modifier = modifier, onDismiss = dismiss) {
        Column {
            emojiList.chunked(EMOJI_PER_ROW).forEach { list ->
                Row(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    list.forEach { emoji ->
                        Box(modifier = Modifier
                            .size(dimensionResource(R.dimen.emoji_box_size))
                            .clip(RoundedCornerShape(16.dp))
                            .run {
                                if (selectedEmoji == emoji) {
                                    background(brush = Brush.horizontalGradient(listOf(Orange200, Orange300)))
                                } else {
                                    this
                                }
                            }
                            .clickable { select(emoji) }
                        ) {
                            EmojiItem(
                                emojiModel = emoji,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(dimensionResource(R.dimen.emoji_menu_size))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiPicker(
    modifier: Modifier,
    selectedEmoji: EmojiModel?,
    openMenu: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { openMenu() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (selectedEmoji == null) {
            Image(painter = painterResource(R.drawable.ic_emoji_template), contentDescription = null, modifier = Modifier.size(28.dp))
        } else {
            EmojiItem(emojiModel = selectedEmoji, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(24.dp)
        )
    }
}

@Composable
private fun EmojiPreview(
    modifier: Modifier,
    emoji: EmojiModel?
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.photo_drawing_corner_radius)))
            .background(color = Color.White)
    ) {
        if (emoji != null) {
            EmojiItem(
                emojiModel = emoji,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(dimensionResource(R.dimen.emoji_preview_size))
            )
        }
    }
}

@Composable
private fun EmojiMashuper(
    modifier: Modifier,
    firstEmoji: EmojiModel?,
    secondEmojiModel: EmojiModel?,
    openMenu: (Int) -> Unit
) {
    Row(modifier = modifier) {
        EmojiPicker(modifier = Modifier, selectedEmoji = firstEmoji, openMenu = { openMenu(0) })
        Image(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .align(Alignment.CenterVertically)
                .size(16.dp)
        )
        EmojiPicker(modifier = Modifier, selectedEmoji = secondEmojiModel, openMenu = { openMenu(1) })
    }
}