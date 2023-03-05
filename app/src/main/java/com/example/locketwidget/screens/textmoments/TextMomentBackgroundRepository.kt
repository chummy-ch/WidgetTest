package com.example.locketwidget.screens.textmoments

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import com.example.locketwidget.R

data class TextMomentBackground(
    @IdRes val backgroundId: Int,
    val textColor: Int
)

class TextMomentBackgroundRepository {
    @SuppressLint("ResourceType")
    val patternIds = listOf(
        TextMomentBackground(R.drawable.text_background_1, R.color.white),
        TextMomentBackground(R.drawable.text_background_2, R.color.white),
        TextMomentBackground(R.drawable.text_background_3, R.color.white),
        TextMomentBackground(R.drawable.text_background_4, R.color.white),
        TextMomentBackground(R.drawable.text_background_5, R.color.white),
        TextMomentBackground(R.drawable.text_background_6, R.color.black),
        TextMomentBackground(R.drawable.text_background_7, R.color.white),
        TextMomentBackground(R.drawable.text_background_8, R.color.white),
        TextMomentBackground(R.drawable.text_background_9, R.color.white),
        TextMomentBackground(R.drawable.text_background_10, R.color.white),
        TextMomentBackground(R.drawable.text_background_11, R.color.white),
        TextMomentBackground(R.drawable.text_background_12, R.color.white),
        TextMomentBackground(R.drawable.text_background_13, R.color.white),
        TextMomentBackground(R.drawable.text_background_14, R.color.white),
        TextMomentBackground(R.drawable.text_background_15, R.color.white),
        TextMomentBackground(R.drawable.text_background_16, R.color.white),
        TextMomentBackground(R.drawable.text_background_17, R.color.white),
        TextMomentBackground(R.drawable.text_background_18, R.color.black),
        TextMomentBackground(R.drawable.text_background_19, R.color.black),
        TextMomentBackground(R.drawable.text_background_20, R.color.white),
        TextMomentBackground(R.drawable.text_background_21, R.color.white),
        TextMomentBackground(R.drawable.text_background_22, R.color.white),
        TextMomentBackground(R.drawable.text_background_23, R.color.white),
        TextMomentBackground(R.drawable.text_background_24, R.color.white),
        TextMomentBackground(R.drawable.text_background_25, R.color.black),
        TextMomentBackground(R.drawable.text_background_26, R.color.white),
        TextMomentBackground(R.drawable.text_background_27, R.color.white),
        TextMomentBackground(R.drawable.text_background_28, R.color.white),
        TextMomentBackground(R.drawable.text_background_29, R.color.white),
        TextMomentBackground(R.drawable.text_background_30, R.color.black),
        TextMomentBackground(R.drawable.text_background_31, R.color.white),
        TextMomentBackground(R.drawable.text_background_32, R.color.white),
        TextMomentBackground(R.drawable.text_background_33, R.color.white),
        TextMomentBackground(R.drawable.text_background_34, R.color.white),
        TextMomentBackground(R.drawable.text_background_35, R.color.white),
        TextMomentBackground(R.drawable.text_background_36, R.color.white)
    )
}