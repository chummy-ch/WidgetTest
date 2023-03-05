package com.example.locketwidget.features.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

enum class MotionEvent {
    Idle, Down, Move, Up
}

enum class DrawMode {
    Draw, Touch, Erase, Line
}

data class PathProperties(
    val strokeWidth: Float = 10f,
    val color: Color = Color.Red,
    val alpha: Float = 1f,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val eraseMode: Boolean = false
)