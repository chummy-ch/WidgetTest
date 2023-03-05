package com.example.locketwidget.screens.camera

import android.content.Context
import androidx.camera.core.ImageCapture

interface CameraCallback {

    fun takePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean)

    suspend fun takeLivePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean, onFinish: suspend () -> Unit)

    fun pickPhotoFromGallery()

    fun finalizeVideoRecording(path: String)
}
