package com.example.locketwidget.core

import androidx.camera.core.CameraSelector
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver


val CameraSelectorSaver = Saver<MutableState<CameraSelector>, Boolean>(
    save = { it.value == CameraSelector.DEFAULT_FRONT_CAMERA },
    restore = {
        mutableStateOf(
            if (it) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
        )
    }
)