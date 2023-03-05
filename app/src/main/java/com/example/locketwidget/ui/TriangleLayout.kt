package com.example.locketwidget.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.example.locketwidget.R
import kotlin.math.roundToInt


@Composable
fun LazyTriangleLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val topPadding = dimensionResource(R.dimen.contacts_items_top_padding)
    val itemPadding = LocalDensity.current.run { topPadding.toPx() }.roundToInt()

    Layout(
        modifier = modifier.verticalScroll(rememberScrollState()),
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        var height = 0

        placeables.forEachIndexed { index, placeable ->
            when {
                index == 0 -> height += placeable.height
                (index + 1) % 3 != 0 -> {
                    height += placeable.height / 2 + itemPadding
                }
            }
        }

        layout(constraints.maxWidth, height) {
            var yPosition = 0

            placeables.forEachIndexed { index, placeable ->
                when (index % 3) {
                    0 -> {
                        val centerX = constraints.maxWidth / 2 - placeable.width / 2
                        placeable.placeRelative(x = centerX, y = yPosition)
                        yPosition += placeable.height / 2 + itemPadding
                    }
                    1 -> {
                        placeable.placeRelative(x = 0, y = yPosition)
                    }
                    2 -> {
                        val x = constraints.maxWidth - placeable.width
                        placeable.placeRelative(x = x, y = yPosition)
                        yPosition += placeable.height / 2 + itemPadding
                    }
                }
            }
        }
    }
}

