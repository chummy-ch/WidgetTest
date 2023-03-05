package com.example.locketwidget.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.locketwidget.ui.theme.Orange200

@Composable
fun ShimmerAnimation(
    content: @Composable (shimmerAlpha: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.7f at 500
            },
            repeatMode = RepeatMode.Reverse
        )
    )
    content(alpha)
}

@Composable
fun ShimmeringItem(
    modifier: Modifier = Modifier,
    alpha: Float = 0f,
) {
    val color = Orange200.copy(alpha = alpha)
    Box(modifier = modifier.background(color))
}
