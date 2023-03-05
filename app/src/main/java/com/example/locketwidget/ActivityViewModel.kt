package com.example.locketwidget

import android.os.Bundle
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.adapty.Adapty
import com.airbnb.mvrx.*
import com.example.locketwidget.core.PremiumUtil
import com.example.locketwidget.core.Result
import com.example.locketwidget.core.provideActionIfPremium
import com.example.locketwidget.data.Event
import com.example.locketwidget.data.FirestoreUserResponse
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.local.LocalStorageManager
import com.example.locketwidget.messaging.HistoryMessageModel
import com.example.locketwidget.messaging.LocketMessagingService
import com.example.locketwidget.network.*
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds


data class ActivityState(
    val user: Async<FirestoreUserResponse> = Uninitialized,
    val navigationGraph: String = ScreenItem.Splash.route,
    val message: Async<String> = Uninitialized,
    val event: Event? = null
) : MavericksState

class ActivityViewModel(
    initState: ActivityState,
    private val dataStore: DataStore<Preferences>,
    private val firebaseFunctionsManager: FirebaseFunctionsManager,
    private val fmcService: LocketMessagingService,
    private val firestoreDataSource: FirestoreDataSource,
    private val localStorageManager: LocalStorageManager,
    private val fileRepository: FileRepository,
    private val authManager: AuthenticationManager,
    private val adaptyRepository: AdaptyRepository
) : MavericksViewModel<ActivityState>(initState) {
    init {
        viewModelScope.launch {
            authManager.loadUser()
            updateFMCToken()
            saveFirstEntersDate()
            subscribeOnSuccessPhotoSendPreference()
        }
        viewModelScope.launch {
            authManager.firebaseUser.collect { user ->
                setState { copy(user = user) }
                if (user is Success) {
                    processSavedLink()
                    adaptyRepository.init(user.invoke().id)
                    showPremiumScreen()
                }
                setSingUpDateForOldUsers()
            }
        }
    }

    private suspend fun showPremiumScreen() {
        Adapty.getPurchaserInfo { purchaserInfo, error ->
            val isPremium = purchaserInfo?.accessLevels?.get("premium")?.isActive == true
            if (!isPremium) {
                viewModelScope.launch {
                    val currentTimeMills = Date().time.milliseconds
                    val firstEntersDate = getFirstEnterDate() ?: return@launch

                    if (currentTimeMills - firstEntersDate.milliseconds >= 1.days) {
                        val lastShownTime = getLastPremiumScreenShownTime()
                        if (lastShownTime == null) {
                            setEvent(Event.Navigate(ScreenItem.Premium.route))
                        } else {
                            if (currentTimeMills - lastShownTime.milliseconds >= PremiumUtil.SCREEN_SHOW_PERIOD_DAYS.days) {
                                setEvent(Event.Navigate(ScreenItem.Premium.route))
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            authManager.firebaseUser.collect { user ->
                setState { copy(user = user) }
                setSingUpDateForOldUsers()
            }
        }
    }

    private suspend fun getLastPremiumScreenShownTime() = dataStore.data.first()[PremiumViewModel.PREMIUM_SCREEN_SHOWN_TIME_KEY]

    private suspend fun getFirstEnterDate() = dataStore.data.first()[FIST_ENTERS_DATE]

    private suspend fun saveFirstEntersDate() {
        dataStore.edit { pref ->
            if (pref[FIST_ENTERS_DATE] == null) {
                pref[FIST_ENTERS_DATE] = Date().time
            }
        }
    }

    private fun setSingUpDateForOldUsers() {
        viewModelScope.launch {
            if (dataStore.data.first()[SING_UP_DATE_KEY] == null) {
                dataStore.edit { pref ->
                    pref[SING_UP_DATE_KEY] = Calendar.getInstance().timeInMillis
                }
            }
        }
    }

    private suspend fun subscribeOnSuccessPhotoSendPreference() {
        val isSuccessPhotoSend: Flow<Boolean> = dataStore.data
            .map { preferences ->
                preferences[IS_SUCCESS_PHOTO_SEND_KEY] ?: false
            }
        isSuccessPhotoSend.collect { isPhotoSend ->
            if (isPhotoSend) {
                val lastShownDate = dataStore.data.first()[APP_RATE_SHOWN_DATE_KEY] ?: 0
                if (lastShownDate == 0L) {
                    val isHistoryEmpty = dataStore.data.first()[IS_HISTORY_EMPTY_KEY] ?: true
                    if (!isHistoryEmpty) {
                        val singUpDate = dataStore.data.first()[SING_UP_DATE_KEY]
                        val singUpCalendar = Calendar.getInstance()
                        if (singUpDate != null) {
                            singUpCalendar.time = Date(singUpDate)
                            singUpCalendar.add(Calendar.DATE, 2)
                        }
                        if (singUpCalendar <= Calendar.getInstance()) {
                            withState {
                                setEvent(Event.AppReview)
                                viewModelScope.launch {
                                    dataStore.edit { pref ->
                                        pref[IS_SUCCESS_PHOTO_SEND_KEY] = false
                                        pref[APP_RATE_SHOWN_DATE_KEY] =
                                            Calendar.getInstance().timeInMillis
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkAddFriendsScreenStatus() {
        val status = dataStore.data.first()[ADD_PRIMARY_SCREENS_STATUS_KEY] ?: false
        if (!status) {
            val event = Event.Navigate(ScreenItem.WidgetOnboardingGuide.route)
            setEvent(event)
            dataStore.edit { pref -> pref[ADD_PRIMARY_SCREENS_STATUS_KEY] = true }
        }
    }

    fun setEvent(event: Event?) {
        setState { copy(event = event) }
    }

    private suspend fun updateFMCToken() {
        viewModelScope.launch {
            if (shouldUpdateFMCToken()) {
                val token = fmcService.updateToken()
                if (token is Result.Success) {
                    val result = firebaseFunctionsManager.updateFMCToken(token.data)
                    if (result is Result.Success) {
                        saveTokenSaveTime(Date().time)
                    }
                }
            }
        }
    }

    private suspend fun shouldUpdateFMCToken(): Boolean {
        val lastTime = dataStore.data.first()[TOKEN_UPDATE_TIME_KEY] ?: 0L
        val currentTime = Date().time
        return (currentTime - lastTime) / 1000 >= SYNC_PERIOD_SEC
    }

    private suspend fun saveTokenSaveTime(time: Long) {
        dataStore.edit { pref ->
            pref[TOKEN_UPDATE_TIME_KEY] = time
        }
    }

    private fun updateName(name: String) {
        viewModelScope.launch {

            val result = firebaseFunctionsManager.updateUserName(name)
            if (result is Result.Success) {
                withState { state ->
                    val newUser = state.user.invoke() ?: return@withState
                    setState { copy(user = Success(newUser.copy(name = name))) }
                }
            } else {
                withState { state ->
                    val newUser = state.user.invoke() ?: return@withState
                    setState { copy(user = Success(newUser)) }
                }
            }
        }
    }

    fun createTempFile() {
        viewModelScope.launch {
            val result = localStorageManager.createTempFileFilesDir()
            if (result.isSuccess) {
                val file = result.getOrNull() ?: return@launch
                val uri = file.toUri()
                setEvent(Event.Share(uri))
            }
        }
    }

    fun updateUser(name: String, photoLink: String) {
        updateName(name)
        updateUserPhoto(photoLink)
        val user = withState(this) { it.user.invoke() } ?: return
        setState { copy(user = Success(user)) }
        viewModelScope.launch {
            updateFMCToken()
            checkAddFriendsScreenStatus()
        }
    }

    fun createUser(id: String, name: String, photoLink: String?) {
        setState { copy(user = Loading()) }

        viewModelScope.launch {
            val result = firebaseFunctionsManager.createUserInDB(name, photoLink)
            if (result is Result.Success) {
                val user = FirestoreUserResponse(id, name, photoLink)
                setState { copy(user = Loading(user)) }
                processSavedLink()
                updateFMCToken()
            } else {
                setState { copy(user = Fail((result as Result.Error).exception)) }
            }
            dataStore.edit { pref ->
                pref[SING_UP_DATE_KEY] = Calendar.getInstance().timeInMillis
            }
        }
    }

    fun saveInvitationLinkToDataStore(id: String) {
        viewModelScope.launch {
            dataStore.edit { pref ->
                pref[INVITATION_ID_KEY] = id
            }
        }
    }

    fun proceedNotificationLink(extras: Bundle?) {
        if (extras != null && !extras.isEmpty) {
            viewModelScope.launch {
                if (extras.containsKey(LocketMessagingService.REACTION_HISTORY_KEY)) {
                    val historyNotificationModel = extras.getParcelable<HistoryMessageModel>(LocketMessagingService.REACTION_HISTORY_KEY)
                    if (historyNotificationModel != null) {
                        proceedReactionNotificationLink(historyNotificationModel)
                    }
                } else if (extras.containsKey(LocketMessagingService.SENDER_ID_KEY)) {
                    val senderId = extras.getString(LocketMessagingService.SENDER_ID_KEY)
                    if (senderId != null) {
                        proceedConnectionNotificationLink(senderId)
                    }
                }
            }
        }
    }

    private suspend fun proceedConnectionNotificationLink(senderId: String) {
        val senderResult = firestoreDataSource.getUser(senderId)
        if (senderResult is Result.Success) {
            val user = authManager.requireUser()
            setEvent(Event.ConnectionCreated(user, senderResult.data, true))
        }
    }

    private suspend fun proceedReactionNotificationLink(historyNotificationModel: HistoryMessageModel) {
        historyNotificationModel.let {
            val currentUser = authManager.requireUser()

            val historyModel = HistoryModel(
                date = Timestamp(Date(historyNotificationModel.date * 1000)),
                photoLink = FileRepository.getPhotoLinkById(historyNotificationModel.photoId),
                photoId = historyNotificationModel.photoId,
                senderId = currentUser.id,
                mode = historyNotificationModel.mode
            )

            val navigationEvent = Event.Navigate(
                route = ScreenItem.getRouteForHistory(historyModel)
            )
            setState { copy(event = navigationEvent) }
        }
    }

    private suspend fun updateUserPhotoLink(photoLink: String) {
        val firebaseUploadResult = firebaseFunctionsManager.updateUserPhoto(photoLink)
        if (firebaseUploadResult is Result.Success) {
            val user = withState(this@ActivityViewModel) { it.user.invoke() } ?: return
            setState { copy(user = Success(user.copy(photoLink = photoLink))) }
        }
    }

    private fun updateUserPhoto(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uri.isEmpty()) return@launch
            if (!uri.contains("https")) {
                val file = localStorageManager.pickPhoto(uri)
                if (file is Result.Success) {
                    val urlResult = firebaseFunctionsManager.createUploadUrl()

                    if (urlResult is Result.Success) {
                        val uploadResult = fileRepository.uploadPhoto(urlResult.data.uploadUrl, file.data)

                        if (uploadResult is Result.Success) {
                            val photoLink = FileRepository.getPhotoLinkById(uploadResult.data.result.id)
                            updateUserPhotoLink(photoLink)
                        }
                    }
                }
            } else {
                updateUserPhotoLink(uri)
            }
        }
    }

    private fun processSavedLink() {
        viewModelScope.launch {
            val hostId = dataStore.data.first()[INVITATION_ID_KEY]
            if (hostId.isNullOrEmpty()) return@launch

            connectUser(hostId)

            dataStore.edit { it[INVITATION_ID_KEY] = "" }
        }
    }

    fun connectUser(hostId: String) {
        viewModelScope.launch {
            val user = withState(this@ActivityViewModel) { it.user }.invoke()
            if (user != null) {
                val connectionResult = firestoreDataSource.getConnectionIds(user.id)
                if (connectionResult is Result.Success) {
                    if (connectionResult.data.size < PremiumUtil.MAX_FRIENDS_COUNT) {
                        createConnection(hostId, user)
                    } else {
                        Adapty.getPurchaserInfo { purchaserInfo, _ ->
                            val isPremium = purchaserInfo?.accessLevels?.get("premium")?.isActive == true
                            provideActionIfPremium(
                                isPremium = isPremium,
                                action = {
                                    launch { createConnection(hostId, user) }
                                },
                                nonPremiumAction = {
                                    setEvent(Event.Navigate("${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}=1"))
                                }
                            )
                        }
                    }
                }
            } else {
                saveInvitationLinkToDataStore(hostId)
            }
        }
    }

    private suspend fun createConnection(hostId: String, user: FirestoreUserResponse) {
        val result = firebaseFunctionsManager.createConnection(hostId)
        if (result is Result.Success) {
            val friendResult = firestoreDataSource.getUser(hostId)
            val userResult = firestoreDataSource.getUser(user.id)
            if (friendResult is Result.Success && userResult is Result.Success) {
                setEvent(
                    Event.ConnectionCreated(
                        userResult.data,
                        friendResult.data,
                        false
                    )
                )
            }
        }
    }

    fun setUser(user: FirestoreUserResponse?) {
        setState {
            if (user != null) copy(user = Success(user))
            else copy(user = Fail(NullPointerException()))
        }
    }

    fun setMessageEvent(message: Int) {
        val event = Event.Message(message)
        setState { copy(event = event) }
    }

    fun clearEvent() = setState { copy(event = null) }

    companion object : MavericksViewModelFactory<ActivityViewModel, ActivityState> {
        private val INVITATION_ID_KEY = stringPreferencesKey("invitation_key")
        private val TOKEN_UPDATE_TIME_KEY = longPreferencesKey("token_update_time")
        private val ADD_PRIMARY_SCREENS_STATUS_KEY = booleanPreferencesKey("add_friends_screen")
        private val APP_RATE_SHOWN_DATE_KEY = longPreferencesKey("app_rate_shown_date_key")
        val IS_SUCCESS_PHOTO_SEND_KEY = booleanPreferencesKey("is_success_photo_send_key")
        private val SING_UP_DATE_KEY = longPreferencesKey("sing_up_date")
        val IS_HISTORY_EMPTY_KEY = booleanPreferencesKey("is_history_empty")
        private val FIST_ENTERS_DATE = longPreferencesKey("firs_enters")
        private const val SYNC_PERIOD_SEC = 86400
        const val HISTORY_AMOUNT = 20L

        override fun create(
            viewModelContext: ViewModelContext,
            state: ActivityState
        ): ActivityViewModel {
            with(viewModelContext.activity) {
                val dataStore: DataStore<Preferences> = applicationContext.dataStore
                val firebaseFunctions: FirebaseFunctionsManager by inject()
                val fmc: LocketMessagingService by inject()
                val firestore: FirestoreDataSource by inject()
                val localStorageManager: LocalStorageManager by inject()
                val fileRepository: FileRepository by inject()
                val authManager: AuthenticationManager by inject()
                val adaptyRepository: AdaptyRepository by inject()
                return ActivityViewModel(
                    state,
                    dataStore,
                    firebaseFunctions,
                    fmc,
                    firestore,
                    localStorageManager,
                    fileRepository,
                    authManager,
                    adaptyRepository
                )
            }
        }
    }
}

