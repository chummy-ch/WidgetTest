package com.example.locketwidget.core

import androidx.activity.ComponentActivity
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    noLocalProvidedFor("LocalActivity")
}

val LocalPhotoScope = compositionLocalOf<CustomLifecycleScope> {
    noLocalProvidedFor("LocalPhotoScope")
}

val LocalTimelineScope = compositionLocalOf<CustomLifecycleScope> {
    noLocalProvidedFor("LocalTimelineScope")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}