package com.example.emoji_mashup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class EmojiMashupUtil(private val context: Context) {
    companion object {
        private const val EMOJI_RESOLUTION = 640
    }

    fun getEmojiBitmap(emojiModel: EmojiModel, resolution: Int = EMOJI_RESOLUTION): Bitmap? {
        val base = ContextCompat.getDrawable(context, emojiModel.base) ?: return null
        val eyes = ContextCompat.getDrawable(context, emojiModel.eyes) ?: return null
        val mouth = ContextCompat.getDrawable(context, emojiModel.mouth) ?: return null
        val feature = emojiModel.feature?.let { ContextCompat.getDrawable(context, it) }

        val workingBitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(workingBitmap)

        base.drawOnCanvas(canvas)
        eyes.drawOnCanvas(canvas)
        mouth.drawOnCanvas(canvas)
        feature?.drawOnCanvas(canvas)

        return workingBitmap
    }

    private fun Drawable.drawOnCanvas(canvas: Canvas) {
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }
}

fun EmojiModel.mashup(emojiModel: EmojiModel): EmojiModel {
    return copy(base = emojiModel.base, feature = emojiModel.feature ?: this.feature)
}