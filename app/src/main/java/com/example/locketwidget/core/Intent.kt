package com.example.locketwidget.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri

fun createShareIntent(uri: Uri, title: String? = null, fileType: String): Intent? {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        clipData = ClipData(
            "Send history",
            arrayOf(fileType),
            ClipData.Item(uri)
        )
        type = fileType

    }
    return Intent.createChooser(intent, title)
}