package com.example.locketwidget.data

interface NavigationType

data class PhotoPreviewType(val path: String, val emojis: List<String>? = null) : NavigationType