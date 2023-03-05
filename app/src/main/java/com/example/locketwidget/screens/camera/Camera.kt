package com.example.locketwidget.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.*
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locketwidget.R
import com.example.locketwidget.core.CameraSelectorSaver
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.RoundedCornerButtonWithTint
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Yellow200
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

private enum class CameraMode {
    LIVE, PHOTO, VIDEO
}

private enum class CameraProcess {
    NONE, RECORDING, LIVING
}

@Composable
private fun VideoCamera(
    preview: UseCase,
    cameraSelector: CameraSelector,
    bindCamera: (Camera) -> Unit,
    videoCapture: VideoCapture<Recorder>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(cameraSelector, preview) {
        try {
            val provider = context.getCameraProvider()
            provider.unbindAll()
            delay(500)
            bindCamera(
                provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
            )
        } catch (exc: Exception) {
            Log.e("CameraCapture", "Use case binding failed", exc)
        }
    }
}

@Composable
private fun PhotoCamera(
    previewUseCase: UseCase,
    cameraSelector: CameraSelector,
    bindCamera: (Camera) -> Unit,
    imageCaptureUseCase: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(previewUseCase, cameraSelector) {
        val cameraProvider = context.getCameraProvider()
        val imageAnalyzer = ImageAnalysis.Builder().apply {
            setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        }.build()

        try {
            cameraProvider.unbindAll()
            bindCamera(
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    imageAnalyzer,
                    imageCaptureUseCase
                )
            )
        } catch (ex: Exception) {
            Log.e("CameraCapture", "Failed to bind camera use cases", ex)
        }
    }
}

