package com.example.locketwidget.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.locketwidget.R
import com.example.locketwidget.ui.LocketScreen

@Composable
fun SplashScreen(closeSplash: () -> Unit) {
    LocketScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.heart_splash))
            val splashState = animateLottieCompositionAsState(composition = composition)
            LottieAnimation(composition = composition, progress = splashState.progress, modifier = Modifier.align(Alignment.Center))

            if (splashState.isAtEnd && splashState.isPlaying) {
                closeSplash()
            }
        }
    }
}