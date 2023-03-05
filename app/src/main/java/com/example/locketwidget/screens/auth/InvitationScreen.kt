package com.example.locketwidget.screens.auth

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.GradientImageButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun InvitationScreen(
    popBackStack: () -> Unit
) {
    val viewModel: InvitationViewModel = mavericksViewModel()
    val linkAsync = viewModel.collectAsState { it.link }.value
    val linkString = if (linkAsync is Success) linkAsync.invoke().toString()
    else stringResource(R.string.loading_deep_link_message)
    val shareLinkResult =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            LocketAnalytics.logFriendLinkCopy()
        }
    val context = LocalContext.current

    LocketScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 100.dp,
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding)
                )
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.invitation_screen_title),
                style = MaterialTheme.typography.h1
            )
            Text(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.invitation_screen_subtitle),
                style = MaterialTheme.typography.h2.copy(lineHeight = 22.sp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth(0.8f)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Orange200, Orange300)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .wrapContentHeight(Alignment.CenterVertically),
                    text = linkString,
                    style = MaterialTheme.typography.h2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(16.dp))
                GradientImageButton(
                    modifier = Modifier.size(dimensionResource(R.dimen.button_size)),
                    drawableRes = R.drawable.ic_share
                ) {
                    if (linkAsync is Success) {
                        shareLinkResult.launch(Intent().apply {
                            val text = context.getString(R.string.share_link_text, linkAsync.invoke())
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(
                modifier = Modifier
                    .padding(
                        bottom = dimensionResource(R.dimen.screen_elements_padding)
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.continue_button)
            ) {
                popBackStack()
            }

        }
    }
}