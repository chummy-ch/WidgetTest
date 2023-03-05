package com.example.locketwidget.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.LoggingCallback
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.example.locketwidget.ui.theme.Roboto

const val TERMS_LINK = "https://smartfox-labs.notion.site/Terms-Conditions-4ed51630f58c40c3a6499b626cb6adcd"
const val POLICY_LINK = "https://smartfox-labs.notion.site/Privacy-Policy-cd4430166b08486089343dde03b15121"

@Composable
fun AuthScreen(
    loggingCallback: LoggingCallback,
    navigateToUserSettings: () -> Unit,
) {
    val activityViewModel: ActivityViewModel = mavericksActivityViewModel()
    val userAsync = activityViewModel.collectAsState { it.user }.value

    LocketScreen(
        systemUIColor = Orange300
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(brush = Brush.horizontalGradient(listOf(Orange300, Orange200))),
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                painter = painterResource(R.drawable.auth_background),
                contentDescription = null
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.caption.copy(
                    color = Color.White,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
            when (userAsync) {
                is Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .align(Alignment.CenterHorizontally),
                        color = Color.White
                    )
                }
                else -> {
                    Column {
                        SignButton(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .height(dimensionResource(R.dimen.button_size))
                                .fillMaxWidth()
                                .padding(
                                    start = dimensionResource(R.dimen.screen_elements_padding),
                                    end = dimensionResource(R.dimen.screen_elements_padding)
                                ),
                            logo = R.drawable.ic_google_button,
                            text = stringResource(id = R.string.google_button_text),
                            singIn = loggingCallback::singInGoogle,
                            background = Color.White,
                            textColor = Color.Gray
                        )
                        SignButton(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .height(dimensionResource(R.dimen.button_size))
                                .fillMaxWidth()
                                .padding(
                                    start = dimensionResource(R.dimen.screen_elements_padding),
                                    end = dimensionResource(R.dimen.screen_elements_padding)
                                ),
                            logo = R.drawable.ic_mail,
                            text = stringResource(id = R.string.mail_button_text),
                            background = Color.Red,
                            textColor = Color.White,
                            singIn = loggingCallback::signInEmail
                        )
                    }
                }
            }
            val annotatedString = buildAnnotatedString {
                val agreement = stringResource(R.string.policy_agreement_to)
                append("$agreement ")
                val conditions = stringResource(R.string.terms_conditions)
                pushStringAnnotation(tag = "condition", annotation = TERMS_LINK)
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(conditions)
                }
                pop()

                val and = stringResource(R.string.and)
                append(" $and ")
                val policy = stringResource(R.string.privacy_policy)
                pushStringAnnotation("terms", POLICY_LINK)
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(policy)
                }
                pop()
                toAnnotatedString()
            }

            ClickableText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding),
                        bottom = 24.dp
                    ),
                text = annotatedString,
                style = MaterialTheme.typography.h2.copy(
                    fontSize = 10.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "condition", start = offset, end = offset).firstOrNull()
                        ?.let {
                            loggingCallback.browseConditionLink(it.item)
                        }
                    annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset).firstOrNull()
                        ?.let {
                            loggingCallback.browseConditionLink(it.item)
                        }
                },
            )
        }
    }


    LaunchedEffect(userAsync) {
        if (userAsync is Loading && userAsync.invoke() != null) {
            navigateToUserSettings()

            LocketAnalytics.logRegistrationFinished()
        }
    }
}

@Composable
fun SignButton(
    modifier: Modifier = Modifier,
    @DrawableRes logo: Int,
    text: String,
    background: Color,
    textColor: Color,
    singIn: () -> Unit
) {
    Button(
        onClick = { singIn() },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(backgroundColor = background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(18.dp)
                    .align(Alignment.CenterStart),
                painter = painterResource(logo),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = text,
                style = MaterialTheme.typography.h2.copy(
                    color = textColor, fontFamily = Roboto,
                    fontWeight = FontWeight.Medium
                ),
            )
        }
    }
}