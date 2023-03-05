package com.example.locketwidget.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity

private const val MAX_SIZE = 1024
private const val RATIO = 9f

fun getUCropIntent(context: Context, source: Uri, destination: Uri) = Intent().apply {
    setClass(context, UCropActivity::class.java)
    putExtras(Bundle().apply {
        putFloat(UCrop.EXTRA_ASPECT_RATIO_X, RATIO)
        putFloat(UCrop.EXTRA_ASPECT_RATIO_Y, RATIO)
        putInt(UCrop.EXTRA_MAX_SIZE_X, MAX_SIZE)
        putInt(UCrop.EXTRA_MAX_SIZE_Y, MAX_SIZE)
        putParcelable(UCrop.EXTRA_INPUT_URI, source)
        putParcelable(UCrop.EXTRA_OUTPUT_URI, destination)
    })
}