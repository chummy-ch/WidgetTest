package com.example.locketwidget.screens.history

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.airbnb.mvrx.*
import com.example.locketwidget.BuildConfig
import com.example.locketwidget.R
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.*
import com.example.locketwidget.local.MediaScanner
import com.example.locketwidget.local.PhotoEditor
import com.example.locketwidget.network.AuthenticationManager
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.network.FirebaseFunctionsManager
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.work.WidgetWorkUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.util.*

data class HistoryState(
    val historyModel: HistoryModel? = null,
    val sender: Async<FirestoreUserResponse> = Uninitialized,
    val event: Event? = null,
    val reactions: Async<List<Async<ReactionModel>>> = Uninitialized
) : MavericksState {
    constructor(history: HistoryModel) : this(historyModel = history)
}

class HistoryViewModel(
    initState: HistoryState,
    private val context: Context,
    private val firestoreDataSource: FirestoreDataSource,
    private val fileRepository: FileRepository,
    private val mediaScanner: MediaScanner,
    private val firebaseFunctionsManager: FirebaseFunctionsManager,
    private val authenticationManager: AuthenticationManager,
    private val photoEditor: PhotoEditor,
    private val widgetWorkUseCase: WidgetWorkUseCase,
) : MavericksViewModel<HistoryState>(initState) {
    init {
        setState { copy(sender = Loading()) }
        val senderId = withState(this) { it.historyModel }!!.senderId
        loadSender(senderId)
        subscribeReactions()
    }

    fun sendReaction(emoji: String) {
        viewModelScope.launch {
            val reactions = withState(this@HistoryViewModel) { it.reactions.invoke() } ?: listOf()
            val newList = listOf(
                Loading(ReactionModel(emoji, authenticationManager.requireUser()))
            ).plus(reactions)
            setState { copy(reactions = Success(newList)) }

            val photoId = withState(this@HistoryViewModel) { it.historyModel?.photoId } ?: return@launch
            val result = firebaseFunctionsManager.sendReaction(emoji, photoId)
            when (result) {
                is Result.Success -> createReactionAnimationEvent(emoji)
                is Result.Error -> failToSendReaction()
            }
        }
    }

    fun changeMainLivePhoto() {
        val currentHistory = withState(this) { it.historyModel } ?: return
        val currentMode = currentHistory.mode as HistoryMode.Live
        val newMode = currentMode.copy(isMain = !currentMode.isMain)
        setState {
            copy(
                historyModel = currentHistory.copy(mode = newMode)
            )
        }
    }

    private suspend fun failToSendReaction() {
        val newReactions = withState(this) { it.reactions.invoke() }?.filterIsInstance<Success<ReactionModel>>()
        if (newReactions != null) {
            setState { copy(reactions = Success(newReactions)) }
        }
        val event = Event.Message(R.string.reaction_fail)
        setState { copy(event = event) }
    }

    private suspend fun createReactionAnimationEvent(emoji: String) {
        val newEvent = Event.Reaction(emoji)
        setState { copy(event = newEvent) }
        delay(Reactions.REACTION_DURATION_MILLS)
        clearEvent()
    }

    private fun subscribeReactions() {
        viewModelScope.launch {
            val photoId = withState(this@HistoryViewModel) { it.historyModel?.photoId } ?: return@launch
            firestoreDataSource.getReactionsFlow(photoId).collect { list: List<ReactionResponse> ->
                setReactions(list)
            }
        }
    }

    private suspend fun setReactions(reactions: List<ReactionResponse>) {
        val reactionModels = reactions.mapNotNull {
            val userResult = firestoreDataSource.getUser(it.sender)
            if (userResult is Result.Success) {
                Success(
                    ReactionModel(
                        it.emoji,
                        userResult.data
                    )
                )
            } else {
                null
            }
        }
        setState { copy(reactions = Success(reactionModels)) }
    }

    private fun loadSender(senderId: String) {
        setState { copy(sender = Loading()) }

        viewModelScope.launch {
            val result = firestoreDataSource.getUser(senderId)
            setState {
                if (result is Result.Success) {
                    copy(sender = Success(result.data))
                } else {
                    copy(sender = Fail((result as Result.Error).exception))
                }
            }
        }
    }

    fun clearEvent() = setState { copy(event = null) }

    fun share(isPremium: Boolean) {
        val history = withState(this) { it.historyModel } ?: return

        when (history.mode) {
            null -> sharePhoto(history.photoId, isPremium)
            is HistoryMode.Video -> shareVideo(history.mode.id, history.photoId)
            is HistoryMode.Live -> shareLivePhoto(history, isPremium)
            is HistoryMode.Mood -> sharePhoto(history.photoId, isPremium)
        }
    }

    private fun shareLivePhoto(history: HistoryModel, isPremium: Boolean) {
        viewModelScope.launch {
            val bitmap: Bitmap? = if (isPremium) {
                createLivePhotoBitmap(history)
            } else {
                createLivePhotoBitmap(history)?.let {
                    photoEditor.addWatermark(it)
                }
            }

            if (bitmap != null) {
                val uri = fileRepository.saveBitmapToLocalDirGetUri(bitmap)

                if (uri != null) {
                    val event = Event.Share<Uri>(uri)
                    setState { copy(event = event) }
                }
            } else {
                val event = Event.Message(R.string.upload_photo_error)
                setState { copy(event = event) }
            }
        }
    }

    private fun downloadLivePhoto(history: HistoryModel, isPremium: Boolean) {
        viewModelScope.launch {
            val bitmap: Bitmap? = if (isPremium) {
                createLivePhotoBitmap(history)
            } else {
                createLivePhotoBitmap(history)?.let {
                    photoEditor.addWatermark(it)
                }
            }

            if (bitmap != null) {
                val path =
                    fileRepository.saveBitmapToDir(bitmap, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
                mediaScanner.setFile(File(path))

                val event = Event.Message(R.string.success_photo_saved_message)
                setState { copy(event = event) }
            } else {
                val event = Event.Message(R.string.unknown_error)
                setState { copy(event = event) }
            }
        }
    }

    private suspend fun createLivePhotoBitmap(history: HistoryModel): Bitmap? {
        val mode = history.mode as HistoryMode.Live

        val mainPhotoPath = fileRepository.downloadFile(
            link = if (mode.isMain) mode.link else history.photoLink,
            name = Date().time.toString(),
            fileExtension = FileRepository.IMAGE_EXTENSION
        ) ?: return null
        val secondPhotoPath = fileRepository.downloadFile(
            link = if (mode.isMain) history.photoLink else mode.link,
            name = Date().time.toString() + "second",
            fileExtension = FileRepository.IMAGE_EXTENSION
        ) ?: return null

        return photoEditor.createLivePhoto(CameraOutput.Live(mainPhotoPath, secondPhotoPath))
    }

    private suspend fun getVideoDownloadLink(id: String): String? {
        val result = firebaseFunctionsManager.createVideoDownloadLink(id)
        if (result is Result.Success) {
            val progress = result.data[FirebaseFunctionsManager.VIDEO_URL_PROGRESS_KEY] as? Int
            if (progress == 100) {
                return result.data[FirebaseFunctionsManager.VIDEO_URL_KEY]!! as String
            }
        }
        return null
    }

    private fun shareVideo(videoId: String, name: String) {
        viewModelScope.launch {
            val link = getVideoDownloadLink(videoId)
            val event = if (link != null) {
                val path = fileRepository.downloadFile(
                    link = link,
                    name = name,
                    fileExtension = FileRepository.VIDEO_EXTENSION
                )
                if (path == null) {
                    Event.Message(R.string.unknown_error)
                } else {
                    Event.Share(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(path)))
                }
            } else {
                Event.Message(R.string.fail_video_save)
            }
            setState { copy(event = event) }
        }
    }

    private fun sharePhoto(photoId: String, isPremium: Boolean) {
        viewModelScope.launch {
            val bitmap = fileRepository.getBitmap(photoId)
            if (bitmap != null) {
                val uri = if (isPremium) fileRepository.saveBitmapToLocalDirGetUri(bitmap)
                else {
                    val wateredBitmap = photoEditor.addWatermark(bitmap) ?: return@launch
                    fileRepository.saveBitmapToLocalDirGetUri(wateredBitmap)
                }
                setState {
                    if (uri != null) {
                        copy(event = Event.Share<Uri>(uri))
                    } else {
                        val event = Event.Message(R.string.unknown_error)
                        copy(event = event)
                    }
                }
            }
        }
    }

    fun download(isPremium: Boolean) {
        val history = withState(this) { it.historyModel } ?: return
        when (history.mode) {
            null -> downloadPhoto(history.photoId, isPremium)
            is HistoryMode.Video -> downloadVideo(history.mode.id, history.photoId)
            is HistoryMode.Live -> downloadLivePhoto(history, isPremium)
            is HistoryMode.Mood -> downloadPhoto(history.photoId, isPremium)
        }
    }

    private fun downloadVideo(videoId: String, name: String) {
        viewModelScope.launch {
            val link = getVideoDownloadLink(videoId)
            val event = if (link != null) {
                val path = fileRepository.downloadFile(
                    link,
                    name,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    FileRepository.VIDEO_EXTENSION
                )
                if (path == null) {
                    Event.Message(R.string.unknown_error)
                } else {
                    mediaScanner.setFile(File(path))
                    Event.Message(R.string.success_video_save)
                }
            } else {
                Event.Message(R.string.fail_video_save)
            }
            setState { copy(event = event) }
        }
    }

    private fun downloadPhoto(photoId: String, isPremium: Boolean) {
        viewModelScope.launch {
            val bitmap = fileRepository.getBitmap(photoId)
            if (bitmap != null) {
                val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val result = if (isPremium) fileRepository.saveBitmapToDir(bitmap, file)
                else {
                    val wateredBitmap = photoEditor.addWatermark(bitmap) ?: return@launch
                    fileRepository.saveBitmapToDir(wateredBitmap, file)
                }
                mediaScanner.setFile(File(result))
                val event = Event.Message(
                    message = R.string.success_photo_saved_message
                )
                setState {
                    copy(
                        event = event
                    )
                }
            }
        }
    }

    fun hidePhoto() {
        viewModelScope.launch {
            val user = authenticationManager.getUser()
            if (user != null) {
                val photoId = withState(this@HistoryViewModel) { it.historyModel }!!.photoId
                val result = firestoreDataSource.hideHistoryById(photoId, user.uid)
                if (result is Result.Success) {
                    widgetWorkUseCase.createScheduleWork()
                    val event = Event.Navigate(null)
                    setState { copy(event = event) }
                } else {
                    val event = Event.Navigate(ScreenItem.History.route)
                    setState { copy(event = event) }
                }
            }
        }
    }

    companion object : MavericksViewModelFactory<HistoryViewModel, HistoryState> {
        override fun create(viewModelContext: ViewModelContext, state: HistoryState): HistoryViewModel {
            val firestore: FirestoreDataSource by viewModelContext.activity.inject()
            val photoRep: FileRepository by viewModelContext.activity.inject()
            val mediaScanner: MediaScanner by viewModelContext.activity.inject()
            val firebaseFunctionsManager: FirebaseFunctionsManager by viewModelContext.activity.inject()
            val authenticationManager: AuthenticationManager by viewModelContext.activity.inject()
            val photoEditor: PhotoEditor by viewModelContext.activity.inject()
            val widgetWorkUseCase: WidgetWorkUseCase by viewModelContext.activity.inject()
            return HistoryViewModel(
                state,
                viewModelContext.activity.applicationContext,
                firestore,
                photoRep,
                mediaScanner,
                firebaseFunctionsManager,
                authenticationManager,
                photoEditor,
                widgetWorkUseCase,
            )
        }
    }
}