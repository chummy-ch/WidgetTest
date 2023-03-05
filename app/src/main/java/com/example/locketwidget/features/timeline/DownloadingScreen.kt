package com.example.locketwidget.features.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalTimelineScope
import com.example.locketwidget.data.Event
import com.example.locketwidget.ui.LocketScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CreatingMomentsScreen(
    navController: NavController
) {
    val viewModel: MomentViewModel = mavericksViewModel(scope = LocalTimelineScope.current)
    val event = viewModel.collectAsState { it.event }.value

    val animationComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.space_adventure))
    val animationProgress by animateLottieCompositionAsState(
        composition = animationComposition,
        iterations = LottieConstants.IterateForever
    )

    BackHandler {}

    LocketScreen {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            LottieAnimation(modifier = Modifier.size(212.dp), composition = animationComposition, progress = animationProgress)
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                modifier = Modifier,
                text = stringResource(R.string.creating_moment_text),
                style = MaterialTheme.typography.h2,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    LaunchedEffect(event) {
        if (event is Event.Message || event is Event.Share<*>) {
            navController.popBackStack()
        }
    }
}

@Composable
fun DownloadingScreen(
    navController: NavController
) {
    val animationComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.flame_training))
    val animationProgress by animateLottieCompositionAsState(
        composition = animationComposition,
        iterations = LottieConstants.IterateForever
    )

    BackHandler {}

    LocketScreen {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            LottieAnimation(modifier = Modifier.size(212.dp), composition = animationComposition, progress = animationProgress)
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                modifier = Modifier,
                text = stringResource(R.string.downloading_moments_text),
                style = MaterialTheme.typography.h2,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(0.5f))

            val progressComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.progress_bar))
            val progress by animateLottieCompositionAsState(
                composition = animationComposition,
                isPlaying = true,
                restartOnPlay = false,
            )
            LottieAnimation(
                composition = progressComposition,
                progress = progress
            )

            LaunchedEffect(Unit) {
                while (progress != 1f && isActive) {
                    delay(100)
                }
                navController.popBackStack()
                //navController.navigate(ScreenItem.TimeLineEditor.route) { popUpTo(ScreenItem.Locket.route) }
            }
        }
    }
}