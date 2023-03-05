package com.example.locketwidget.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locketwidget.R
import com.example.locketwidget.screens.preview.SendingAnimation
import com.example.locketwidget.ui.theme.Orange250

@Composable
fun OnboardingSendPhotoScreen(
    next: () -> Unit
) {
    OnboardingBody(
        imageContent = {
            Column(modifier = Modifier.fillMaxHeight(0.5f)) {
                Image(
                    painter = painterResource(R.drawable.onboarding_image_3),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                SendingAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(start = 18.dp, end = 18.dp),
                    rawRes = R.raw.loading
                )
            }
        },
        title = getAnnotatedString(stringResource(R.string.onboarding_send_photo_title)),
        text = stringResource(R.string.onboarding_send_photo_text),
        currentScreenNumber = 2,
        buttonAction = next
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    OnboardingSendPhotoScreen {}
}

private fun getAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    val textList = text.split(" ")
    pushStyle(SpanStyle(color = Orange250, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append("${textList.first()} ")
    pop()
    pushStyle(SpanStyle(color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append(textList.last())
    pop()
    toAnnotatedString()
}