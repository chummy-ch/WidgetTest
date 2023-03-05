package com.example.locketwidget.features.timeline

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocalTimelineScope
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.TopBarWithCloseButton
import com.example.locketwidget.ui.theme.Orange200
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun SpeedSettingsScree(navController: NavController) {
    val viewModel: MomentViewModel = mavericksViewModel(scope = LocalTimelineScope.current)

    val selectedPhotos = viewModel.collectAsState { it.selectedPhotos }
    val speed = viewModel.collectAsState { it.speed }.value
    var isPlaying by remember {
        mutableStateOf(false)
    }
    var currentPhotoIndex by remember {
        mutableStateOf(0)
    }
    val context = LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.timeline_audio).apply { isLooping = true }
    }

    LocketScreen(
        topBar = {
            TopBarWithCloseButton(screen = ScreenItem.SpeedSettings) {
                navController.popBackStack()
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                .padding(bottom = dimensionResource(R.dimen.screen_elements_padding))
                .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Player(
                modifier = Modifier
                    .padding(top = dimensionResource(R.dimen.top_screen_elements_padding))
                    .size(LocalCameraSize.current.size)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.image_corner_radius))),
                currentPhoto = selectedPhotos.value[currentPhotoIndex].photoLink,
                isPlaying = isPlaying,
                play = { isPlaying = !isPlaying }
            )

            SpeedsRow(
                modifier = Modifier
                    .fillMaxWidth(),
                speeds = listOf(0.5f, 0.75f, 1f, 1.5f, 2f),
                selectedSpeed = speed,
                selectSpeed = viewModel::selectSpeed
            )

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth(), text = stringResource(R.string.photo_editor_save_button)
            ) {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(isPlaying, speed) {
        val photoDuration = (MomentViewModel.PHOTO_DURATION_MLS * (1 / speed)).toLong()
        val isLastPhoto = currentPhotoIndex == selectedPhotos.value.size - 1

        if (isPlaying) {
            mediaPlayer.start()
        } else if (mediaPlayer.isPlaying) {
            if (isLastPhoto) mediaPlayer.seekTo(0)
            mediaPlayer.pause()
        }

        while (isPlaying && coroutineContext.isActive) {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.seekTo(0)
                mediaPlayer.start()
            }
            if (isLastPhoto) {
                currentPhotoIndex = 0
            }
            delay(photoDuration)
            currentPhotoIndex += 1
            if (isLastPhoto) {
                delay(photoDuration)
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
private fun SpeedsRow(
    modifier: Modifier,
    speeds: List<Float>,
    selectedSpeed: Float,
    selectSpeed: (Float) -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        speeds.forEach { speed ->
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.button_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
                    .background(color = Color.White)
                    .clickable { selectSpeed(speed) }
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = if (speed % 1f == 0f) speed.roundToInt().toString() else speed.toString(),
                    style = MaterialTheme.typography.h2.copy(
                        fontWeight = FontWeight.W700,
                        color = if (selectedSpeed == speed) Orange200 else Color.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun Player(
    modifier: Modifier = Modifier,
    currentPhoto: String,
    isPlaying: Boolean,
    play: () -> Unit
) {
    Box(modifier = modifier) {
        AsyncImage(modifier = Modifier.fillMaxSize(), model = currentPhoto, contentDescription = null)
        Image(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .clickable { play() },
            painter = if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play),
            contentDescription = null
        )
    }
}