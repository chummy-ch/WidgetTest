package com.example.locketwidget.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.emoji_mashup.EmojiMashupUtil
import com.example.locketwidget.AppVibrationUseCase
import com.example.locketwidget.DeepLinkUseCase
import com.example.locketwidget.R
import com.example.locketwidget.local.*
import com.example.locketwidget.messaging.LocketMessagingService
import com.example.locketwidget.network.*
import com.example.locketwidget.screens.textmoments.TextMomentBackgroundRepository
import com.example.locketwidget.widget.WidgetManager
import com.example.locketwidget.work.WidgetWorkUseCase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val mainModule = module {
    single { WidgetWorkUseCase(androidContext()) }
    single { LocalStorageManager(context = androidContext()) }
    single { WidgetManager(context = androidContext(), photoEditor = get()) }
    factory { DeepLinkUseCase() }

    single { RetrofitClient() }
    single { FileRepository(client = get(), context = androidContext()) }
    single { FirebaseFunctionsManager() }
    single { FirestoreDataSource() }
    single { LocketMessagingService() }

    single { LocalWidgetsRepository(dataStore = androidContext().dataStore) }

    single {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(androidContext().resources.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val googleSingInClient = GoogleSignIn.getClient(androidContext(), gso)
        AuthenticationManager(
            firebaseAuth = FirebaseAuth.getInstance(),
            googleClient = googleSingInClient,
            firestoreDataSource = get()
        )
    }

    factory { MediaScanner(context = androidContext()) }
    single { PhotoEditor(context = androidContext()) }

    single { AppVibrationUseCase(androidContext()) }
    single { TextMomentBackgroundRepository() }
    single { AdaptyRepository(context = androidContext()) }

    // TODO: Timeline
    //factory { FFmpegUseCase() }
    single { ContactsFilterUseCase() }
    factory { EmojiMashupUtil(context = androidContext()) }
}
