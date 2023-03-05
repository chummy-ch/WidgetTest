package com.example.locketwidget.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locketwidget.R
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.OnboardingIndicators
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun OnboardingBody(
    imageContent: @Composable () -> Unit,
    title: AnnotatedString,
    text: String,
    currentScreenNumber: Int,
    isBackButtonRemoved: Boolean = true,
    buttonAction: () -> Unit
) {
    if (isBackButtonRemoved) BackHandler {}

    LocketScreen(systemUIColor = Orange300) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            imageContent()

            OnboardingIndicators(
                modifier = Modifier.padding(top = 24.dp),
                amount = ONBOARDING_SCREENS_COUNT,
                selected = currentScreenNumber
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                modifier = Modifier.padding(top = 24.dp),
                lineHeight = 34.sp,
                style = MaterialTheme.typography.h1.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    letterSpacing = 0.5.sp
                ),
                letterSpacing = 0.5.sp
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 12.dp,
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding)
                    ),
                text = text,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                style = MaterialTheme.typography.h2.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.W500
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            GradientButton(
                modifier = Modifier
                    .padding(
                        bottom = dimensionResource(R.dimen.screen_elements_padding),
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding)
                    )
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_size)),
                text = if (currentScreenNumber == 0) stringResource(R.string.onboarding_first_screen_button_text)
                else stringResource(R.string.onboarding_next_button_text),
                style = MaterialTheme.typography.button.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 2.sp
                )
            ) {
                buttonAction()
            }
        }
    }
}