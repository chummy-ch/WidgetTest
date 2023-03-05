package com.example.locketwidget.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.locketwidget.R
import kotlinx.parcelize.Parcelize

interface WidgetStyle {
    companion object {
        fun getList(): List<WidgetStyle> {
            return (WidgetShapes.getShapeList() as List<WidgetStyle>).plus(
                listOf(
                    WidgetForeground(
                        asset = "rainbow",
                        frames = 23,
                        preview = R.drawable.rainbow_animation,
                        interval = 80,
                        isPremium = false,
                        padding = 20
                    ),
                    WidgetForeground(
                        asset = "heart",
                        frames = 12,
                        preview = R.drawable.heart_animation,
                        interval = 100,
                        isPremium = false,
                        padding = 0
                    ),
                    WidgetForeground(
                        asset = "hearts",
                        frames = 1,
                        preview = R.drawable.hearts_animation,
                        interval = 500,
                        isPremium = true,
                        padding = 10
                    ),
                    WidgetForeground(
                        asset = "pow",
                        frames = 29,
                        preview = R.drawable.pow_animation,
                        interval = 30,
                        isPremium = true,
                        padding = 14
                    ),
                    WidgetForeground(
                        asset = "likes",
                        frames = 12,
                        preview = R.drawable.likes_animation,
                        interval = 100,
                        padding = 0,
                        isPremium = true
                    ),
                    WidgetForeground(
                        asset = "blue_hearts",
                        frames = 36,
                        preview = R.drawable.hearts_blue_animation,
                        interval = 30,
                        padding = 20,
                        isPremium = true
                    ),
                    WidgetForeground(
                        asset = "water_pixels",
                        frames = 6,
                        preview = R.drawable.water_pixel_animation,
                        interval = 60,
                        padding = 14,
                        isPremium = true
                    )
                )
            )
        }
    }

    val isPremium: Boolean
}

@Parcelize
data class WidgetForeground(
    val asset: String,
    val frames: Int,
    @DrawableRes val preview: Int,
    val interval: Int,
    override val isPremium: Boolean,
    val padding: Int
) : WidgetStyle, Parcelable

@Parcelize
data class WidgetShape(
    val shape: WidgetShapes,
    override val isPremium: Boolean,
    val stroke: StrokeGradientModel = StrokeGradientModel(listOf(Color.Transparent.toArgb()), GradientDirection.NONE)
) : WidgetStyle, Parcelable

interface WidgetShaper {
    fun toShape(image: Bitmap): Bitmap
}

enum class WidgetShapes(@DrawableRes val backgroundRes: Int) : WidgetShaper {

    Rectangle(backgroundRes = R.drawable.shape_rectangle_background) {
        override fun toShape(image: Bitmap): Bitmap {
            val width = image.width
            val height = image.height

            val bitmap = Bitmap.createBitmap(
                width, // width in pixels
                height, // height in pixels
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            // path to draw rounded corners bitmap
            val path = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    Path.Direction.CCW
                )
            }
            canvas.clipPath(path)

            // draw the rounded corners bitmap on canvas
            canvas.drawBitmap(image, 0f, 0f, null)
            return bitmap
        }
    },

    // TODO: Fix shape
    Heart(backgroundRes = R.drawable.shape_heart_background) {
        override fun toShape(image: Bitmap): Bitmap {
            val width = image.width
            val height = image.height

            val bitmap = Bitmap.createBitmap(
                width, // width in pixels
                height, // height in pixels
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val path = Path().apply {
                moveTo(width / 2f, height / 5f)
                cubicTo(
                    5f * width / 14, 0f,
                    0f, height / 15f,
                    width / 28f, 2 * height / 5f
                )
                cubicTo(
                    width / 14f, 2f * height / 3,
                    3 * width / 7f, 5f * height / 6,
                    width / 2f, height.toFloat()
                )
                cubicTo(
                    4f * width / 7, 5f * height / 6,
                    13f * width / 14, 2f * height / 3,
                    27f * width / 28, 2f * height / 5
                )
                cubicTo(
                    width.toFloat(), height / 15f,
                    9 * width / 14f, 0f,
                    width / 2f, height / 5f
                )
            }
            canvas.clipPath(path)

            // draw the rounded corners bitmap on canvas
            canvas.drawBitmap(image, 0f, 0f, null)
            return bitmap
        }
    },

    // TODO: Fix shape
    Hexagon(backgroundRes = R.drawable.shape_hexagon_background) {
        override fun toShape(image: Bitmap): Bitmap {
            val width = image.width
            val height = image.height

            val bitmap = Bitmap.createBitmap(
                width, // width in pixels
                height, // height in pixels
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val path = Path().apply {
                moveTo(0f, height / 4f)
                lineTo(width / 2f, 0f)
                lineTo(width.toFloat(), height / 4f)
                lineTo(width.toFloat(), height * 0.75f)
                lineTo(width / 2f, height.toFloat())
                lineTo(0f, height * 0.75f)
            }
            canvas.clipPath(path)

            // draw the rounded corners bitmap on canvas
            canvas.drawBitmap(image, 0f, 0f, null)
            return bitmap
        }
    };

    companion object {
        private const val CORNER_RADIUS = 50f

        fun getShapeList(): List<WidgetShape> {
            return listOf(
                WidgetShape(Rectangle, false), WidgetShape(Heart, false), WidgetShape(Hexagon, false)
            )
        }
    }
}