package com.example.locketwidget.screens.mood

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.example.emoji_mashup.EmojiMashupUtil
import com.example.emoji_mashup.EmojiModel
import com.example.emoji_mashup.mashup
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.network.FileRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class MoodState(
    val firstEmoji: EmojiModel? = null,
    val secondEmoji: EmojiModel? = null,
    val mashupEmoji: EmojiModel? = null,
    val event: Event? = null
) : MavericksState

class MoodViewModel(
    state: MoodState,
    private val emojiMashupUtil: EmojiMashupUtil,
    private val fileRepository: FileRepository
) : MavericksViewModel<MoodState>(state) {

    fun saveMood() {
        withState { state ->
            viewModelScope.launch {
                val mashup = state.mashupEmoji
                val event = if (mashup == null) {
                    Event.Message(R.string.empty_mood_error)
                } else {
                    val bitmap = emojiMashupUtil.getEmojiBitmap(mashup)
                    if (bitmap != null) {
                        val path = fileRepository.saveBitmapToLocalDir(bitmap)
                        val json = LocketGson.gson.toJson(listOfNotNull(state.firstEmoji?.unicode, state.secondEmoji?.unicode))
                        val route = "${ScreenItem.PhotoPreview.route}?" +
                                "${ScreenItem.PREVIEW_ARG}=$path," +
                                "${ScreenItem.PREVIEW_TYPE_ARG}=${false}," +
                                "${ScreenItem.EMOJIS_ARG}=${json}"
                        Event.Navigate(route)
                    } else {
                        Event.Message(R.string.unknown_error)
                    }
                }
                setState { copy(event = event) }
            }
        }
    }

    private fun mashup() {
        withState { state ->
            viewModelScope.launch {
                val firstEmoji = state.firstEmoji
                val secondEmojiModel = state.secondEmoji

                val list = listOfNotNull(firstEmoji, secondEmojiModel)
                val mash = list.firstOrNull()?.mashup(list.lastOrNull() ?: list.first())
                setState { copy(mashupEmoji = mash) }
            }
        }
    }

    fun clearEvent() = setState { copy(event = null) }

    fun selectEmoji(index: Int, emoji: EmojiModel) {
        withState { state ->
            if (index == 0) {
                if (state.firstEmoji != emoji) setState { copy(firstEmoji = emoji) }
                else setState { copy(firstEmoji = null) }
            } else if (index == 1) {
                if (state.secondEmoji != emoji) setState { copy(secondEmoji = emoji) }
                else setState { copy(secondEmoji = null) }
            }
        }
        mashup()
    }

    companion object : MavericksViewModelFactory<MoodViewModel, MoodState> {
        override fun create(viewModelContext: ViewModelContext, state: MoodState): MoodViewModel? {
            val emojiMashupUtil: EmojiMashupUtil by viewModelContext.activity.inject()
            val fileRepository: FileRepository by viewModelContext.activity.inject()
            return MoodViewModel(state, emojiMashupUtil, fileRepository)
        }
    }
}