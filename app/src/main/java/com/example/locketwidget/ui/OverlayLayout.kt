package com.example.locketwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

private const val paddingCoefficient = 0.6

@Composable
fun OverlayLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        var width = 0
        placeables.forEachIndexed { index, placeable ->
            width += if (index == 0) placeable.width
            else (placeable.width * paddingCoefficient).roundToInt()
        }
        val height = placeables.firstOrNull()?.height ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                if (index == 0) {
                    placeable.placeRelative(0, 0)
                } else {
                    val x = placeable.width * paddingCoefficient * index
                    val z = 0 - index.toFloat()
                    placeable.placeRelativeWithLayer(x.toInt(), 0, z)
                }
            }
        }
    }
}