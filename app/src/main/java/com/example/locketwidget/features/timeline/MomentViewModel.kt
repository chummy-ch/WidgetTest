package com.example.locketwidget.features.timeline

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.example.locketwidget.R
import com.example.locketwidget.core.HistoryPageModel
import com.example.locketwidget.core.HistoryPaging
import com.example.locketwidget.core.HistorySource
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.HistoryResponse
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.local.MediaScanner
import com.example.locketwidget.local.PhotoEditor
import com.example.locketwidget.network.AdaptyRepository
import com.example.locketwidget.network.FileRepository
import com.example.locketwidget.network.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class MomentState(
    val historyFlow: Flow<PagingData<HistoryPaging>> = flowOf(),
    val selectedPhotos: List<HistoryModel> = listOf(),
    val downloadedPhotos: List<Pair<HistoryModel, String>> = listOf(),
    val name: String = "",
    val speed: Float = 1f,
    val event: Event? = null
) : MavericksState

class MomentViewModel(
    initState: MomentState,
    private val context: Context,
    private val firestoreDataSource: FirestoreDataSource,
    private val adaptyRepository: AdaptyRepository,
    private val fileRepository: FileRepository,
    //private val fFmpegUseCase: FFmpegUseCase,
    private val photoEditor: PhotoEditor
) : MavericksViewModel<MomentState>(initState) {
    init {
        val navigateEvent = Event.Navigate(ScreenItem.DownloadingMoments.route)
        setState { copy(event = navigateEvent) }

        viewModelScope.launch {
            val pageSize = FirestoreDataSource.HISTORY_LOAD_AMOUNT.toInt()
            val isPremium = adaptyRepository.isPremium()
            val flow = Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize / 2
                )
            ) {
                HistorySource(firestoreDataSource, isPremium, listOf())
            }.flow.cachedIn(viewModelScope)
            setState { copy(historyFlow = flow) }
        }

        val pageSize = FirestoreDataSource.HISTORY_LOAD_AMOUNT
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val query = firestoreDataSource.getFilteredHistoryListByPageQuery(listOf(), userId, null, pageSize * 2)
            val result = query.awaitHistoryResponse()
            if (result is Result.Success) {
                val history = result.data.historyResponse.map { it.mapToHistoryModel() }.reversed()
                setState { copy(selectedPhotos = history) }
                preloadImages(history)
            }
        }
    }

    fun clearEvent() = setState { copy(event = null) }

    fun setName(name: String) = setState { copy(name = name) }

    private suspend fun preloadImages(list: List<HistoryModel>) {
        val imageLoader = context.imageLoader
        list.forEach {
            val request = ImageRequest.Builder(context).data(it.photoLink).memoryCachePolicy(CachePolicy.ENABLED).build()
            imageLoader.enqueue(request).job.start()
        }
    }

    private suspend fun Query.awaitHistoryResponse() = suspendCoroutine<Result<HistoryPageModel>> { con ->
        get().addOnSuccessListener { snaphost ->
            val docs = snaphost.documents
            val list = docs.mapNotNull { it.toObject(HistoryResponse::class.java) }
            val page = HistoryPageModel(
                historyResponse = list,
                lastDoc = docs.lastOrNull()
            )
            con.resume(Result.Success(page))
        }.addOnFailureListener {
            con.resume(Result.Error(it))
        }
    }

    fun selectSpeed(speed: Float) = setState { copy(speed = speed) }

    fun shareVideo(isPremium: Boolean) {
        /* withState { state ->

             viewModelScope.launch {
                 if (checkResources(state.selectedPhotos)) {
                     val audioFilePath = downloadAudio()

                     if (audioFilePath == null) {
                         setVideoErrorEvent()
                         return@launch
                     }

                     val videoResult = createVideo(state.name, context.filesDir.absolutePath, audioFilePath, isPremium)

                     if (videoResult != null) {
                         val shareEvent =
                             Event.Share(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(videoResult), state.name))
                         setState { copy(event = shareEvent) }
                     } else {
                         setVideoErrorEvent()
                     }
                 }
             }
         }*/
    }

    fun saveToGallery(isPremium: Boolean) {
        /*withState { state ->

            viewModelScope.launch(Dispatchers.Default) {
                if (checkResources(state.selectedPhotos)) {

                    val audioFilePath = downloadAudio()

                    if (audioFilePath == null) {
                        setVideoErrorEvent()
                        return@launch
                    }

                    val videoResult = createVideo(
                        state.name,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
                        audioFilePath,
                        isPremium
                    )

                    if (videoResult != null) {
                        scanFile(videoResult)
                        val event = Event.Message(R.string.moment_saved_to_gallery)
                        setState { copy(event = event) }
                    } else {
                        setVideoErrorEvent()
                    }
                }
            }
        }*/
    }

    private suspend fun checkResources(selectedPhotos: List<HistoryModel>): Boolean {
        if (selectedPhotos.size < PHOTO_MIN_COUNT) {
            setPhotoNotEnoughEvent()
            return false
        }

        val audio = downloadAudio()
        if (audio == null) {
            setVideoErrorEvent()
            return false
        }

        return true
    }

    private suspend fun downloadAudio(): String? {
        return fileRepository.downloadFile(AUDIO_LINK, "timeline_audio", context.filesDir, FileRepository.AUDIO_EXTENSION)
    }

    private suspend fun scanFile(path: String) {
        MediaScanner(context).setFile(File(path))
    }

    /*private suspend fun createVideo(name: String, path: String, audioPath: String, isPremium: Boolean): String? {
        return onDefault {
            val creatingEvent = Event.Navigate(ScreenItem.CreatingMoments.route)
            setState { copy(event = creatingEvent) }

            val selectedPhotos = com.airbnb.mvrx.withState(this@MomentViewModel) { it.selectedPhotos }

            if (selectedPhotos.size < PHOTO_MIN_COUNT) {
                setPhotoNotEnoughEvent()
                return@onDefault null
            }

            var paths = downloadPhotos()

            if (paths.size < PHOTO_MIN_COUNT) {
                setPhotoNotEnoughEvent()
                return@onDefault null
            }

            if (!isPremium) {
                val firstFrame = paths.first()
                val watermarkFrame = photoEditor.addWatermark(BitmapFactory.decodeFile(firstFrame.second)) ?: return@onDefault null
                val firstFramePath = fileRepository.saveBitmapToLocalDirGetUri(watermarkFrame)?.lastPathSegment ?: return@onDefault null
                paths = paths.map {
                    if (it == firstFrame) it.first to File(context.filesDir, firstFramePath).absolutePath
                    else it
                }
            }
            val startTime = Date().time
            val nonAudioResult = fFmpegUseCase.createTimeline(paths.map { it.second })
            val endTime = Date().time

            return@onDefault if (nonAudioResult is Result.Success) {
                val result = fFmpegUseCase.addAudio(nonAudioResult.data, name, path, audioPath)
                if (result is Result.Success) {
                    LocketAnalytics.logMomentCreated(endTime - startTime)
                    result.data
                } else {
                    null
                }
            } else {
                null
            }
        }
    }*/

    private fun setVideoErrorEvent() {
        val navigateEvent = Event.Navigate(null)
        setState { copy(event = navigateEvent) }
        val errorEvent = Event.Message(R.string.timeline_error)
        setState { copy(event = errorEvent) }
    }

    private suspend fun downloadPhotos(): List<Pair<HistoryModel, String>> {
        setState { copy(downloadedPhotos = listOf()) }
        val selectedPhotos = com.airbnb.mvrx.withState(this@MomentViewModel) { it.selectedPhotos }
        val downloads = selectedPhotos.mapNotNull { history ->
            val path = fileRepository.downloadPhotoToDirById(history.photoId)
            if (path != null) {
                history to path
            } else null
        }
        setState { copy(downloadedPhotos = downloads) }
        return downloads
    }

    fun filterDownloadedHistory() {
        withState { state ->
            val downloaded = state.downloadedPhotos
            val selected = state.selectedPhotos
            val newDownloadedList = downloaded.filter { selected.contains(it.first) }
            setState { copy(downloadedPhotos = newDownloadedList) }
        }
    }

    fun setPhotoNotEnoughEvent() {
        val event = Event.Message(R.string.timeline_photo_amount_error)
        setState { copy(event = event) }
    }

    fun selectPhoto(history: HistoryModel) {
        setState { copy(selectedPhotos = selectedPhotos.plus(history)) }
    }

    fun unselectPhoto(history: HistoryModel) {
        setState { copy(selectedPhotos = selectedPhotos.minus(history)) }
    }

    companion object : MavericksViewModelFactory<MomentViewModel, MomentState> {
        const val PHOTO_DURATION_MLS = 3000L
        const val PHOTO_MIN_COUNT = 5
        private const val AUDIO_LINK =
            "https://firebasestorage.googleapis.com/v0/b/locket-widget-e4df9.appspot.com/o/timeline.mp3?alt=media&token=86285def-3b68-411a-b2bf-e243843fda2b"

        override fun create(
            viewModelContext: ViewModelContext,
            state: MomentState
        ): MomentViewModel {
            val firestore: FirestoreDataSource by viewModelContext.activity.inject()
            val adapty: AdaptyRepository by viewModelContext.activity.inject()
            val fileRepository: FileRepository by viewModelContext.activity.inject()
            //val fFmpegUseCase: FFmpegUseCase by viewModelContext.activity.inject()
            val photoEditor: PhotoEditor by viewModelContext.activity.inject()
            return MomentViewModel(
                state,
                viewModelContext.activity.applicationContext,
                firestore,
                adapty,
                fileRepository,
//                fFmpegUseCase,
                photoEditor
            )
        }
    }
}