package com.example.locketwidget.data

data class ImageResponse(
    val id: String,
    val filename: String,
    val uploaded: String,
    val requireSignedURLs: Boolean,
    val variants: List<String>
)

data class ImageDeliveryResponse(
    val result: ImageResponse,
    val result_info: String?,
    val success: Boolean,
    val errors: List<String>,
    val messages: List<String>
)
