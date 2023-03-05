package com.example.locketwidget.screens.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalActivity
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.theme.Orange250
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun AudioPermissionScreen(
    close: () -> Unit
) {
    BackHandler {}

    val context = LocalContext.current
    val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    val isPermissionGranted = remember {
        mutableStateOf<Boolean?>(
            if (audioPermission == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                null
            }
        )
    }
    val activity = LocalActivity.current
    val shouldShowRationale = remember {
        mutableStateOf(activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO))
    }
    val shouldOpenSettings by derivedStateOf {
        isPermissionGranted.value == shouldShowRationale.value
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        isPermissionGranted.value = result
        shouldShowRationale.value = activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
    }
    val appSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isPermissionGranted.value = true
        }
    }

    LaunchedEffect(isPermissionGranted.value) {

        if (isPermissionGranted.value == true) {
            close()
        }
    }

    LocketScreen(systemUIColor = Orange300) {

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))

            Image(painter = painterResource(R.drawable.ic_onboarding_camera_permission), contentDescription = null)
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = getAnnotatedString(stringResource(R.string.audio_permission_title)),
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
                text = stringResource(R.string.audio_permission_text),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                style = MaterialTheme.typography.h2.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.W500
                )
            )
            Spacer(modifier = Modifier.weight(1f))

            PermissionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding),
                        bottom = 32.dp
                    ),
                shouldOpenSettings = shouldOpenSettings
            ) {
                if (shouldOpenSettings) {
                    appSettingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        val uri = Uri.fromParts("package", context.packageName, null)
                        data = uri
                    })
                } else {
                    permissionRequest.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
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