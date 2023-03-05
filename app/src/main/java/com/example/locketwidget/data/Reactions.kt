package com.example.locketwidget.data

import androidx.annotation.RawRes
import androidx.compose.ui.unit.dp
import com.example.locketwidget.R
import com.google.firebase.firestore.DocumentReference

data class ReactionResponse(
    val emoji: String,
    val sender: DocumentReference,
)

data class ReactionModel(
    val emoji: String,
    val sender: FirestoreUserResponse
)

data class ReactionAnimationModel(
    val name: String,
    @RawRes val rawRes: Int,
    val IsPremium: Boolean
)

object Reactions {
    const val ANIMATION_DURATION_MILLS = 250L
    const val REACTION_DURATION_MILLS = 3000L
    val START_SIZE_ANIMATION = 50.dp
    val END_SIZE_ANIMATION = 100.dp

    val reactions = listOf(
        ReactionAnimationModel("up", R.raw.up, false),
        ReactionAnimationModel("down", R.raw.down, false),
        ReactionAnimationModel("love", R.raw.love, false),
        ReactionAnimationModel("fire", R.raw.fire, false),
        ReactionAnimationModel("cry", R.raw.cry, false),
        ReactionAnimationModel("shock", R.raw.shock, false),
        ReactionAnimationModel("pigeon", R.raw.pigeon, true),
        ReactionAnimationModel("zevaet", R.raw.zevaet, true),
        ReactionAnimationModel("laugh", R.raw.laugh, true),
        ReactionAnimationModel("poop", R.raw.poop, true),
        ReactionAnimationModel("star", R.raw.star, true),
        ReactionAnimationModel("boomstick", R.raw.boomstick, true),
    )

    fun getReaction(emoji: String): Int? {
        return reactions.firstOrNull { it.name == emoji }?.rawRes
    }
}