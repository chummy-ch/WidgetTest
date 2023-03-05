package com.example.locketwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.locketwidget.core.*

private val LightColorPalette = lightColors(
    primary = Orange250,
    primaryVariant = Purple700,
    secondary = Color.White,
    background = Pink200,
    onSecondary = Orange200,
    secondaryVariant = Orange300,
    onPrimary = Black400,
    onSurface = Orange200,
)

@Composable
fun LocketWidgetTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {

    CompositionLocalProvider(
        LocalCameraSize provides CameraSize(getLocketWindowWidthWithPadding().dp),
        LocalPhotoScope provides CustomLifecycleScope(),
        LocalTimelineScope provides CustomLifecycleScope()
    ) {
        val colors = if (darkTheme) LightColorPalette else LightColorPalette

        LaunchedEffect(darkTheme) {
            LocketAnalytics.setThemeProperty(darkTheme)
        }

        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

@Composable
fun getEditTextDefaultColors() = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)