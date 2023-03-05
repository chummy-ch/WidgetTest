package com.example.locketwidget.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.locketwidget.R
import com.example.locketwidget.data.FirestoreUserResponse
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun ConnectionScreen(
    user: FirestoreUserResponse,
    friend: FirestoreUserResponse,
    popBackStack: () -> Unit
) {
    LocketScreen(systemUIColor = Orange300) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(brush = Brush.horizontalGradient(listOf(Orange300, Orange200))),
        ) {
            UserPhotos(modifier = Modifier.padding(top = 70.dp), user = user, friend = friend)

            Spacer(modifier = Modifier.weight(1f))

            Congratulations(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                name = friend.name
            )

            Spacer(modifier = Modifier.weight(1f))

            PopButton(popBackStack = popBackStack)

            Spacer(modifier = Modifier.weight(0.3f))
        }
        val partyLeft = Party(
            angle = Angle.RIGHT,
            spread = 160,
            position = Position.Relative(0.0, 0.3),
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(50)
        )
        val partyRight = Party(
            angle = Angle.LEFT,
            spread = 160,
            position = Position.Relative(1.0, 0.3),
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(50)
        )
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = listOf(partyLeft, partyRight),
        )
    }
}

@Composable
private fun PopButton(
    popBackStack: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.button_size))
            .padding(
                start = dimensionResource(R.dimen.screen_elements_padding),
                end = dimensionResource(R.dimen.screen_elements_padding),
            )
            .clip(RoundedCornerShape(16.dp)),
        onClick = { popBackStack() },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
    ) {
        Text(
            text = stringResource(R.string.connection_congratulation_button),
            style = MaterialTheme.typography.button.copy(
                fontSize = 14.sp,
                color = Orange300,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                lineHeight = 22.sp
            ),
            letterSpacing = 2.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun UserPhotos(
    modifier: Modifier = Modifier,
    user: FirestoreUserResponse,
    friend: FirestoreUserResponse
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(246.dp)
            .padding(
                start = dimensionResource(R.dimen.screen_elements_padding),
                end = dimensionResource(R.dimen.screen_elements_padding),
            )
    ) {

        Image(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center),
            painter = painterResource(R.drawable.connection_screen_background),
            contentDescription = null
        )

        UserConnectionItem(
            modifier = Modifier.align(Alignment.CenterStart),
            name = stringResource(R.string.connection_screen_you),
            photoLink = user.photoLink
        )
        UserConnectionItem(
            modifier = Modifier.align(Alignment.CenterEnd),
            name = friend.name,
            photoLink = friend.photoLink
        )
    }
}

@Composable
fun UserConnectionItem(
    modifier: Modifier = Modifier,
    name: String,
    photoLink: String?
) {
    Column(modifier = modifier.width(120.dp)) {
        Image(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(6.dp, Color.White, CircleShape),
            painter = rememberAsyncImagePainter(photoLink),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Text(
            text = name,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.h2.copy(
                color = Color.White,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun Congratulations(modifier: Modifier = Modifier, name: String) {
    val text = stringResource(R.string.connection_text)
    val spanned = buildAnnotatedString {
        append(text)
        withStyle(style = SpanStyle(color = Color.Yellow)) { append(" $name") }
        append(" \uD83C\uDF89")
    }

    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.screen_elements_padding),
                end = dimensionResource(R.dimen.screen_elements_padding)
            ),
        text = spanned,
        style = MaterialTheme.typography.h1.copy(color = Color.White, letterSpacing = 0.5.sp),
        textAlign = TextAlign.Center
    )
}