package com.example.locketwidget.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.locketwidget.data.ReactionAnimationModel
import com.example.locketwidget.data.Reactions

enum class MultiFabState {
    COLLAPSED, EXPANDED
}

@Composable
fun FloatingDialogActionMenu(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = MutableInteractionSource(), indication = null) { onDismiss() }
        ) {
            FloatingActionMenu(modifier = modifier, content = content)
        }
    }
}


@Composable
fun FloatingActionMenu(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color = MaterialTheme.colors.secondary),
        content = content
    )
}

@Composable
fun FloatingReactionActionMenu(
    modifier: Modifier = Modifier,
    select: (ReactionAnimationModel) -> Unit
) {
    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color = MaterialTheme.colors.secondary),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(Reactions.reactions) { reaction ->
            ReactionAnimation(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { select(reaction) }
                    .padding(4.dp),
                animation = reaction.rawRes
            )
        }
    }
}

@Composable
fun ReactionAnimation(
    modifier: Modifier = Modifier,
    @RawRes animation: Int
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
    )
    LottieAnimation(
        composition = composition,
        modifier = modifier,
        progress = progress
    )
}


@Composable
fun ReactionItem(
    modifier: Modifier = Modifier,
    senderPhoto: String?,
    animation: Int
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(20.dp)),
            painter = rememberAsyncImagePainter(senderPhoto),
            contentDescription = null
        )
        ReactionAnimation(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(20.dp)),
            animation = animation
        )
    }
}