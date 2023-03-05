package com.example.locketwidget.data

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.parcelize.Parcelize

@Parcelize
data class StrokeGradientModel(
    val colors: List<Int>,
    val direction: GradientDirection = GradientDirection.NONE
) : Parcelable

enum class GradientDirection {
    NONE, START, TOP, DIAGONAL
}

val WidgetStrokeColors = listOf(
    StrokeGradientModel(listOf(Color(0xff52acff).toArgb(), Color(0xffFFE32C).toArgb()), GradientDirection.TOP),
    StrokeGradientModel(listOf(Color(0xffEF6B60).toArgb(), Color(0xffFF8F42).toArgb()), GradientDirection.START),
    StrokeGradientModel(
        listOf(Color(0xff4158D0).toArgb(), Color(0xffC850C0).toArgb(), Color(0xffFFCC70).toArgb()),
        GradientDirection.DIAGONAL
    ),
    StrokeGradientModel(listOf(Color(0xff21D4FD).toArgb(), Color(0xffB721FF).toArgb()), GradientDirection.DIAGONAL),
    StrokeGradientModel(
        listOf(
            Color(0xffFA8BFF).toArgb(), Color(0xff2BD2FF).toArgb(), Color(0xff2BFF88).toArgb(),
        ),
        GradientDirection.DIAGONAL
    ),
    StrokeGradientModel(listOf(Color(0xff85FFBD).toArgb(), Color(0xffFFFB7D).toArgb()), GradientDirection.DIAGONAL),
    StrokeGradientModel(listOf(Color(0xffA9C9FF).toArgb(), Color(0xffFFBBEC).toArgb()), GradientDirection.TOP),
    StrokeGradientModel(listOf(Color(0xffFBDA61).toArgb(), Color(0xffFF5ACD).toArgb()), GradientDirection.DIAGONAL),
    StrokeGradientModel(listOf(Color(0xffFAD961).toArgb(), Color(0xffF76B1C).toArgb()), GradientDirection.START),
    StrokeGradientModel(listOf(Color(0xff08AEEA).toArgb(), Color(0xff2AF598).toArgb()), GradientDirection.TOP),
    StrokeGradientModel(listOf(Color(0xffFBDA61).toArgb(), Color(0xffFF5ACD).toArgb()), GradientDirection.DIAGONAL),
)