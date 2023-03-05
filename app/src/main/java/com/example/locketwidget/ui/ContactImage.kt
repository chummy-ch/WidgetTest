package com.example.locketwidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

@Composable
fun ContactImageWithBorder(
    modifier: Modifier = Modifier,
    painter: Painter,
    borderBrush: Brush,
    borderPadding: Dp,
    borderSize: Dp
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = borderSize,
                brush = borderBrush,
                shape = CircleShape
            )
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .padding(borderPadding)
                .clip(CircleShape)
                .align(Alignment.Center),
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}