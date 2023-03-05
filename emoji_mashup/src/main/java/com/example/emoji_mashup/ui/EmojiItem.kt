package com.example.emoji_mashup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emoji_mashup.DefaultEmojiList
import com.example.emoji_mashup.EmojiModel

@Preview
@Composable
private fun EmojitemPreview() {
    EmojiItem(emojiModel = DefaultEmojiList.first(), modifier = Modifier.size(32.dp))
}

@Composable
fun EmojiItem(
    modifier: Modifier = Modifier,
    emojiModel: EmojiModel,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(emojiModel.base), contentDescription = null, modifier = Modifier
                .aspectRatio(1f)
                .fillMaxSize()
        )
        Image(
            painter = painterResource(emojiModel.eyes), contentDescription = null, modifier = Modifier
                .aspectRatio(1f)
                .fillMaxSize()
        )
        Image(
            painter = painterResource(emojiModel.mouth), contentDescription = null, modifier = Modifier
                .aspectRatio(1f)
                .fillMaxSize()
        )
        emojiModel.feature?.let { feature ->
            Image(
                painter = painterResource(feature), contentDescription = null, modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
            )
        }
    }
}