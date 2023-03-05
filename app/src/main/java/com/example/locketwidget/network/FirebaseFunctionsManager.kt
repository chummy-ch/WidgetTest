package com.example.locketwidget.network

import com.example.locketwidget.core.Result
import com.example.locketwidget.data.PhotoResponse
import com.example.locketwidget.data.ReactionResponse
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseFunctionsManager {
    companion object {
        const val VIDEO_URL_KEY = "url"
        const val VIDEO_URL_PROGRESS_KEY = "percentComplete"
    }

    private val functions = Firebase.functions

    suspend fun createVideoDownloadLink(id: String) = suspendCoroutine<Result<Map<String, Any>>> { con ->
        val data = hashMapOf(
            "videoId" to id
        )
        functions.getHttpsCallable("createVideoDownloadUrl")
            .call(data)
            .addOnSuccessListener {
                val result = (it.data as? HashMap<String, Any>)
                if (result != null && result.containsKey(VIDEO_URL_KEY) && result.containsKey(VIDEO_URL_PROGRESS_KEY)) {
                    con.resume(Result.Success(result))
                } else {
                    con.resume(Result.Error(NullPointerException()))
                }
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun updateFMCToken(token: String) = suspendCoroutine<Result<Unit>> { con ->
        val data = hashMapOf(
            "token" to token
        )
        functions.getHttpsCallable("updatePushToken")
            .call(data)
            .addOnSuccessListener {
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }

    suspend fun sendReaction(emoji: String, photoId: String) = suspendCoroutine<Result<List<ReactionResponse>>> { con ->
        val data = hashMapOf(
            "photoId" to photoId,
            "emoji" to emoji
        )
        functions.getHttpsCallable("addReaction")
            .call(data)
            .addOnSuccessListener {
                val result = it.data as? HashMap<Any, Any>
                if (result != null) {
                    val reactions = result["reactions"]!!
                    val response = (reactions as ArrayList<HashMap<String, Any>>).map { map ->
                        val emojiString = map["emoji"] as String
                        val sender = (map["sender"] as HashMap<String, Any>).toDocumentRef()
                        ReactionResponse(
                            emoji = emojiString,
                            sender = sender
                        )
                    }
                    con.resume(Result.Success(response))
                } else {
                    con.resume(Result.Error(Exception("Result null")))
                }
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun createVideoUploadUrl() = suspendCoroutine<Result<PhotoResponse>> { con ->
        functions.getHttpsCallable("createVideoUploadUrl")
            .call()
            .addOnSuccessListener {
                val result = it.data as? HashMap<String, String>
                if (result != null) {
                    val url = result["uploadURL"]
                    val id = result["id"]
                    if (url != null && id != null) {
                        con.resume(Result.Success(PhotoResponse(id, url)))
                    } else con.resume(Result.Error(NullPointerException("url = $url, id = $id")))
                } else {
                    con.resume(Result.Error(NullPointerException()))
                }
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun createUploadUrl() = suspendCoroutine<Result<PhotoResponse>> { con ->
        functions.getHttpsCallable("createUploadURL")
            .call()
            .addOnSuccessListener {
                val result = it.data as? HashMap<String, String>
                if (result != null) {
                    val url = result["uploadURL"]
                    val id = result["id"]
                    if (url != null && id != null) {
                        con.resume(Result.Success(PhotoResponse(id, url)))
                    } else con.resume(Result.Error(NullPointerException("url = $url, id = $id")))
                } else {
                    con.resume(Result.Error(NullPointerException()))
                }
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun removeConnection(id: String) = suspendCoroutine<Result<Unit>> { con ->
        val data = hashMapOf(
            "connectionId" to id
        )
        functions.getHttpsCallable("removeConnection")
            .call(data)
            .addOnSuccessListener {
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    /*suspend fun addLivePhoto(mainPhotoId: String, secondPhotoId: String, ids: List<String>) = suspendCoroutine { con ->
        val data = hashMapOf(
            "photoId" to mainPhotoId,
            "receivers" to ids,
            "live" to secondPhotoId
        )

        functions.getHttpsCallable("addPhoto")
    }*/

    suspend fun addPhoto(photoId: String, ids: List<String>, video: String? = null, live: String? = null, emojis: List<String>? = null) =
        suspendCoroutine { con ->
            val data = hashMapOf(
                "photoId" to photoId,
                "receivers" to ids,
                "live" to live,
                "video" to video,
                "emojis" to emojis
            )

            functions.getHttpsCallable("addPhoto")
                .call(data)
                .addOnSuccessListener {
                    con.resume(Result.Success(Unit))
                }
                .addOnFailureListener {
                    con.resume(Result.Error(it))
                }
        }

    suspend fun updateUserPhoto(link: String) = suspendCoroutine<Result<Unit>> { con ->
        val data = hashMapOf(
            "photo" to link
        )
        functions.getHttpsCallable("updatePhoto")
            .call(data)
            .addOnSuccessListener {
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }

    suspend fun updateUserName(name: String) = suspendCoroutine<Result<Unit>> { con ->
        val data = hashMapOf(
            "name" to name
        )
        functions.getHttpsCallable("updateName")
            .call(data)
            .addOnSuccessListener { con.resume(Result.Success(Unit)) }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun createUserInDB(displayName: String, photoUrl: String?) = suspendCoroutine<Result<Unit>> { con ->
        val data = mutableMapOf(
            "displayName" to displayName,
        )
        if (photoUrl != null) data["photo"] = photoUrl

        functions.getHttpsCallable("createUserInDB")
            .call(data)
            .addOnSuccessListener {
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }

    suspend fun createConnection(hostId: String) = suspendCoroutine<Result<Unit>> { con ->
        val data = hashMapOf(
            "hostId" to hostId
        )

        functions.getHttpsCallable("createConnection")
            .call(data)
            .addOnSuccessListener {
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }
}

fun HashMap<String, Any>.toDocumentRef(): DocumentReference {
    val path = this["_path"] as HashMap<String, Any>
    val segments = path["segments"] as ArrayList<String>
    val docPath = segments.joinToString(separator = "/")
    return Firebase.firestore.document(docPath)
}
