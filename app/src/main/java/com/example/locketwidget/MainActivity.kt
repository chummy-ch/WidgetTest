package com.example.locketwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.core.LocalActivity
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.*
import com.example.locketwidget.local.LocalStorageManager
import com.example.locketwidget.messaging.LocketMessagingService.Companion.EVENT_FRIENDS
import com.example.locketwidget.messaging.LocketMessagingService.Companion.EVENT_TIMELINE
import com.example.locketwidget.messaging.LocketMessagingService.Companion.EVENT_WIDGETS
import com.example.locketwidget.network.AuthenticationManager
import com.example.locketwidget.ui.theme.LocketWidgetTheme
import com.example.locketwidget.widget.LocketWidgetProvider
import com.example.locketwidget.work.WidgetWorkUseCase
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    companion object {
        const val HISTORY_DETAILS_KEY = "history"
        const val INVITE_FRIENDS_KEY = "friends"
    }

    private lateinit var activityViewModel: ActivityViewModel

    private val authManager: AuthenticationManager by inject()
    private val widgetWorkUseCase: WidgetWorkUseCase by inject()
    private val localStorageManager: LocalStorageManager by inject()
    private val reviewUseCase: ReviewUseCase by lazy { ReviewUseCase(this) }

    private val registerGoogleLoginForActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        authManager.getActivityResultCallback { account ->
            if (account != null) {
                val token = account.idToken
                if (token != null) {
                    lifecycleScope.launch {
                        val result = authManager.firebaseAuthWithGoogle(token)
                        if (result is Result.Success) {
                            val name = account.displayName ?: ""
                            activityViewModel.createUser(
                                result.data.uid,
                                name,
                                account.photoUrl?.toString()
                            )
                        }
                    }
                }
            }
        })

    private val emailSignInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser!!
            lifecycleScope.launch {
                activityViewModel.createUser(
                    user.uid,
                    user.displayName ?: "",
                    user.photoUrl?.toString() ?: ""
                )
            }
        } else {
            authManager.logout()
        }
    }

    private val loggingCallback = object : LoggingCallback {

        override fun browseConditionLink(link: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(intent)
        }

        override fun singInGoogle() {
            this@MainActivity.signInGoogle()
        }

        override fun signInEmail() {
            this@MainActivity.signInEmail()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            activityViewModel = mavericksActivityViewModel()
            val user = activityViewModel.collectAsState(ActivityState::user).value

            CompositionLocalProvider(LocalActivity provides this) {

                LocketWidgetTheme {

                    val event = activityViewModel.collectAsState { it.event }.value
                    LocketApplication(
                        navController,
                        user,
                        loggingCallback,
                        this::logout,
                        this::cropImage,
                        event
                    )
                }
            }
            val appReviewEvent = activityViewModel.collectAsState(ActivityState::event).value
            LaunchedEffect(appReviewEvent) {
                if (appReviewEvent is Event.AppReview) {
                    reviewUseCase.startReviewFlow(this@MainActivity)
                }
            }

            LaunchedEffect(intent, user) {
                if (user is Success) {
                    activityViewModel.proceedNotificationLink(intent.extras)
                    val extras = intent.extras
                    if (extras != null) {
                        try {
                            if (extras.containsKey(HISTORY_DETAILS_KEY)) {
                                val history = extras.getParcelable<HistoryModel>(HISTORY_DETAILS_KEY) ?: return@LaunchedEffect
                                val path = "${ScreenItem.History.route}?${ScreenItem.HISTORY_ARG}=${history.toJson()}"
                                navController.navigate(path)
                                intent.removeExtra(HISTORY_DETAILS_KEY)
                            } else if (extras.containsKey(INVITE_FRIENDS_KEY)) {
                                navController.navigate(ScreenItem.Invitation.route)
                                intent.removeExtra(INVITE_FRIENDS_KEY)
                            } else if (extras.containsKey(EVENT_TIMELINE)) {
                                //   navController.navigate(ScreenItem.TimeLineEditor.route)
                                intent.removeExtra(EVENT_TIMELINE)
                            } else if (extras.containsKey(EVENT_FRIENDS)) {
                                navController.navigate(ScreenItem.Invitation.route)
                                intent.removeExtra(EVENT_FRIENDS)
                            } else if (extras.containsKey(EVENT_WIDGETS)) {
                                navController.navigate(ScreenItem.WidgetList.route)
                                addWidget(this@MainActivity)
                                intent.removeExtra(EVENT_WIDGETS)
                            }
                        } catch (exception: IllegalArgumentException) {
                            Log.e("MainActivity", exception.toString())
                        }
                    }
                }
            }
        }
        widgetWorkUseCase.createScheduleWork()
        processDynamicLink()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!) ?: return
            val path = resultUri.path ?: return
            val isPhoto = true
            val route = "${ScreenItem.PhotoPreview.route}?${ScreenItem.PREVIEW_ARG}=$path," +
                    "${ScreenItem.PREVIEW_TYPE_ARG}=$isPhoto," +
                    "${ScreenItem.EMOJIS_ARG}=${null}"
            activityViewModel.setEvent(Event.Navigate(route))
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Firebase.crashlytics.log(cropError.toString())
        }
    }

    private fun cropImage(source: Uri) {
        lifecycleScope.launch {
            val tempFileResult = localStorageManager.createTempFileFilesDir()
            if (tempFileResult.isSuccess) {
                val file = tempFileResult.getOrNull() ?: return@launch
                val destination = Uri.fromFile(file)

                UCrop.of(source, destination)
                    .withAspectRatio(9f, 9f)
                    .withMaxResultSize(1024, 1024)
                    .start(this@MainActivity)
            }
        }
    }

    private fun processDynamicLink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                lifecycleScope.launch {
                    val deepLink: Uri? = pendingDynamicLinkData?.link
                    deepLink?.let { link ->
                        val hasParameter = link.getBooleanQueryParameter(DeepLinkUseCase.INVITATION_QUERY, false)
                        if (hasParameter) {
                            val inviterId = deepLink.getQueryParameter(DeepLinkUseCase.INVITATION_QUERY) ?: return@let
                            delay(200)
                            if (authManager.getUser() == null) {
                                activityViewModel.saveInvitationLinkToDataStore(inviterId)
                            } else {
                                activityViewModel.connectUser(inviterId)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener(this) { e -> Log.w("DeepLinkUse", "getDynamicLink:onFailure", e) }
    }

    private fun logout() {
        authManager.logout()
        activityViewModel.setUser(null)
    }

    private fun signInEmail() {
        val provider = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(provider)
            .build()
        emailSignInLauncher.launch(intent)
    }

    private fun signInGoogle() {
        registerGoogleLoginForActivityResult.launch(authManager.getSignInIntent())
    }
}

@Composable
fun LocketApplication(
    navController: NavHostController,
    user: Async<FirestoreUserResponse>,
    loggingCallback: LoggingCallback,
    logout: () -> Unit,
    cropPhoto: (Uri) -> Unit,
    event: Event?
) {
    LocketNavHost(
        navController = navController,
        userAsync = user,
        logout = logout,
        loggingCallback = loggingCallback,
        cropPhoto = cropPhoto
    )

    LaunchedEffect(event) {
        event?.handleEvent(
            onNavigate = { route ->
                if (route != null) navController.navigate(route)
            },
            onConnectionCreated = { user, friend, isInvitationSender ->
                navigateToCongratulationScreen(
                    navController,
                    Event.ConnectionCreated(user, friend, isInvitationSender)
                )
            }
        )
    }
}

private fun navigateToCongratulationScreen(
    navController: NavController,
    event: Event.ConnectionCreated
) {
    val userJson = event.user.toJson()
    val friendsJson = event.friend.toJson()

    val path = "${ScreenItem.Connection.route}?" +
            "${ScreenItem.CONNECTION_USER_ARG}=$userJson," +
            "${ScreenItem.CONNECTION_FRIEND_ARG}=$friendsJson"
    if (event.isInvitationSender)
        navController.navigate(ScreenItem.Contacts.route)
    navController.navigate(path)
}

fun addWidget(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val widgetManager = context.getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(context, LocketWidgetProvider::class.java)
        val intent = Intent(context, LocketWidgetProvider::class.java)
        val callback = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        widgetManager.requestPinAppWidget(provider, Bundle(), callback)
        true
    } else {
        false
    }
}

