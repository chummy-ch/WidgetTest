package com.example.locketwidget.network

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.locketwidget.BuildConfig
import com.example.locketwidget.core.Result
import com.example.locketwidget.core.onDefault
import com.example.locketwidget.core.onIO
import com.example.locketwidget.data.ImageDeliveryResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
class FileRepository(private val client: RetrofitClient, private val context: Context) {
    companion object {
        const val TAG = "FileRepository"
        private const val BASE_PHOTO_LINK = "hidden"
        private const val LINK_PRIVACY = "/original"
        const val DEFAULT_AVATAR_LINK = "hidden"
        const val IMAGE_EXTENSION = ".webp"
        const val AUDIO_EXTENSION = ".mp3"
        const val VIDEO_EXTENSION = ".mp4"

        enum class MediaType(val type: String) {
            IMAGE("image"), VIDEO("video/*")
        }

        fun getPhotoLinkById(id: String) = "$BASE_PHOTO_LINK$id$LINK_PRIVACY"

        fun getPhotoThumbLinkById(id: String) = "$BASE_PHOTO_LINK$id/thumb"

        fun getVideoLinkById(id: String) = "https://cloudflarestream.com/$id/manifest/video.m3u8"
    }

    suspend fun uploadPhoto(url: String, file: File): Result<ImageDeliveryResponse> {
        return onIO {
            val service = client.getImageDeliveryService()

            val requestFile = file.asRequestBody("image".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = service.uploadPhoto(url, body)
            if (response.code() == 200) {
                val deliveryResponse = response.body() ?: return@onIO Result.Error(NullPointerException("Response error"))
                return@onIO Result.Success(deliveryResponse)
            } else {
                return@onIO Result.Error(Exception(response.message()))
            }
        }
    }

    suspend fun uploadPhoto(url: String, path: String): Result<ImageDeliveryResponse> {
        return onDefault {
            val file = File(path)
            val service = client.getImageDeliveryService()
            val requestFile = file.asRequestBody(MediaType.IMAGE.type.toMediaTypeOrNull())

            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            try {
                val response = service.uploadPhoto(url, body)
                if (response.code() == 200) {
                    val deliveryResponse = response.body() ?: return@onDefault Result.Error(NullPointerException("Response error"))
                    return@onDefault Result.Success(deliveryResponse)
                } else {
                    return@onDefault Result.Error(Exception(response.message()))
                }
            } catch (timeout: Exception) {
                return@onDefault Result.Error(timeout)
            }
        }
    }

    suspend fun uploadVideo(url: String, path: String): Result<Unit> {
        return onDefault {
            val file = File(path)
            val service = client.getImageDeliveryService()
            val requestFile = file.asRequestBody(MediaType.VIDEO.type.toMediaTypeOrNull())

            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            try {
                val response = service.uploadVideo(url, body)
                if (response.code() == 200) {
                    return@onDefault Result.Success(Unit)
                } else {
                    return@onDefault Result.Error(Exception(response.message()))
                }
            } catch (timeout: Exception) {
                return@onDefault Result.Error(timeout)
            }
        }
    }

    suspend fun getBitmap(photoId: String): Bitmap? {
        return onIO {
            URL(getPhotoLinkById(photoId)).openStream().use {
                return@onIO BitmapFactory.decodeStream(it)
            }
        }
    }

    suspend fun saveBitmapToLocalDirGetUri(bitmap: Bitmap): Uri? {
        val path = saveBitmapToDir(bitmap, context.filesDir)
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(path))
    }

    suspend fun saveBitmapToLocalDir(bitmap: Bitmap): String {
        return saveBitmapToDir(bitmap, context.filesDir)
    }

    suspend fun saveBitmapToDir(bitmap: Bitmap, dir: File): String {
        return onIO {
            val file = File(dir, "${Date().time}$IMAGE_EXTENSION")
            FileOutputStream(file).use { stream ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
                }
                return@onIO file.absolutePath
            }
        }
    }

    suspend fun downloadFile(link: String, name: String, dir: File = context.filesDir, fileExtension: String): String? {
        return onDefault {
            val file = File(dir, "$name$fileExtension")
            if (!file.exists()) {
                try {
                    URL(link).openStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "error = ${e} with video link = $link")
                    return@onDefault null
                }
            }
            return@onDefault file.path
        }
    }

    suspend fun downloadPhotoToDirById(photoId: String, dir: File = context.filesDir): String? {
        return onIO {
            val file = downloadPhoto(dir, photoId) ?: return@onIO null
            return@onIO file.path
        }
    }

    private suspend fun downloadImage(link: String, name: String, dir: File): File? {
        val file = File(dir, "$name$IMAGE_EXTENSION")
        if (!file.exists()) {
            try {
                URL(link).openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "error = ${e} with photo = $link")
                return null
            }
        }
        return file
    }

    private suspend fun downloadPhoto(dir: File, photoId: String): File? {
        val name = photoId.substring(0, 5)
        return downloadImage(getPhotoLinkById(photoId), name, dir)
    }

    private fun downloadPhotoWithManager(url: String, photoId: String) {
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(false)
            .setTitle(photoId)
            .setMimeType("image/webp")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES,
                File.separator + photoId + ".webp"
            )
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}
