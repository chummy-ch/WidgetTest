package com.example.locketwidget.network

import com.example.locketwidget.data.ImageDeliveryResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface ImageDeliveryService {

    @Multipart
    @POST
    suspend fun uploadPhoto(@Url url: String, @Part file: MultipartBody.Part): Response<ImageDeliveryResponse>

    @Multipart
    @POST
    suspend fun uploadVideo(@Url url: String, @Part file: MultipartBody.Part): Response<Unit>
}
