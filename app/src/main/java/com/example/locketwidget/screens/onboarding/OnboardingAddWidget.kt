package com.example.locketwidget.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.sp
import com.example.locketwidget.R
import com.example.locketwidget.ui.theme.Orange250

@Composable
fun OnboardingAddWidgetScreen(
    next: () -> Unit
) {
    OnboardingBody(
        imageContent = {
            Box(modifier = Modifier.fillMaxHeight(0.5f)) {
                Image(
                    painter = painterResource(R.drawable.onboarding_image_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        },
        title = getAnnotatedString(stringResource(R.string.onboarding_add_widget_title)),
        text = stringResource(R.string.onboarding_add_widget_text),
        currentScreenNumber = 1,
        buttonAction = next
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    OnboardingAddWidgetScreen {}
}

private fun getAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    val textList = text.split(" ")
    pushStyle(SpanStyle(color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append("${textList.first()} ")
    pop()
    pushStyle(SpanStyle(color = Orange250, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append(textList.last())
    pop()
    toAnnotatedString()
}