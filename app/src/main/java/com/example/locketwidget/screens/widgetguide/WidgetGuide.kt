package com.example.locketwidget.screens.widgetguide

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.locketwidget.R
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.widgetmanager.addWidget
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.OnboardingIndicators
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.example.locketwidget.ui.theme.White200
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager

private const val SCREENS_AMOUNT = 3

private data class GuideModel(
    @StringRes val title: Int,
    @DrawableRes val image: Int,
    @StringRes val subtitle: Int,
    @StringRes val buttonText: Int? = null
)

private val screens = listOf(
    GuideModel(R.string.widget_guide_title, R.drawable.widget_guide_1, R.string.widget_guide_subtitle_1),
    GuideModel(R.string.widget_guide_title, R.drawable.widget_guide_2, R.string.widget_guide_subtitle_2),
    GuideModel(
        R.string.widget_guide_title,
        R.drawable.widget_guide_3,
        R.string.widget_guide_subtitle_3,
        R.string.widget_guide_add_widget
    )
)

@Composable
fun WidgetSettingsGuideScreen(navController: NavController) {
    WidgetGuideScreen(closeAction = { navController.popBackStack() }, isOnboarding = false)
}

@Composable
fun WidgetOnboardingGuideScreen(navController: NavController) {
    WidgetGuideScreen(
        closeAction = {
            navController.popBackStack()
            navController.navigate(ScreenItem.Invitation.route)
            navController.navigate(ScreenItem.Premium.route)
        },
        isOnboarding = true
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WidgetGuideScreen(
    closeAction: () -> Unit,
    isOnboarding: Boolean,
) {
    LocketScreen(systemUIColor = Orange300) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.horizontalGradient(listOf(Orange300, Orange200)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 22.dp, top = 22.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.CenterEnd)
                        .clickable { closeAction() },
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            HorizontalPager(modifier = Modifier.padding(top = 22.dp), count = SCREENS_AMOUNT) { page ->
                with(screens[page]) {
                    WidgetGuideBody(
                        title = stringResource(title),
                        image = painterResource(image),
                        subtitle = stringResource(subtitle),
                        buttonText = buttonText?.let { text -> stringResource(text) },
                        selectedScreen = page,
                        popBackStack = closeAction,
                        isOnboarding = isOnboarding
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetGuideBody(
    modifier: Modifier = Modifier,
    title: String,
    image: Painter,
    subtitle: String,
    buttonText: String?,
    selectedScreen: Int,
    isOnboarding: Boolean,
    popBackStack: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp),
            text = title,
            style = MaterialTheme.typography.h1.copy(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Image(modifier = Modifier.align(Alignment.CenterHorizontally), painter = image, contentDescription = null)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp),
            text = subtitle,
            style = MaterialTheme.typography.h3.copy(
                color = White200,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            ),
            letterSpacing = 1.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )

        if (selectedScreen == 2 && buttonText != null) {
            val context = LocalContext.current
            Button(
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 32.dp)
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius))),
                onClick = {
                    if (addWidget(context) && isOnboarding) {
                        popBackStack()
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.button.copy(color = Color.Black, fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        OnboardingIndicators(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            amount = SCREENS_AMOUNT,
            selected = selectedScreen,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}