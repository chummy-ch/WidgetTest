package com.example.locketwidget.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.locketwidget.R

@Composable
fun getLocketWindowWidthWithPadding(): Int {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val padding = dimensionResource(R.dimen.camera_vertical_padding)
    return remember {
        (screenWidth - padding * 2).value.toInt() / 10 * 10
    }
}

data class CameraSize(val size: Dp = 0.dp)

val LocalCameraSize = staticCompositionLocalOf { CameraSize() }


