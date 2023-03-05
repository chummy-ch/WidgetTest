package com.example.locketwidget.screens.preview

import android.media.ThumbnailUtils
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Path
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.airbnb.mvrx.*
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.*
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.features.drawing.PathProperties
import com.example.locketwidget.local.*
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.network.FirebaseFunctionsManager
import com.example.locketwidget.network.FirestoreDataSource
import com.example.locketwidget.screens.contacts_group.GroupViewModel
import com.example.locketwidget.screens.main.CameraOutput
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

data class PickFriendsModel(val friend: ContactResponse, val isSelected: Boolean = false)

data class PreviewState(
    val cameraOutput: Async<CameraOutput> = Loading(),
    val drawedPhotoPath: Async<String> = Uninitialized,
    val friends: Async<List<PickFriendsModel>> = Uninitialized,
    val groups: Async<List<Pair<DataStoreGroupModel, Boolean>>> = Uninitialized,
    val event: Event? = null,
    val widgetTextModel: WidgetTextModel = WidgetTextModel()
) : MavericksState {
    constructor(output: CameraOutput) : this(cameraOutput = Success(output))
}

class PreviewViewModel(
    initState: PreviewState,
    private val localStorageManager: LocalStorageManager,
    private val firebaseFunctionsManager: FirebaseFunctionsManager,
    private val fileRepository: FileRepository,
    private val firestoreDataSource: FirestoreDataSource,
    private val photoEditor: PhotoEditor,
    private val dataStore: DataStore<Preferences>,
    private val mediaScanner: MediaScanner,
    private val contactsFilterUseCase: ContactsFilterUseCase
) : MavericksViewModel<PreviewState>(initState) {
    init {
        viewModelScope.launch {
            val friendsResult = firestoreDataSource.getUserContacts(FirebaseAuth.getInstance().currentUser!!.uid)
            setState {
                when (friendsResult) {
                    is Result.Success -> {
                        val friends = friendsResult.data.map { PickFriendsModel(it) }
                        copy(friends = Success(friends))
                    }
                    is Result.Error -> {
                        copy(friends = Fail(friendsResult.exception))
                    }
                }
            }
            val groups =
                LocketGson.gson.fromJson(dataStore.data.first()[GroupViewModel.CONTACTS_GROUP_KEY], DataStoreGroupModels::class.java)
            setState {
                copy(groups = Success(groups?.groupModels?.map { it to false } ?: listOf()))
            }
        }
    }

    fun clearEvent() = setState { copy(event = null) }

    fun selectGroup(groupId: String, isSelected: Boolean) {
        val groups = withState(this@PreviewViewModel) { it.groups }.invoke()
        val newGroups = groups?.map {
            if (it.first.id == groupId) it.first to isSelected
            else it
        }
        setState { copy(groups = Success(newGroups ?: listOf())) }
        filterContacts(groupId, isSelected)
    }

    private fun filterContacts(groupId: String, isSelected: Boolean) {
        val groups = withState(this@PreviewViewModel) { it.groups }.invoke()
        val friends = withState(this@PreviewViewModel) { it.friends }.invoke()
        if (groups != null && friends != null) {
            if (isSelected) {
                setState {
                    copy(friends = Success(contactsFilterUseCase.selectContacts(friends, groups, groupId)))
                }
            } else {
                setState {
                    copy(friends = Success(contactsFilterUseCase.deselectContacts(friends, groups, groupId)))
                }
            }
        }
    }

    fun draw(paths: List<Pair<Path, PathProperties>>, size: Int) {
        viewModelScope.launch {
            val output = withState(this@PreviewViewModel) { it.cameraOutput }.invoke()
            val photo = output?.photoPath() ?: return@launch
            val bitmap = photoEditor.drawOnPhoto(photo, paths, size) ?: error("Bitmap is null")
            val path = localStorageManager.savePhotoToStorage(bitmap)
            if (path is Result.Success) {
                setState { copy(drawedPhotoPath = Success(path.data)) }
            }
        }
    }

    fun downloadVideo() {
        viewModelScope.launch {
            val output = withState(this@PreviewViewModel) { it.cameraOutput }.invoke() as? CameraOutput.Video ?: return@launch

            val result = localStorageManager.copyVideo(output.path)
            if (result is Result.Success) {
                mediaScanner.setFile(File(result.data))
            }
            val event = Event.Message(R.string.success_video_save)
            setState { copy(event = event) }
        }
    }

    fun downloadLivePhoto() {
        withState { state ->
            viewModelScope.launch(Dispatchers.IO) {
                val output = state.cameraOutput.invoke() as? CameraOutput.Live ?: return@launch
                val resultBitmap = photoEditor.createLivePhoto(output)
                val event = if (resultBitmap != null) {
                    val path = fileRepository.saveBitmapToDir(
                        resultBitmap,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    )
                    mediaScanner.setFile(File(path))
                    Event.Message(R.string.success_photo_saved_message)
                } else {
                    Event.Message(R.string.save_photo_error)
                }
                setState { copy(event = event) }
            }
        }
    }

    fun downloadImage() {
        viewModelScope.launch {
            val output = withState(this@PreviewViewModel) { it.cameraOutput }.invoke() ?: return@launch
            val photoPath = output.photoPath() ?: return@launch

            val result = localStorageManager.copyImage(photoPath)
            val event = if (result is Result.Success) {
                mediaScanner.setFile(File(result.data))
                Event.Message(R.string.success_photo_saved_message)
            } else {
                Event.Message(R.string.save_photo_error)
            }
            setState { copy(event = event) }
        }
    }

    fun setPreviewSize(size: Float) {
        withState { state ->
            val textOffsetModel = state.widgetTextModel
            setState { copy(widgetTextModel = textOffsetModel.copy(previewSize = size)) }
        }
    }

    fun changeOffset(offsetY: Int) {
        val newOffsetY = withState(this) { it.widgetTextModel.offsetY } + offsetY
        setState { copy(widgetTextModel = this.widgetTextModel.copy(offsetY = newOffsetY)) }
    }

    fun changeRectSize(size: Int) {
        setState { copy(widgetTextModel = this.widgetTextModel.copy(rectHeight = size)) }
    }

    fun changeText(text: String) {
        withState { state ->
            if (text.length <= WIDGET_TEXT_MAX_SIZE) {
                val textOffsetModel = state.widgetTextModel
                setState { copy(widgetTextModel = textOffsetModel.copy(text = text)) }
            }
        }
    }

    fun enableTextField() {
        withState { state ->
            val textOffsetModel = state.widgetTextModel
            setState { copy(widgetTextModel = textOffsetModel.copy(isActive = true)) }
        }
    }

    fun disableTextField() {
        withState { state ->
            val textOffsetModel = state.widgetTextModel
            setState { copy(widgetTextModel = textOffsetModel.copy(isActive = false)) }
        }
    }

    fun pickFriend(friendId: String, pick: Boolean) {
        withState { state ->
            viewModelScope.launch {
                val friend = state.friends.invoke()?.firstOrNull { it.friend.id == friendId } ?: return@launch
                val newFriendModel = friend.copy(isSelected = pick)
                val newFriends = state.friends.invoke()?.map {
                    if (it.friend.id == newFriendModel.friend.id) newFriendModel
                    else it
                } ?: return@launch
                setState { copy(friends = Success(newFriends)) }

                filterGroups(newFriends, pick)
            }
        }
    }

    private fun filterGroups(newFriends: List<PickFriendsModel>, pick: Boolean) {
        val groups = withState(this@PreviewViewModel) { it.groups }.invoke()
        if (groups != null) {
            if (pick) {
                setState { copy(groups = Success(contactsFilterUseCase.selectGroups(newFriends, groups))) }
            } else {
                setState { copy(groups = Success(contactsFilterUseCase.deselectGroups(newFriends, groups))) }
            }
        }
    }

    fun sendVideo() {
        val output = withState(this) { it.cameraOutput.invoke() } ?: return
        val event = Event.PhotoSending(SendingStatus.Sending)
        setState { copy(event = event) }

        viewModelScope.launch(Dispatchers.Default) {
            val uploadLink = firebaseFunctionsManager.createVideoUploadUrl()
            if (uploadLink is Result.Success) {
                val video = output as CameraOutput.Video
                val uploadResult = fileRepository.uploadVideo(uploadLink.data.uploadUrl, video.path)

                if (uploadResult is Result.Success) {
                    val ids = getFriendList()

                    if (ids == null) {
                        uploadVideoFailed()
                        return@launch
                    }

                    val thumb = ThumbnailUtils.createVideoThumbnail(output.path, MediaStore.Images.Thumbnails.MINI_KIND)
                    if (thumb == null) {
                        uploadVideoFailed()
                        return@launch
                    }
                    val thumbPath = fileRepository.saveBitmapToLocalDir(photoEditor.scaleBitmap(thumb))
                    val thumbUploadLink = firebaseFunctionsManager.createUploadUrl()
                    if (thumbUploadLink !is Result.Success) {
                        uploadVideoFailed()
                        return@launch
                    }
                    val thumbUploadResult = fileRepository.uploadPhoto(thumbUploadLink.data.uploadUrl, thumbPath)
                    if (thumbUploadResult !is Result.Success) {
                        uploadVideoFailed()
                        return@launch
                    }

                    val uploadUrl = uploadLink.data.uploadUrl
                    val id = uploadUrl.substring(uploadUrl.lastIndexOf("/"))

                    val firebaseUploadResult = firebaseFunctionsManager.addPhoto(
                        photoId = thumbUploadResult.data.result.id,
                        ids = ids,
                        video = id
                    )

                    if (firebaseUploadResult is Result.Success) {
                        saveIsUploadingSuccess()

                        val sendingEvent = Event.PhotoSending(SendingStatus.Success)
                        setState {
                            copy(
                                event = sendingEvent,
                                cameraOutput = Uninitialized,
                                widgetTextModel = WidgetTextModel()
                            )
                        }
                    } else if (firebaseUploadResult is Result.Error) {
                        val sendingEvent = Event.PhotoSending(SendingStatus.Fail(firebaseUploadResult.exception))
                        setState {
                            copy(
                                cameraOutput = Uninitialized,
                                event = sendingEvent
                            )
                        }
                    }
                } else {
                    uploadVideoFailed()
                }
            } else {
                uploadVideoFailed()
            }
        }
    }

    fun sendLivePhoto() {
        val output = withState(this) { it.cameraOutput }.invoke() ?: return
        val event = Event.PhotoSending(SendingStatus.Sending)
        setState { copy(event = event) }

        viewModelScope.launch {
            val mainUploadLink = firebaseFunctionsManager.createUploadUrl()
            val secondUploadLink = firebaseFunctionsManager.createUploadUrl()
            if (mainUploadLink is Result.Success && secondUploadLink is Result.Success) {
                val live = output as CameraOutput.Live
                val mainUploadResult =
                    fileRepository.uploadPhoto(
                        mainUploadLink.data.uploadUrl,
                        live.mainPhotoPath
                    )
                val secondUploadResult = fileRepository.uploadPhoto(
                    secondUploadLink.data.uploadUrl,
                    live.secondaryPhotoPath!!,
                )

                if (mainUploadResult is Result.Success && secondUploadResult is Result.Success) {
                    val ids = getFriendList()
                    if (ids == null) {
                        uploadPhotoFailed()
                        return@launch
                    }

                    val mainPhotoId = mainUploadResult.data.result.id
                    val secondPhotoId = secondUploadResult.data.result.id
                    val firebaseUploadResult = firebaseFunctionsManager.addPhoto(photoId = mainPhotoId, ids = ids, live = secondPhotoId)

                    if (firebaseUploadResult is Result.Success) {
                        saveIsUploadingSuccess()

                        val sendingEvent = Event.PhotoSending(SendingStatus.Success)
                        setState {
                            copy(
                                event = sendingEvent,
                                cameraOutput = Uninitialized,
                                widgetTextModel = WidgetTextModel()
                            )
                        }
                    } else if (firebaseUploadResult is Result.Error) {
                        val sendingEvent = Event.PhotoSending(SendingStatus.Fail(firebaseUploadResult.exception))
                        setState {
                            copy(
                                cameraOutput = Uninitialized,
                                event = sendingEvent
                            )
                        }
                    }
                } else {
                    uploadPhotoFailed()
                }
            } else {
                val errorEvent = Event.Message(R.string.unknown_error)
                setState { copy(event = errorEvent) }
            }
        }
    }

    fun send() {
        withState { state ->
            when (val output = state.cameraOutput.invoke()) {
                is CameraOutput.Photo -> sendPhoto()
                is CameraOutput.Video -> sendVideo()
                is CameraOutput.Live -> sendLivePhoto()
                is CameraOutput.Mood -> uploadPhoto(output.image, output.emojis)
                else -> throw IllegalAccessException()
            }
        }
    }

    fun sendPhoto() {
        val textOffsetModel = withState(this) { it.widgetTextModel }
        if (textOffsetModel.isActive) {
            uploadEditedPhoto(textOffsetModel)
        } else {
            val path = withState(this@PreviewViewModel) { it.drawedPhotoPath.invoke() ?: it.drawedPhotoPath.invoke() }
                ?: withState(this@PreviewViewModel) { it.cameraOutput.invoke() }?.photoPath()
                ?: return
            uploadPhoto(path)
        }
    }

    fun changeLiveMainPhoto() {
        withState { state ->
            val output = state.cameraOutput.invoke() ?: return@withState
            val newOutput = (output as CameraOutput.Live).copy(
                mainPhotoPath = output.secondaryPhotoPath!!,
                secondaryPhotoPath = output.mainPhotoPath
            )
            setState { copy(cameraOutput = Success(newOutput)) }
        }
    }

    fun removePhoto() {
        viewModelScope.launch {
            val output = withState(this@PreviewViewModel) { it.cameraOutput }.invoke() ?: return@launch
            when (output) {
                is CameraOutput.Photo -> {
                    localStorageManager.removeFile(output.path)
                }
                is CameraOutput.Live -> {
                    localStorageManager.removeFile(output.mainPhotoPath)
                    output.secondaryPhotoPath?.let { path ->
                        localStorageManager.removeFile(path)
                    }
                }
                is CameraOutput.Video -> {
                    localStorageManager.removeFile(output.path)
                }
                is CameraOutput.Mood -> {
                    localStorageManager.removeFile(output.image)
                }
            }
            val event = Event.Navigate(null)
            setState {
                copy(
                    cameraOutput = Uninitialized,
                    widgetTextModel = WidgetTextModel(),
                    event = event
                )
            }
        }
    }

    private fun CameraOutput.photoPath(): String? {
        return (this as? CameraOutput.Photo)?.path
    }

    private fun uploadEditedPhoto(textOffsetModel: WidgetTextModel) {
        withState {
            val path = withState(this@PreviewViewModel) { it.drawedPhotoPath.invoke() }
                ?: withState(this@PreviewViewModel) { it.cameraOutput.invoke() }?.photoPath()
                ?: return@withState
            viewModelScope.launch {
                val bitmap = photoEditor.addTextOnPhoto(path, textOffsetModel)
                if (bitmap != null) {
                    val result = localStorageManager.savePhotoToStorage(bitmap)
                    if (result is Result.Success) {
                        uploadPhoto(result.data)
                    }
                }
            }
        }
    }

    private fun uploadPhoto(path: String, emojis: List<String>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = Event.PhotoSending(SendingStatus.Sending)
            setState { copy(event = event) }

            val urlResult = firebaseFunctionsManager.createUploadUrl()

            if (urlResult is Result.Success) {
                val uploadResult = fileRepository.uploadPhoto(
                    urlResult.data.uploadUrl,
                    path
                )

                when (uploadResult) {
                    is Result.Success -> {
                        val ids = getFriendList()

                        if (ids == null) {
                            uploadPhotoFailed()
                            return@launch
                        }

                        val photoId = uploadResult.data.result.id
                        val firebaseUploadResult = firebaseFunctionsManager.addPhoto(photoId, ids, emojis = emojis)
                        when (firebaseUploadResult) {
                            is Result.Success -> {
                                saveIsUploadingSuccess()

                                val sendingEvent = Event.PhotoSending(SendingStatus.Success)
                                setState {
                                    copy(
                                        event = sendingEvent,
                                        cameraOutput = Uninitialized,
                                        widgetTextModel = WidgetTextModel()
                                    )
                                }
                            }
                            is Result.Error -> {
                                val sendingEvent = Event.PhotoSending(SendingStatus.Fail(firebaseUploadResult.exception))
                                setState {
                                    copy(
                                        cameraOutput = Uninitialized,
                                        event = sendingEvent
                                    )
                                }
                            }
                        }
                    }
                    is Result.Error -> {
                        val errorEvent = Event.Message(R.string.unknown_error)
                        setState { copy(event = errorEvent) }
                    }
                }
            }
        }
    }

    private suspend fun getFriendList(): List<String>? {
        val contacts = withState(this@PreviewViewModel) { it.friends.invoke() } ?: return null

        val friends = contacts.filter { it.isSelected }

        return if (friends.isEmpty()) contacts.map { it.friend.id }
        else friends.map { it.friend.id }
    }

    private suspend fun saveIsUploadingSuccess() {
        dataStore.edit { pref ->
            pref[ActivityViewModel.IS_SUCCESS_PHOTO_SEND_KEY] = true
        }
    }

    private fun uploadVideoFailed() {
        val event = Event.Message(R.string.upload_video_error)
        setState { copy(event = event) }
    }

    private fun uploadPhotoFailed() {
        val event = Event.Message(R.string.upload_photo_error)
        setState { copy(event = event) }
    }

    companion object : MavericksViewModelFactory<PreviewViewModel, PreviewState> {
        private const val WIDGET_TEXT_MAX_SIZE = 30
        override fun create(viewModelContext: ViewModelContext, state: PreviewState): PreviewViewModel {
            with(viewModelContext.activity) {
                val storageManager: LocalStorageManager by inject()
                val functions: FirebaseFunctionsManager by inject()
                val fileRepository: FileRepository by inject()
                val firestore: FirestoreDataSource by inject()
                val photoEditor: PhotoEditor by inject()
                val dataStore: DataStore<Preferences> = applicationContext.dataStore
                val mediaScanner: MediaScanner by inject()
                val contactsFilterUseCase: ContactsFilterUseCase by inject()
                return PreviewViewModel(
                    state,
                    storageManager,
                    functions,
                    fileRepository,
                    firestore,
                    photoEditor,
                    dataStore,
                    mediaScanner,
                    contactsFilterUseCase
                )
            }
        }
    }
}