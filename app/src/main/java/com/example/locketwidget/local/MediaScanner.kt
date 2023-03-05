package com.example.locketwidget.local

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.File

class MediaScanner(context: Context) : MediaScannerConnection.MediaScannerConnectionClient {

    private var file: File? = null
    private val scanner: MediaScannerConnection by lazy { MediaScannerConnection(context, this) }

    fun setFile(file: File) {
        this.file = file
        scanner.connect()
    }

    override fun onScanCompleted(p0: String?, p1: Uri?) {
        scanner.disconnect()
    }

    override fun onMediaScannerConnected() {
        val currentFile = file
        if (currentFile != null) {
            scanner.scanFile(currentFile.absolutePath, null)
        }
    }
}