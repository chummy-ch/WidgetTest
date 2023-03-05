package com.example.emoji_mashup

import androidx.annotation.DrawableRes


data class EmojiModel(
    val unicode: String,
    @DrawableRes val base: Int,
    @DrawableRes val eyes: Int,
    @DrawableRes val mouth: Int,
    @DrawableRes val feature: Int?
)

fun getByUnicode(code: String) = DefaultEmojiList.firstOrNull { it.unicode == code }

val DefaultEmojiList = listOf(
    EmojiModel(
        "\uD83D\uDE2E\u200D\uD83D\uDCA8",
        R.drawable.emoji_1_base,
        R.drawable.emoji_1_eyes,
        R.drawable.emoji_1_mouth,
        R.drawable.emoji_1_feature
    ),
    EmojiModel("\uD83D\uDE05", R.drawable.emoji_2_base, R.drawable.emoji_2_eyes, R.drawable.emoji_2_mouth, R.drawable.emoji_2_feature),
    EmojiModel("\uD83D\uDE20", R.drawable.emoji_3_base, R.drawable.emoji_3_eyes, R.drawable.emoji_3_mouth, null),
    EmojiModel("\uD83D\uDE18", R.drawable.emoji_4_base, R.drawable.emoji_4_eyes, R.drawable.emoji_4_mouth, R.drawable.emoji_4_feature),
    EmojiModel("\uD83D\uDE31", R.drawable.emoji_5_base, R.drawable.emoji_5_eyes, R.drawable.emoji_5_mouth, R.drawable.emoji_5_feature),
    EmojiModel("\uD83E\uDD2E", R.drawable.emoji_6_base, R.drawable.emoji_6_eyes, R.drawable.emoji_6_mouth, R.drawable.emoji_6_feature),
    EmojiModel("\uD83D\uDE02", R.drawable.emoji_7_base, R.drawable.emoji_7_eyes, R.drawable.emoji_7_mouth, R.drawable.emoji_7_feature),
    EmojiModel("\uD83D\uDE1B", R.drawable.emoji_8_base, R.drawable.emoji_8_eyes, R.drawable.emoji_8_mouth, null),
    EmojiModel("\uD83E\uDD2F", R.drawable.emoji_9_base, R.drawable.emoji_9_eyes, R.drawable.emoji_9_mouth, R.drawable.emoji_9_feature),
    EmojiModel("\uD83E\uDD15", R.drawable.emoji_10_base, R.drawable.emoji_10_eyes, R.drawable.emoji_10_mouth, R.drawable.emoji_10_feature),
    EmojiModel("\uD83D\uDE2A", R.drawable.emoji_11_base, R.drawable.emoji_11_eyes, R.drawable.emoji_11_mouth, R.drawable.emoji_11_feature),
    EmojiModel("\uD83E\uDD74", R.drawable.emoji_12_base, R.drawable.emoji_12_eyes, R.drawable.emoji_12_mouth, null)
)
