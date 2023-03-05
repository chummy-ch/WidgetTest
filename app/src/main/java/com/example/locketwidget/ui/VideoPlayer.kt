package com.example.locketwidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.locketwidget.R
import kotlinx.coroutines.delay

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    mediaSource: MediaSource,
    thumb: String? = null,
    isPlaying: Boolean,
    play: () -> Unit,
    pause: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val isReady = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                    isReady.value = true
                }
            }
        })
    }

    var isPLayButtonHidden by remember { mutableStateOf(false) }
    val imageRes = remember(isPlaying) {
        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_start
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.photo_drawing_corner_radius)))
            .clickable {
                isPLayButtonHidden = false
                if (isPlaying) pause() else play()
            }
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    player = exoPlayer.apply { seekTo(1) }
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    exoPlayer.addListener(object : Player.Listener {

                        override fun onIsPlayingChanged(isExoPlaing: Boolean) {
                            super.onIsPlayingChanged(isExoPlaing)
                            if (!isExoPlaing) {
                                isPLayButtonHidden = false
                                pause()
                            }
                        }
                    })
                }
            }
        )

        if (!isReady.value) {
            Image(painter = rememberAsyncImagePainter(thumb), contentDescription = null, modifier = Modifier.fillMaxSize())
        }

        if (!isPLayButtonHidden || !isPlaying) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(dimensionResource(R.dimen.button_size)),
                painter = painterResource(imageRes),
                contentDescription = null
            )
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && !exoPlayer.isPlaying) {
            if (exoPlayer.totalBufferedDuration == 0L) {
                exoPlayer.seekTo(0)
            } else {
                exoPlayer.play()
            }
        } else if (!isPlaying && exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        delay(250)
        isPLayButtonHidden = true
    }
}