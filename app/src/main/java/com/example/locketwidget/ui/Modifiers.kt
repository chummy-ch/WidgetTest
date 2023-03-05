package com.example.locketwidget.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.example.locketwidget.R
import com.example.locketwidget.data.GradientDirection
import com.example.locketwidget.data.StrokeGradientModel


fun Modifier.gradientBorder(gradient: StrokeGradientModel, size: Dp, cornerRadius: Dp, strokeWidth: Dp): Modifier = composed {
    this.then(
        if (gradient.direction == GradientDirection.NONE) {
            Modifier.border(
                width = dimensionResource(R.dimen.widget_custom_stroke_width),
                color = Color(gradient.colors.first()),
                shape = RoundedCornerShape(cornerRadius)
            )
        } else {
            val brush = when (gradient.direction) {
                GradientDirection.TOP -> Brush.verticalGradient(gradient.colors.map { Color(it) })
                GradientDirection.START -> Brush.horizontalGradient(gradient.colors.map { Color(it) })
                GradientDirection.DIAGONAL -> Brush.linearGradient(
                    colors = gradient.colors.map { Color(it) },
                    start = Offset(0f, size.value), end = Offset(size.value, 0f)
                )
                else -> error("error $gradient")
            }
            Modifier.border(
                width = strokeWidth,
                brush = brush,
                shape = RoundedCornerShape(cornerRadius)
            )
        }
    )
}