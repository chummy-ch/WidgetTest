package com.example.locketwidget.screens.main

import android.content.Context
import android.os.Parcelable
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.Stable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.airbnb.mvrx.*
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.local.LocalStorageManager
import com.example.locketwidget.network.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject

@Stable
sealed class CameraOutput {

    @Parcelize
    data class Photo(val path: String) : CameraOutput(), Parcelable

    @Parcelize
    data class Video(val path: String) : CameraOutput(), Parcelable

    @Stable
    @Parcelize
    data class Live(val mainPhotoPath: String, val secondaryPhotoPath: String?) : CameraOutput(), Parcelable

    @Stable
    @Parcelize
    data class Mood(val image: String, val emojis: List<String>) : CameraOutput(), Parcelable
}

data class LocketState(
    val user: FirebaseUser = FirebaseAuth.getInstance().currentUser!!,
    val currentCameraOuput: Async<CameraOutput> = Uninitialized,
    val historyPhotos: Async<List<HistoryModel>> = Uninitialized,
    val event: Event.Navigate? = null
) : MavericksState

class LocketViewModel(
    initialState: LocketState,
    private val firestoreDataSource: FirestoreDataSource,
    private val localStorageManager: LocalStorageManager,
    private val dataStore: DataStore<Preferences>
) : MavericksViewModel<LocketState>(initialState) {
    init {
        setState { copy(historyPhotos = Loading()) }
        viewModelScope.launch {
            loadHistory(FirebaseAuth.getInstance().uid!!)
        }
    }

    fun clearPhoto() = setState { copy(currentCameraOuput = Uninitialized) }

    fun takePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoResult = localStorageManager.takePhoto(context, imageCapture, isReversedHorizontal)
            if (photoResult is Result.Success) {
                val event = Event.Navigate(ScreenItem.PhotoPreview.route)
                val cameraOutput = CameraOutput.Photo(photoResult.data.absolutePath)
                setState {
                    copy(
                        currentCameraOuput = Success(cameraOutput),
                        event = event
                    )
                }
            } else {
                setState {
                    copy(currentCameraOuput = Fail((photoResult as Result.Error).exception))
                }
            }
        }
    }

    fun takeVideo(path: String) {
        val event = Event.Navigate(ScreenItem.VideoPreview.route)
        setState { copy(event = event, currentCameraOuput = Success(CameraOutput.Video(path))) }
    }

    fun takeLivePhoto(context: Context, imageCapture: ImageCapture, isReversedHorizontal: Boolean, onFinish: suspend () -> Unit) {
        viewModelScope.launch {
            val photoResult = localStorageManager.takePhoto(context, imageCapture, isReversedHorizontal)
            if (photoResult is Result.Success) {
                val path = photoResult.data.absolutePath
                val cameraOutput = withState(this@LocketViewModel) { it.currentCameraOuput }
                if (cameraOutput is Loading) {
                    val output = cameraOutput.invoke() as? CameraOutput.Live ?: return@launch
                    val newOutput = output.copy(secondaryPhotoPath = path)
                    val event = Event.Navigate(ScreenItem.PhotoPreview.route)
                    setState { copy(currentCameraOuput = Success(newOutput), event = event) }
                } else if (cameraOutput is Uninitialized) {
                    val output = CameraOutput.Live(mainPhotoPath = path, secondaryPhotoPath = null)
                    setState { copy(currentCameraOuput = Loading(output)) }
                }
                onFinish()
            } else {
                setState {
                    copy(currentCameraOuput = Fail((photoResult as Result.Error).exception))
                }
            }
        }
    }

    private suspend fun loadHistory(userId: String) {
        val history = firestoreDataSource.getPhotoHistoryFlow(userId)
        history.collect { list ->
            val modelList = list.map { it.mapToHistoryModel() }
            setState { copy(historyPhotos = Success(modelList)) }
            if (modelList.isNotEmpty())
                dataStore.edit { pref ->
                    pref[ActivityViewModel.IS_HISTORY_EMPTY_KEY] = false
                }
        }
    }

    companion object : MavericksViewModelFactory<LocketViewModel, LocketState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: LocketState
        ): LocketViewModel {
            with(viewModelContext.activity) {
                val firestoreDataSource: FirestoreDataSource by inject()
                val localStorageManager: LocalStorageManager by inject()
                val dataStore: DataStore<Preferences> =
                    viewModelContext.activity.applicationContext.dataStore
                return LocketViewModel(state, firestoreDataSource, localStorageManager, dataStore)
            }
        }
    }
}