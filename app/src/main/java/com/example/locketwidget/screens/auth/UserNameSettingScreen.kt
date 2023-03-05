package com.example.locketwidget.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.getUCropIntent
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.FirestoreUserResponse
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.ui.EditNameTextField
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.yalantis.ucrop.UCrop


@Composable
fun UserSettingsScreen(
    navigate: () -> Unit
) {
    val activityViewModel: ActivityViewModel = mavericksActivityViewModel()
    val userAsync = activityViewModel.collectAsState { it.user }.value
    val user = userAsync.invoke()
    val event = activityViewModel.collectAsState { it.event }.value
    val shortNameError = stringResource(R.string.short_name_error)
    val message = remember { mutableStateOf<Event.ErrorMessage?>(null) }

    DisposableEffect(Unit) {
        onDispose { activityViewModel.clearEvent() }
    }

    LocketScreen(
        message = message.value
    ) {
        if (user != null) {
            UserSettingsScreenBody(
                user = user,
                event = event,
                createTempFile = activityViewModel::createTempFile,
                saveUser = { name, photo ->
                    activityViewModel.updateUser(name, photo)
                    navigate()
                },
                clearEvent = { activityViewModel.setEvent(null) },
                showErrorMessage = { message.value = Event.ErrorMessage(shortNameError) }
            )
        }
    }
}

@Composable
private fun UserSettingsScreenBody(
    user: FirestoreUserResponse,
    event: Event?,
    saveUser: (String, String) -> Unit,
    clearEvent: () -> Unit,
    createTempFile: () -> Unit,
    showErrorMessage: () -> Unit
) {
    val photoState = remember { mutableStateOf(if (user.photoLink.isNullOrEmpty()) FileRepository.DEFAULT_AVATAR_LINK else user.photoLink) }
    val nameState = remember { mutableStateOf(user.name) }
    val context = LocalContext.current
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data ?: return@rememberLauncherForActivityResult
        val uri = UCrop.getOutput(intent) ?: return@rememberLauncherForActivityResult
        photoState.value = uri.toString()
        clearEvent()
    }
    val photoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && event is Event.Share<*>) {
            val destination = event.data as? Uri ?: return@rememberLauncherForActivityResult
            val intent = getUCropIntent(context, uri, destination)
            cropLauncher.launch(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
    ) {
        val name = when {
            user.name.isNotEmpty() -> user.name
            else -> ""
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = name,
            style = MaterialTheme.typography.h1
        )

        PhotoPicker(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 30.dp)
                .size(120.dp),
            photo = photoState.value
        ) {
            createTempFile()
            LocketAnalytics.logUserPhotoSelect()
        }

        EditNameTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 36.dp,
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding)
                ),
            hint = stringResource(id = R.string.name_hint),
            name = nameState.value,
            changeName = { nameState.value = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        GradientButton(
            modifier = Modifier
                .padding(
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding),
                    bottom = dimensionResource(R.dimen.screen_elements_padding)
                )
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.button_size)),
            text = stringResource(R.string.continue_button)
        ) {
            if (nameState.value.length > 1) {
                saveUser(nameState.value, photoState.value)
            } else {
                showErrorMessage()
            }
        }
    }

    LaunchedEffect(event) {
        if (event is Event.Share<*>) {
            photoPicker.launch("image/*")
        }
    }
}

@Composable
fun PhotoPicker(
    modifier: Modifier = Modifier,
    photo: String,
    pickPhoto: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable { pickPhoto() },
            painter = rememberAsyncImagePainter(
                model = photo,
                placeholder = painterResource(R.drawable.ic_avatar),
                error = painterResource(R.drawable.ic_avatar)
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(brush = Brush.horizontalGradient(listOf(Orange300, Orange200)))
                .align(Alignment.BottomEnd)
                .clickable { pickPhoto() }
        ) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                tint = Color.White
            )
        }

    }
}