@Composable
fun BestieCamera(
    modifier: Modifier = Modifier,
    cameraCallback: CameraCallback,
    openBottomSheet: () -> Unit,
    openAudioPermissionScreen: () -> Unit
) {
    val context = LocalContext.current

    var cameraMode by rememberSaveable { mutableStateOf(CameraMode.PHOTO) }
    var previewUseCase by remember { mutableStateOf<UseCase>(Preview.Builder().build()) }
    val camera = remember { mutableStateOf<Camera?>(null) }
    val cameraSelector = rememberSaveable(saver = CameraSelectorSaver) { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var flashMode by rememberSaveable { mutableStateOf(FLASH_MODE_OFF) }

    //For photo
    val imageCaptureUseCase by remember {
        mutableStateOf(
            ImageCapture.Builder()
                .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(900, 900))
                .build()
        )
    }

    //For video
    val qualitySelector = remember { QualitySelector.from(Quality.HD) }
    val recorder = remember { Recorder.Builder().setQualitySelector(qualitySelector).setExecutor(context.executor).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var recorderUseCase by remember { mutableStateOf<Recording?>(null) }

    val scope = rememberCoroutineScope()
    val cameraProcess = remember {
        mutableStateOf(CameraProcess.NONE)
    }

    if (cameraMode == CameraMode.VIDEO) {
        VideoCamera(
            preview = previewUseCase,
            bindCamera = { camera.value = it },
            cameraSelector = cameraSelector.value,
            videoCapture = videoCapture
        )
    } else {
        PhotoCamera(
            previewUseCase = previewUseCase,
            cameraSelector = cameraSelector.value,
            bindCamera = { camera.value = it },
            imageCaptureUseCase = imageCaptureUseCase
        )
    }

    Column(modifier = modifier) {

        Spacer(modifier = Modifier.weight(1f))

        CameraPreview(
            modifier = Modifier
                .padding(top = 26.dp)
                .align(Alignment.CenterHorizontally)
                .size(LocalCameraSize.current.size),
            changeUseCase = { previewUseCase = it },
            flashMode = flashMode,
            changeFlashMode = {
                if (recorderUseCase == null) {
                    flashMode = if (flashMode == FLASH_MODE_OFF) FLASH_MODE_ON else FLASH_MODE_OFF
                }
            },
            selectMode = {
                if (it == CameraMode.VIDEO) {
                    val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    if (audioPermission != PackageManager.PERMISSION_GRANTED) {
                        openAudioPermissionScreen()
                    }
                }
                cameraMode = it
            },
            cameraMode = cameraMode,
            isFlashButtonVisible = cameraSelector.value != CameraSelector.DEFAULT_FRONT_CAMERA && cameraProcess.value != CameraProcess.RECORDING
        )

        Spacer(modifier = Modifier.weight(1f))

        CameraButtons(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            cameraProcess = cameraProcess.value,
            takePhoto = {
                when (cameraMode) {
                    CameraMode.PHOTO -> {
                        cameraCallback.takePhoto(
                            context,
                            imageCaptureUseCase.apply { this.flashMode = flashMode },
                            cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
                        )
                    }
                    CameraMode.VIDEO -> {
                        recordVideo(
                            context = context,
                            recorderUseCase = recorderUseCase,
                            camera = camera.value,
                            flashMode = flashMode,
                            videoCapture = videoCapture,
                            setRecorder = { recorderUseCase = it },
                            openAudioPermissionScreen = openAudioPermissionScreen,
                            finalizeVideoRecording = cameraCallback::finalizeVideoRecording
                        )
                        cameraProcess.value = CameraProcess.RECORDING
                    }
                    CameraMode.LIVE -> {
                        scope.launch {
                            cameraProcess.value = CameraProcess.LIVING
                            cameraCallback.takeLivePhoto(
                                context,
                                imageCaptureUseCase.apply { this.flashMode = flashMode },
                                cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
                            ) {
                                cameraSelector.swapCamera()

                                delay(2000)
                                cameraCallback.takeLivePhoto(
                                    context,
                                    imageCaptureUseCase.apply { this.flashMode = flashMode },
                                    cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
                                ) {
                                    cameraSelector.swapCamera()
                                }
                            }
                        }
                    }
                }
            },
            changeCamera = cameraSelector::swapCamera,
            showSheet = openBottomSheet,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun MutableState<CameraSelector>.swapCamera() {
    this.value = if (this.value == CameraSelector.DEFAULT_BACK_CAMERA) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
}

private fun recordVideo(
    context: Context,
    recorderUseCase: Recording?,
    camera: Camera?,
    flashMode: Int,
    videoCapture: VideoCapture<Recorder>,
    setRecorder: (Recording?) -> Unit,
    openAudioPermissionScreen: () -> Unit,
    finalizeVideoRecording: (String) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        if (recorderUseCase == null) {
            val fileOutputOptions = getFileOutputOptions(context)
            camera?.cameraControl?.enableTorch(flashMode == FLASH_MODE_ON)
            val recorder = videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .withAudioEnabled()
                .start(context.executor) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        finalizeVideoRecording(fileOutputOptions.file.path)
                    }
                }
            setRecorder(recorder)
        } else {
            recorderUseCase.stop()
            setRecorder(null)
        }
    } else {
        openAudioPermissionScreen()
    }
}

private fun getFileOutputOptions(context: Context): FileOutputOptions {
    val fileName = "${Date().time}.mp4"
    return FileOutputOptions.Builder(File(context.filesDir, fileName)).build()
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    changeUseCase: (Preview) -> Unit,
    isFlashButtonVisible: Boolean,
    flashMode: Int,
    changeFlashMode: () -> Unit,
    selectMode: (CameraMode) -> Unit,
    cameraMode: CameraMode
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .border(width = 8.dp, color = Color.LightGray, shape = RoundedCornerShape(30.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    this.scaleType = scaleType
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                changeUseCase(
                    Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                )
                previewView
            }
        )

        val iconTint = if (flashMode == FLASH_MODE_ON) Yellow200 else Color.White
        val icon = if (flashMode == FLASH_MODE_ON) R.drawable.ic_filled_flash else R.drawable.ic_flash
        if (isFlashButtonVisible) {
            RoundedCornerButtonWithTint(
                modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .size(dimensionResource(R.dimen.button_size))
                    .alpha(0.7f),
                drawableRes = icon,
                backgroundColor = Color.DarkGray,
                tint = iconTint
            ) {
                changeFlashMode()
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            val textModifier = Modifier.padding(horizontal = 16.dp)
            ModeText(
                modifier = textModifier,
                mode = stringResource(R.string.camera_live_mode),
                isSelected = cameraMode == CameraMode.LIVE,
                selectMode = { selectMode(CameraMode.LIVE) }
            )
            ModeText(
                modifier = textModifier,
                mode = stringResource(R.string.camera_photo_mode),
                isSelected = cameraMode == CameraMode.PHOTO,
                selectMode = { selectMode(CameraMode.PHOTO) }
            )
            ModeText(
                modifier = textModifier,
                mode = stringResource(R.string.camera_video_mode),
                isSelected = cameraMode == CameraMode.VIDEO,
                selectMode = { selectMode(CameraMode.VIDEO) }
            )
        }
    }
}

@Composable
private fun ModeText(modifier: Modifier, mode: String, isSelected: Boolean, selectMode: () -> Unit) {
    Text(
        text = mode,
        style = MaterialTheme.typography.h2.copy(color = if (isSelected) Orange200 else Color.LightGray),
        modifier = modifier
            .clickable {
                selectMode()
            }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CameraButtons(
    modifier: Modifier = Modifier,
    cameraProcess: CameraProcess,
    takePhoto: () -> Unit,
    changeCamera: () -> Unit,
    showSheet: () -> Unit
) {

    Box(modifier = modifier) {
        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = cameraProcess == CameraProcess.NONE,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Row {

                RoundedCornerButtonWithTint(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(dimensionResource(R.dimen.button_size)),
                    drawableRes = R.drawable.ic_focus, backgroundColor = Color.White
                ) {
                    showSheet()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 24.dp)
                        .size(dimensionResource(R.dimen.picture_button_size))
                ) {
                    GradientButton(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        takePhoto()
                        LocketAnalytics.logTakePhoto()
                    }

                }

                RoundedCornerButtonWithTint(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .align(Alignment.CenterVertically)
                        .size(dimensionResource(R.dimen.button_size)),
                    drawableRes = R.drawable.ic_rotation, backgroundColor = Color.White
                ) {
                    changeCamera()
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = cameraProcess == CameraProcess.RECORDING,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            VideoButton(modifier = Modifier.size(56.dp)) {
                takePhoto()
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = cameraProcess == CameraProcess.LIVING,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            GradientButton(modifier = Modifier.size(56.dp), action = {})
        }
    }
}

private const val VIDEO_MAX_DURATION = 15

@Composable
private fun VideoButton(
    modifier: Modifier = Modifier,
    stop: () -> Unit
) {
    val duration = remember {
        mutableStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (duration.value != VIDEO_MAX_DURATION) {
            duration.value += 1
            delay(1000)
        }
        stop()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.camera_button_corner_radius)))
            .background(color = Color.White)
            .clickable { stop() },
    ) {
        Text(
            text = "${VIDEO_MAX_DURATION - duration.value + 1}",
            style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.W700),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}