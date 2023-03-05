package com.example.locketwidget

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat

class AppVibrationUseCase(context: Context) {
    companion object {
        private const val DEFAULT_VIBRATION_DURATION = 250L
    }

    private val vibration = ContextCompat.getSystemService(context, Vibrator::class.java)

    fun vibrate(durationMills: Long = DEFAULT_VIBRATION_DURATION) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibration?.vibrate(VibrationEffect.createOneShot(durationMills, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibration?.vibrate(durationMills)
        }
    }
}