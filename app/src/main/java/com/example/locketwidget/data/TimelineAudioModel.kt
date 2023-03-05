package com.example.locketwidget.data

import androidx.annotation.RawRes
import com.example.locketwidget.R

data class TimelineAudioModel(
    @RawRes val res: Int,
    val duration: Long,
    val firstFrame: Long
)

val timelineAudio = TimelineAudioModel(R.raw.timeline_audio, 12774, 3500)