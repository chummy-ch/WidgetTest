package com.example.locketwidget.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.locketwidget.R

val Poppins = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_regular, FontWeight.W400),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)
val Roboto = FontFamily(
    Font(R.font.roboto_medium, FontWeight.Medium)
)

val Typography = Typography(
    body1 = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    h1 = TextStyle(
        fontSize = 24.sp,
        color = Black400,
        fontWeight = FontWeight.Bold,
        fontFamily = Poppins
    ),
    h2 = TextStyle(
        fontSize = 14.sp,
        color = Black400,
        fontFamily = Poppins
    ),
    h3 = TextStyle(
        fontSize = 12.sp,
        color = Black400,
        fontFamily = Poppins
    ),
    caption = TextStyle(
        fontSize = 24.sp,
        color = Black400,
        fontWeight = FontWeight.Bold,
        fontFamily = Poppins
    ),
    button = TextStyle(
        fontSize = 16.sp,
        color = Color.White,
        letterSpacing = 2.sp,
        fontFeatureSettings = "c2sc, smcp",
        fontWeight = FontWeight.Bold,
        fontFamily = Poppins
    ),
    subtitle1 = TextStyle(
        fontSize = 48.sp,
        color = White200,
        fontWeight = FontWeight.Bold,
        fontFamily = Poppins
    )
)