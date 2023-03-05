package com.example.locketwidget

import android.net.Uri
import com.example.locketwidget.core.Result
import com.google.android.gms.tasks.Task
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.iosParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DeepLinkUseCase {
    companion object {
        const val INVITATION_QUERY = "invitedBy"
        private const val INVITATION_LINK = "https://smartfoxlab.page.link"
        private const val DOMAIN_PREFIX = "https://smartfoxlab.page.link"
    }

    suspend fun createLink(userId: String): Uri? {
        val userInvitationLink = "$INVITATION_LINK/?$INVITATION_QUERY=$userId"
        val shortLinkAsync = Firebase.dynamicLinks.shortLinkAsync {
            link = Uri.parse(userInvitationLink)
            domainUriPrefix = DOMAIN_PREFIX
            androidParameters("com.smartfoxlab.locket.widget") {
                minimumVersion = 1
            }
            iosParameters("com.smartfoxlab.locket.widget") {}
        }

        val result = loadDeepLink(shortLinkAsync)
        return if (result is Result.Success) result.data
        else null
    }

    private suspend fun loadDeepLink(task: Task<ShortDynamicLink>) = suspendCancellableCoroutine<Result<Uri>> { con ->
        task.addOnSuccessListener { shortDynamicLink ->
            val link = shortDynamicLink.shortLink
            if (link != null) con.resume(Result.Success(link))
            else con.resume(Result.Error(NullPointerException()))
        }.addOnFailureListener {
            con.resume(Result.Error(it))
        }
    }
}
