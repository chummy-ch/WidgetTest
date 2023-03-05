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

const val ONBOARDING_SCREENS_COUNT = 5

@Composable
fun OnboardingAddFriendsScreen(
    next: () -> Unit
) {
    OnboardingBody(
        imageContent = {
            Box(modifier = Modifier.fillMaxHeight(0.5f)) {
                Image(
                    painter = painterResource(R.drawable.onboarding_image_1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        },
        title = getAnnotatedString(stringResource(R.string.onboarding_add_friends_title)),
        text = stringResource(R.string.onboaring_add_friends_text),
        currentScreenNumber = 0,
        buttonAction = next,
        isBackButtonRemoved = false
    )
}

private fun getAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    val textList = text.split(" ")
    pushStyle(SpanStyle(color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append("${textList[0]} ${textList[1]} ")
    pop()
    pushStyle(SpanStyle(color = Orange250, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append(textList.last())
    pop()
    toAnnotatedString()
}

@Preview(showBackground = true)
@Composable
private fun ScreenPreview() {
    OnboardingAddFriendsScreen {}
}