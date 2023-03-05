package com.example.locketwidget.local

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import androidx.exifinterface.media.ExifInterface
import com.example.locketwidget.R
import com.example.locketwidget.data.GradientDirection
import com.example.locketwidget.data.StrokeGradientModel
import com.example.locketwidget.data.WidgetShapes
import com.example.locketwidget.features.drawing.PathProperties
import com.example.locketwidget.screens.main.CameraOutput
import kotlin.math.roundToInt

data class WidgetTextModel(
    val isActive: Boolean = false,
    val text: String = "",
    val offsetY: Int = 0,
    val previewSize: Float = 0f,
    val rectHeight: Int = 200,
    @ColorRes val textColor: Int = R.color.white,
    @ColorRes val backgroundColor: Int = R.color.black
)

data class ImageText(
    val text: String = "",
    val previewSize: Float = 0f,
    val textSize: Int = 48,
    @ColorRes val textColor: Int = R.color.white,
)

class PhotoEditor(private val context: Context) {

    fun createLivePhoto(output: CameraOutput.Live): Bitmap? {
        val mainBitmap = getRotatedBitmap(output.mainPhotoPath, null) ?: return null
        val secondaryBitmap = getRotatedBitmap(output.secondaryPhotoPath!!, null) ?: return null
        return createLivePhoto(mainBitmap, secondaryBitmap)
    }

    fun createLivePhoto(main: Bitmap, live: Bitmap): Bitmap? {
        val workingBitmap = Bitmap.createScaledBitmap(main, main.width, main.height, true).copy(Bitmap.Config.ARGB_8888, true)
        main.recycle()

        val liveBitmap = WidgetShapes.Rectangle.toShape(live)
        live.recycle()

        val canvas = Canvas(workingBitmap)

        val liveSizeRatio = context.resources.getInteger(R.integer.live_photo_size_ratio)
        val size = workingBitmap.width / liveSizeRatio
        val padding = context.resources.getDimension(R.dimen.live_photo_padding)
        val rect = RectF(size * (liveSizeRatio - 1) - padding, padding, workingBitmap.width - padding, size + padding)
        canvas.drawBitmap(liveBitmap, null, rect, null)
        return workingBitmap
    }

    fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val cutPixels = (bitmap.height - bitmap.width) / 2
        val result = Bitmap.createBitmap(bitmap, 0, cutPixels, bitmap.width, bitmap.width)
        bitmap.recycle()
        return result
    }

    fun addCustomStroke(bitmap: Bitmap, stroke: StrokeGradientModel): Bitmap {
        val strokeWidth = context.resources.getDimension(R.dimen.widget_custom_stroke_width)
        val scaledBitmap = Bitmap.createScaledBitmap(
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888),
            bitmap.width + strokeWidth.roundToInt(), bitmap.height + strokeWidth.roundToInt(),
            true
        )
        val canvas = Canvas(scaledBitmap)
        canvas.drawBitmap(bitmap, strokeWidth / 2, strokeWidth / 2, Paint())

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (stroke.direction == GradientDirection.NONE) {
                color = stroke.colors.first()
            } else {
                shader = when (stroke.direction) {
                    GradientDirection.TOP -> LinearGradient(
                        0f,
                        0f,
                        0f,
                        scaledBitmap.height.toFloat(),
                        stroke.colors.toIntArray(),
                        null,
                        Shader.TileMode.MIRROR
                    )
                    GradientDirection.START -> LinearGradient(
                        0f,
                        0f,
                        scaledBitmap.width.toFloat(),
                        0f,
                        stroke.colors.toIntArray(),
                        null,
                        Shader.TileMode.MIRROR
                    )
                    GradientDirection.DIAGONAL -> LinearGradient(
                        0f,
                        scaledBitmap.width.toFloat(),
                        scaledBitmap.height.toFloat(),
                        0f,
                        stroke.colors.toIntArray(),
                        null,
                        Shader.TileMode.MIRROR
                    )
                    else -> error("error stroke $color")
                }
            }
            isAntiAlias = true
        }
        val radius = 50f
        val rect = RectF(
            strokeWidth / 2,
            strokeWidth / 2,
            scaledBitmap.width.toFloat() - strokeWidth / 2,
            scaledBitmap.height.toFloat() - strokeWidth / 2
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        return scaledBitmap
    }

    fun addTextOnPhoto(photoPath: String, textModel: WidgetTextModel): Bitmap? {
        val workingBitmap = getRotatedBitmap(photoPath, textModel.previewSize.roundToInt()) ?: return null

        val canvas = Canvas(workingBitmap)
        val paint = Paint()

        paint.color = ContextCompat.getColor(context, textModel.backgroundColor)
        paint.alpha = 100

        val ratio = workingBitmap.width / textModel.previewSize

        val widgetTextSize = 14f
        val textSize = context.resources.displayMetrics.density * widgetTextSize * ratio

        val rectHeight = textModel.rectHeight / 2 * ratio
        val rectTop = (textModel.offsetY * ratio + workingBitmap.height / 2) - rectHeight
        val rectBottom = (textModel.offsetY * ratio + workingBitmap.height / 2) + rectHeight

        canvas.drawRect(Rect(0, rectTop.roundToInt(), workingBitmap.width, rectBottom.roundToInt()), paint)

        paint.color = ContextCompat.getColor(context, textModel.textColor)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = textSize
        val textYPos = textModel.offsetY * ratio + workingBitmap.height / 2 + textSize / 2
        canvas.drawText(textModel.text, workingBitmap.width / 2f, textYPos, paint)

        return workingBitmap
    }

    fun addWatermark(bitmap: Bitmap): Bitmap? {
        val size = 960

        val waterMark = getBitmap(R.drawable.water_mark) ?: throw Exception()
        val scaledWatermark =
            Bitmap.createScaledBitmap(waterMark, size, (size / 4.5).roundToInt(), true)
        val waterMarkOutSize = (scaledWatermark.height * 0.6).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
        bitmap.recycle()

        val height = size + (scaledWatermark.height - waterMarkOutSize)
        val workingBitmap = Bitmap.createBitmap(
            size,
            height,
            scaledBitmap.config
        )
        val canvas = Canvas(workingBitmap)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        canvas.drawBitmap(scaledWatermark, 0f, workingBitmap.width.toFloat() - waterMarkOutSize, null)

        val paintTextSize = context.resources.displayMetrics.density * 12f

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = paintTextSize
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            context.getString(R.string.water_mark_text),
            (workingBitmap.width - context.resources.displayMetrics.density * 8),
            workingBitmap.height - waterMarkOutSize / 4f,
            paint
        )
        return workingBitmap
    }

    private fun getRotatedBitmap(photoPath: String, size: Int?): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(photoPath) ?: return null
        val bitmapSize = size ?: bitmap.width

        val ie = ExifInterface(photoPath)
        return when (ie.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateBitmap(bitmap, 90f, bitmapSize)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateBitmap(bitmap, 180f, bitmapSize)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateBitmap(bitmap, 270f, bitmapSize)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotateBitmap(bitmap, 270f, bitmapSize, true)
            }
            else -> {
                if (size != null) rotateBitmap(bitmap, 0f, bitmapSize) else bitmap
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float, newSize: Int, isPostScaled: Boolean = false): Bitmap? {
        val matrix = Matrix().apply {
            postRotate(degrees)
            if (isPostScaled) {
                postScale(-1f, 1f)
            }
        }
        val scaled = Bitmap.createScaledBitmap(bitmap, newSize, newSize, true)

        val rotateBitmap = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
        val result = rotateBitmap.copy(rotateBitmap.config, true)

        scaled.recycle()
        rotateBitmap.recycle()
        bitmap.recycle()
        return result
    }

    private fun getBitmap(drawableRes: Int): Bitmap? {
        val drawable: Drawable = ContextCompat.getDrawable(context, drawableRes)!!
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    @SuppressLint("ResourceAsColor")
    fun addTextOnImage(@DrawableRes imageId: Int, textModel: ImageText): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, imageId).scale(
            textModel.previewSize.roundToInt(),
            textModel.previewSize.roundToInt()
        )
        val canvas = Canvas(bitmap)
        val textStyle = if (Build.VERSION.SDK_INT >= 28) {
            val typefaceA = ResourcesCompat.getFont(context, R.font.poppins_bold)
            Typeface.create(typefaceA, 700, false)
        } else {
            ResourcesCompat.getFont(context, R.font.poppins_bold)
        }

        val paint = TextPaint().apply {
            color = ContextCompat.getColor(context, textModel.textColor)
            textSize = context.resources.displayMetrics.density * textModel.textSize.toFloat()
            textAlign = Paint.Align.CENTER
            textScaleX = 1.12f
            typeface = textStyle
        }

        canvas.drawMultilineText(
            text = textModel.text,
            textPaint = paint,
            width = bitmap.width -
                    (context.resources.getDimension(R.dimen.text_moment_text_padding) / context.resources.displayMetrics.density).toInt(),
            x = (bitmap.width / 2).toFloat(),
            y = bitmap.height.toFloat(),
            isProper = false
        )
        return bitmap
    }

    private fun Canvas.drawMultilineText(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        start: Int = 0,
        end: Int = text.length,
        isProper: Boolean
    ) {
        val staticLayout =
            StaticLayout.Builder.obtain(text, start, end, textPaint, width).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        if (isProper) staticLayout.draw(this, x, y)
        else {
            val padding = (y - staticLayout.getLineBottom(staticLayout.lineCount - 1)) / 2
            drawMultilineText(text = text, textPaint = textPaint, width = width, x = x, y = padding, isProper = true)
        }
    }

    private fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
        canvas.withTranslation(x, y) { draw(this) }
    }

    fun drawOnPhoto(photoPath: String?, paths: List<Pair<Path, PathProperties>>, size: Int): Bitmap? {
        val workingBitmap = if (photoPath != null) {
            getRotatedBitmap(photoPath, size)
        } else {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        } ?: return null

        val drawingBitmap = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(drawingBitmap)

        paths.forEach {

            val path = it.first.asAndroidPath()
            val property = it.second

            val paint = Paint().apply {
                if (property.eraseMode) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = property.color.toArgb()
                }
                strokeWidth = property.strokeWidth
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }

            canvas.drawPath(path, paint)
        }
        val resultCanvas = Canvas(workingBitmap)
        resultCanvas.drawBitmap(drawingBitmap, 0f, 0f, null)
        return workingBitmap
    }
}