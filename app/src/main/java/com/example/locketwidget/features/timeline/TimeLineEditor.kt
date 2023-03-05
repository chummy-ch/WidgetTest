package com.example.locketwidget.features.timeline

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocalTimelineScope
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.handleEvent
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.data.timelineAudio
import com.example.locketwidget.screens.main.BottomSheetListItem
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.example.locketwidget.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TimeLineEditorScreen(
    navController: NavController
) {
    val premiumViewModel: PremiumViewModel = mavericksActivityViewModel()
    val isPremium = premiumViewModel.collectAsState { it.isPremium }.value

    val viewModel: MomentViewModel = mavericksViewModel(scope = LocalTimelineScope.current.apply { setScope(LocalLifecycleOwner.current) })
    val selectedList = viewModel.collectAsState { it.selectedPhotos }.value
    val name = viewModel.collectAsState { it.name }.value
    val event = viewModel.collectAsState { it.event }.value

    var currentPhotoIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.timeline_audio).apply { isLooping = true } }
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val defaultName = stringResource(id = R.string.default_timeline_name)
    val checkEmptyName: (name: String) -> Unit = remember {
        {
            if (name.isEmpty()) viewModel.setName(defaultName)
        }
    }

    val shareResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    LocketScreen(
        /*
        topBar = {
            TopBar(currentScreenItem = ScreenItem.TimeLineEditor) {
                navController.popBackStack()
            }
        },
         */
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) {

        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditNameTextField(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                hint = stringResource(R.string.timeline_vide_name_hint),
                name = name,
                changeName = viewModel::setName
            )

            if (selectedList.isNotEmpty()) {
                AsyncImage(
                    modifier = Modifier
                        .size(LocalCameraSize.current.size)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.image_corner_radius))),
                    model = selectedList[currentPhotoIndex].photoLink,
                    contentDescription = null,
                )
            }

            EditorButtonsRow(
                modifier = Modifier.padding(bottom = 26.dp),
                isPlaying = isPlaying,
                openSettings = { scope.launch { bottomSheetState.show() } },
                play = {
                    isPlaying = !isPlaying
                },
                share = {
                    checkEmptyName(name)
                    viewModel.shareVideo(isPremium.invoke() == true)
                    LocketAnalytics.logSharingMoment()
                }
            )
        }
    }

    BestieBottomSheet(modifier = Modifier.fillMaxSize(), state = bottomSheetState) {
        EditorBottomSheetMenu(
            hide = { scope.launch { bottomSheetState.hide() } },
            speedSettings = { navController.navigate(ScreenItem.SpeedSettings.route) },
            selectMoments = {
                navController.navigate(ScreenItem.SelectMoment.route)
                LocketAnalytics.logMomentSelection()
            },
            saveToGallery = {
                checkEmptyName(name)
                viewModel.saveToGallery(isPremium.invoke() == true)
                LocketAnalytics.logSharingMoment()
            }
        )
    }

    LaunchedEffect(event) {
        event?.handleEvent(
            onNavigate = { route ->
                if (route != null) navController.navigate(route)
                else navController.popBackStack()
            },
            onShare = { data ->
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, data as Uri)
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_moment_text))
                    type = "video/mp4/text"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                shareResult.launch(Intent.createChooser(intent, context.getString(R.string.share_moment_title)))
            }
        )
    }

    LaunchedEffect(isPlaying) {
        val frameDuration = (timelineAudio.duration - timelineAudio.firstFrame) / (selectedList.size - 1)
        if (isPlaying) {
            mediaPlayer.start()
        } else if (mediaPlayer.isPlaying) {
            if (currentPhotoIndex == selectedList.size - 1) mediaPlayer.seekTo(0)
            mediaPlayer.pause()
        }

        while (isPlaying && coroutineContext.isActive) {
            if (currentPhotoIndex == selectedList.size - 1) {
                currentPhotoIndex = 0
            }

            val currentFrameDuration = (if (currentPhotoIndex != 0) frameDuration else timelineAudio.firstFrame)
            delay(currentFrameDuration)
            currentPhotoIndex += 1
            if (currentPhotoIndex == selectedList.size - 1) {
                delay(currentFrameDuration)
                isPlaying = false
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }
}

@Composable
private fun ColumnScope.EditorBottomSheetMenu(
    hide: () -> Unit,
    speedSettings: () -> Unit,
    selectMoments: () -> Unit,
    saveToGallery: () -> Unit
) {
    /*BottomSheetListItem(icon = R.drawable.ic_speed, text = stringResource(R.string.timeline_sheet_speed), onClick = {
        hide()
        speedSettings()
    })*/
    BottomSheetListItem(icon = R.drawable.ic_approve_image, text = stringResource(R.string.timeline_sheet_select_moments), onClick = {
        hide()
        selectMoments()
    })
    BottomSheetListItem(icon = R.drawable.ic_download_gradient, text = stringResource(R.string.timeline_sheet_save), onClick = {
        hide()
        saveToGallery()
    })
}

@Composable
private fun EditorButtonsRow(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    openSettings: () -> Unit,
    play: () -> Unit,
    share: () -> Unit
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundedCornerButton(
            modifier = Modifier.size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_equalizer
        ) {
            openSettings()
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding)))
        GradientImageButton(
            modifier = Modifier.size(dimensionResource(R.dimen.main_button_size)),
            drawableRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        ) {
            play()
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.screen_elements_padding)))
        RoundedCornerButton(
            modifier = Modifier.size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_share
        ) {
            share()
        }
    }
}
