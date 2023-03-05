package com.example.locketwidget.screens.textmoments

import android.annotation.SuppressLint
import com.airbnb.mvrx.*
import com.example.locketwidget.R
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.local.ImageText
import com.example.locketwidget.local.LocalStorageManager
import com.example.locketwidget.local.MediaScanner
import com.example.locketwidget.local.PhotoEditor
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

data class TextMomentsState(
    val patterns: List<TextMomentBackground> = listOf(),
    val chosenPattern: TextMomentBackground? = null,
    val textModel: ImageText = ImageText(),
    val imagePath: Async<String> = Uninitialized,
    val event: Event? = null
) : MavericksState

@SuppressLint("ResourceType")
class TextMomentsViewModel(
    initialState: TextMomentsState,
    private val photoEditor: PhotoEditor,
    private val localStorageManager: LocalStorageManager,
    private val mediaScanner: MediaScanner,
    private val backgroundRepository: TextMomentBackgroundRepository
) : MavericksViewModel<TextMomentsState>(initialState) {
    init {
        setPatterns()
        setState {
            copy(chosenPattern = this.patterns.first())
        }
    }

    private fun setPatterns() {
        setState {
            copy(patterns = backgroundRepository.patternIds)
        }
    }

    fun resetChosenPattern(pattern: TextMomentBackground) {
        withState { state ->
            setState {
                copy(chosenPattern = pattern, textModel = state.textModel.copy(textColor = pattern.textColor))
            }
        }
    }

    fun changeText(text: String) {
        withState { state ->
            setState { copy(textModel = state.textModel.copy(text = text)) }
        }
    }

    fun changeTextSize(size: Int) {
        withState { state ->
            setState { copy(textModel = state.textModel.copy(textSize = size)) }
        }
    }

    fun setPreviewSize(size: Float) {
        withState { state ->
            setState { copy(textModel = state.textModel.copy(previewSize = size)) }
        }
    }

    fun saveImage() {
        withState { state ->
            viewModelScope.launch {
                if (state.chosenPattern != null) {
                    val bitmap =
                        photoEditor.addTextOnImage(state.chosenPattern.backgroundId, state.textModel)
                    val result = localStorageManager.savePhotoToStorage(bitmap)
                    setState {
                        if (result is Result.Success) {
                            val event = Event.Navigate(ScreenItem.PhotoPreview.route)
                            copy(
                                imagePath = Success(result.data),
                                event = event
                            )
                        } else {
                            val event = Event.Message(R.string.unknown_error)
                            copy(event = event)
                        }
                    }
                }
            }
        }
    }

    fun sharePhoto() {
        withState { state ->
            viewModelScope.launch {
                if (state.chosenPattern != null) {
                    val bitmap =
                        photoEditor.addTextOnImage(state.chosenPattern.backgroundId, state.textModel)
                    val result = localStorageManager.savePhotoToStorage(bitmap)
                    setState {
                        if (result is Result.Success) {
                            copy(
                                imagePath = Success(result.data),
                                event = Event.Share<String>(result.data)
                            )
                        } else {
                            val event = Event.Message(R.string.unknown_error)
                            copy(event = event)
                        }
                    }
                }
            }
        }
    }

    fun downloadPhoto() {
        withState { state ->
            viewModelScope.launch {
                if (state.chosenPattern != null) {
                    val bitmap = photoEditor.addTextOnImage(state.chosenPattern.backgroundId, state.textModel)
                    val result = localStorageManager.savePhotoToStorage(bitmap, false)
                    if (result is Result.Success) {
                        mediaScanner.setFile(File(result.data))
                    }
                    val event = Event.Message(R.string.success_photo_saved_message)
                    setState {
                        copy(event = event)
                    }
                } else {
                    val event = Event.Message(R.string.unknown_error)
                    setState {
                        copy(event = event)
                    }
                }
            }
        }
    }

    fun clearEvent() {
        setState { copy(event = null) }
    }

    companion object : MavericksViewModelFactory<TextMomentsViewModel, TextMomentsState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: TextMomentsState
        ): TextMomentsViewModel {
            with(viewModelContext.activity) {
                val photoEditor: PhotoEditor by inject()
                val storageManager: LocalStorageManager by inject()
                val mediaScanner: MediaScanner by inject()
                val backgroundRepository: TextMomentBackgroundRepository by inject()
                return TextMomentsViewModel(
                    state,
                    photoEditor,
                    storageManager,
                    mediaScanner,
                    backgroundRepository
                )
            }
        }
    }
}