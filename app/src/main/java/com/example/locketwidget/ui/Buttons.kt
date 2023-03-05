package com.example.locketwidget.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.locketwidget.R
import com.example.locketwidget.ui.theme.Black400
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun GradientButton(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.button,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .background(brush = Brush.linearGradient(listOf(Orange300, Orange200)))
            .clickable { action() }
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp),
            text = text.uppercase(),
            style = style
        )
    }
}


@Composable
fun GradientButton(
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .border(
                width = 4.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .matchParentSize()
                .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Orange300, Orange200
                        )
                    )
                )
                .clickable { action() }
        )
    }
}

@Composable
fun GradientImageButton(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Orange300, Orange200
                    )
                )
            )
            .clickable { action() }
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(drawableRes),
            contentDescription = null
        )
    }
}

@Composable
fun RoundedCornerButtonWithTint(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    backgroundColor: Color = Color.White,
    tint: Color = Black400,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .background(color = backgroundColor)
            .clickable { action() }
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
fun RoundedCornerButton(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    backgroundColor: Color = Color.White,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .background(color = backgroundColor)
            .clickable { action() }
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = drawableRes),
            contentDescription = null,
        )
    }
}
