package com.example.locketwidget.widget

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import com.example.locketwidget.data.WidgetShapes

class WidgetTransformation(
    private val shape: WidgetShapes
) : Transformation {
    override val cacheKey: String
        get() = shape.toString()

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return shape.toShape(input)
    }
}