package com.example.locketwidget.features.drawing

import androidx.compose.ui.graphics.Path
import com.airbnb.mvrx.*
import com.example.locketwidget.core.Result
import com.example.locketwidget.local.LocalStorageManager
import com.example.locketwidget.local.PhotoEditor
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class DrawingMomentState(
    val savedImagePath: Async<String> = Uninitialized
) : MavericksState

class DrawingMomentViewModel(
    initState: DrawingMomentState,
    private val photoEditor: PhotoEditor,
    private val localStorageManager: LocalStorageManager
) : MavericksViewModel<DrawingMomentState>(initState) {

    fun save(paths: List<Pair<Path, PathProperties>>, size: Int) {
        viewModelScope.launch {
            val bitmap = photoEditor.drawOnPhoto(null, paths, size)
            if (bitmap != null) {
                val result = localStorageManager.savePhotoToStorage(bitmap)
                if (result is Result.Success) {
                    setState { copy(savedImagePath = Success(result.data)) }
                }
            }
        }
    }

    companion object : MavericksViewModelFactory<DrawingMomentViewModel, DrawingMomentState> {
        override fun create(viewModelContext: ViewModelContext, state: DrawingMomentState): DrawingMomentViewModel? {
            val photoEditor: PhotoEditor by viewModelContext.activity.inject()
            val localStorageManager: LocalStorageManager by viewModelContext.activity.inject()
            return DrawingMomentViewModel(state, photoEditor, localStorageManager)
        }
    }
}