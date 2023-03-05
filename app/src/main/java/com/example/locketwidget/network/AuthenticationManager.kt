package com.example.locketwidget.network

import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import com.adapty.Adapty
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.FirestoreUserResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotInitializedUser : Exception("User is not initialized")

class AuthenticationManager(
    private val firebaseAuth: FirebaseAuth,
    private val googleClient: GoogleSignInClient,
    private val firestoreDataSource: FirestoreDataSource
) {
    private val _firebaseUser = MutableStateFlow<Async<FirestoreUserResponse>>(Uninitialized)
    val firebaseUser: StateFlow<Async<FirestoreUserResponse>> = _firebaseUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                Adapty.identify(currentUserId) {}
            } else {
                Adapty.logout {}
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        googleClient.signOut()
        _firebaseUser.value = Fail(NotInitializedUser())
    }

    suspend fun requireUser(): FirestoreUserResponse {
        return if (firebaseUser.value is Success) {
            firebaseUser.value.invoke()!!
        } else {
            when (val userResult = firestoreDataSource.getUser(getUser()!!.uid)) {
                is Result.Success -> {
                    userResult.data
                }
                is Result.Error -> {
                    throw userResult.exception
                }
            }
        }
    }

    suspend fun loadUser() {
        val firebaseUser = getUser()
        if (firebaseUser == null) {
            _firebaseUser.value = Fail(NotInitializedUser())
        } else {
            val userResult = firestoreDataSource.getUser(firebaseUser.uid)
            _firebaseUser.value = if (userResult is Result.Success) {
                Success(userResult.data)
            } else {
                Fail(NotInitializedUser())
            }
        }
    }

    fun getFirestoreUser() = firebaseUser.value

    fun getUser(): FirebaseUser? = firebaseAuth.currentUser

    fun getSignInIntent() = googleClient.signInIntent

    suspend fun firebaseAuthWithGoogle(token: String) = suspendCoroutine<Result<FirebaseUser>> { con ->
        val credential = GoogleAuthProvider.getCredential(token, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser != null) con.resume(Result.Success(firebaseUser))
                else con.resume(Result.Error(NullPointerException()))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }

    fun getActivityResultCallback(callback: (GoogleSignInAccount?) -> Unit) =
        ActivityResultCallback<ActivityResult> { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val accountResult = kotlin.runCatching {
                task.getResult(ApiException::class.java)
            }
            if (accountResult.isSuccess) {
                val user = accountResult.getOrThrow()
                callback(user)
            } else {
                callback(null)
            }
        }
}
