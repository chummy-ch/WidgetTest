package com.example.locketwidget.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.Result
import com.example.locketwidget.screens.camera.executor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocalStorageManager(private val context: Context) {
    companion object {
        const val TAG = "LocalStorageManager"
        const val PHOTO_EXTENSION = ".webp"
        const val AUTHORITY = "com.smartfoxlab.locket.widget"
    }

    suspend fun copyVideo(path: String): Result<String> {
        val currentFile = File(path)
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), currentFile.name)
        currentFile.copyTo(file)
        return Result.Success(file.absolutePath)
    }

    suspend fun copyImage(path: String): Result<String> {
        val fileResult = createTempFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        val file = fileResult.getOrNull() ?: return Result.Error(java.lang.NullPointerException())
        File(path).inputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return Result.Success(file.absolutePath)
    }

    fun removeFile(path: String): Result<Unit> {
        val file = File(path)
        return if (file.exists()) {
            file.delete()
            Result.Success(Unit)
        } else {
            Result.Error(NoSuchFileException(file))
        }
    }

    suspend fun createTempFileFilesDir(): kotlin.Result<File> {
        return createTempFile(context.filesDir)
    }

    private suspend fun createTempFile(dir: File?): kotlin.Result<File> {
        return kotlin.runCatching {
            val fileName = "${Date().time}"
            File.createTempFile(fileName, PHOTO_EXTENSION, dir)
        }
    }

    suspend fun pickPhoto(uri: String): Result<File> {
        val photoFileResult = createTempFileFilesDir()

        if (photoFileResult.isSuccess) {
            val file = photoFileResult.getOrNull()
            if (file != null) {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
                    if (input == null) return Result.Error(NullPointerException())

                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return Result.Success(file)
            }
            return Result.Error(FileNotFoundException())
        }
        return Result.Error(java.lang.NullPointerException())
    }

    suspend fun takePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean): Result<File> {
        val photoFileResult = createTempFileFilesDir()

        return suspendCoroutine { continuation ->
            if (photoFileResult.isSuccess) {
                val file = photoFileResult.getOrNull()
                if (file != null) {
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                        .setMetadata(ImageCapture.Metadata().apply {
                            this.isReversedHorizontal = isReversedHorizontal
                        }).build()
                    imageCapture.takePicture(
                        outputOptions,
                        context.executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                                if (LocketAnalytics.shouldLogResolution()) {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    LocketAnalytics.setCameraResolution(bitmap.width)
                                }

                                continuation.resume(Result.Success(file))
                            }

                            override fun onError(ex: ImageCaptureException) {
                                Log.e("TakePicture", "Image capture failed", ex)
                                continuation.resume(Result.Error(ex))
                            }
                        }
                    )
                } else {
                    continuation.resume(Result.Error(NullPointerException()))
                }
            } else {
                val exception =
                    Exception(photoFileResult.exceptionOrNull() ?: NullPointerException())
                continuation.resume(Result.Error(exception))
            }
        }
    }

    suspend fun savePhotoToStorage(bitmap: Bitmap, isHiddenDir: Boolean = true) =
        suspendCancellableCoroutine<Result<String>> { con ->
            val fileName = "${Date().time}$PHOTO_EXTENSION"
            val dir = if (isHiddenDir) context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(dir, fileName)

            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(imageFile)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
            } catch (e: Exception) {
                out?.close()
                con.resume(Result.Error(e))
                return@suspendCancellableCoroutine
            }
            con.resume(Result.Success(imageFile.absolutePath))
        }
}
