package com.example.locketwidget.network

import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class EmptyHistoryList(error: String) : Exception(error)

class FirestoreDataSource {
    companion object {
        const val USER_COLLECTION = "users"
        private const val REACTION_COLLECTION = "reactions"
        private const val HISTORY_COLLECTION = "history"
        private const val DATE_FIELD = "date"
        private const val SENDER_FIELD = "sender"
        private const val PHOTO_FIELD = "photo"
        private const val NAME_FIELD = "name"
        private const val CONNECTIONS_FIELD = "connections"
        const val HISTORY_LOAD_AMOUNT = 15L
    }

    private val db = Firebase.firestore

    suspend fun getUser(userDoc: DocumentReference) = suspendCoroutine<Result<FirestoreUserResponse>> { con ->
        userDoc.get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString(NAME_FIELD) ?: ""
                val photo = snapshot.getString(PHOTO_FIELD) ?: ""

                val firestoreUserResponse =
                    FirestoreUserResponse(name = name, photoLink = photo).copy(id = snapshot.id)
                con.resume(Result.Success(firestoreUserResponse))
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun getUser(userId: String) = suspendCoroutine<Result<FirestoreUserResponse>> { con ->
        db.collection(USER_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString(NAME_FIELD) ?: ""
                val photo = snapshot.getString(PHOTO_FIELD) ?: ""

                val firestoreUserResponse =
                    FirestoreUserResponse(name = name, photoLink = photo).copy(id = snapshot.id)
                con.resume(Result.Success(firestoreUserResponse))
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    private fun QuerySnapshot.getHistory(): List<HistoryResponse> {
        return documents.mapNotNull { doc ->
            doc.toObject(HistoryResponse::class.java)
        }
    }

    private fun QuerySnapshot.getContacts(): List<ContactResponse> {
        val docs = this.documents
        val list = docs.map { doc ->
            ContactResponse(
                id = doc.id,
                name = doc.getString(NAME_FIELD) ?: "",
                photoUrl = doc.getString(PHOTO_FIELD)
            )
        }
        return list
    }

    private suspend fun Query.getQueryContactsSnapshotFlow() = callbackFlow {
        val listener = addSnapshotListener { value, error ->
            if (error != null) {
                cancel(CancellationException(error.message))
                return@addSnapshotListener
            }
            if (value != null) {
                trySend(value.getContacts())
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    suspend fun getUserContactsFlow(userId: String): Flow<List<ContactResponse>> {
        val userDoc = db.collection(USER_COLLECTION).document(userId)
        return db.collection(USER_COLLECTION)
            .whereArrayContainsAny(CONNECTIONS_FIELD, listOf(userDoc))
            .getQueryContactsSnapshotFlow()
    }


    suspend fun getUserContacts(userId: String) = suspendCoroutine<Result<List<ContactResponse>>> { con ->
        val userDoc = db.collection(USER_COLLECTION).document(userId)
        db.collection(USER_COLLECTION).whereArrayContainsAny(CONNECTIONS_FIELD, listOf(userDoc)).get()
            .addOnSuccessListener { query ->
                con.resume(Result.Success(query.getContacts()))
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    private suspend fun getContactsDocs(userId: String) = suspendCoroutine<Result<List<DocumentReference>>> { con ->
        db.collection(USER_COLLECTION).document(userId).get()
            .addOnSuccessListener { snapshot ->
                val friends = snapshot.get(CONNECTIONS_FIELD) as? List<DocumentReference>

                if (friends != null) con.resume(Result.Success(friends))
                else con.resume(Result.Error(java.lang.NullPointerException()))
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }

    suspend fun getReactionsFlow(photoId: String): Flow<List<ReactionResponse>> {
        return db.collection(REACTION_COLLECTION)
            .document(photoId)
            .getQueryReactionsSnapshotFlow()
    }

    private fun DocumentReference.getQueryReactionsSnapshotFlow() = callbackFlow {
        val listener = addSnapshotListener { value, error ->
            if (error != null) {
                cancel(CancellationException(error.message))
                return@addSnapshotListener
            }
            if (value != null) {
                val maps = value.get("list") as? List<Map<String, Any>>
                if (maps != null) {
                    val responses = maps.map {
                        ReactionResponse(
                            emoji = it["emoji"] as String,
                            sender = it["sender"] as DocumentReference
                        )
                    }
                    trySend(responses)
                }
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    suspend fun getPhotoHistoryFlow(userId: String): Flow<List<HistoryResponse>> {
        return db.collection(USER_COLLECTION)
            .document(userId)
            .collection(HISTORY_COLLECTION)
            .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
            .limit(ActivityViewModel.HISTORY_AMOUNT)
            .getQueryHistorySnapshotFlow()
    }

    private suspend fun Query.getQueryHistorySnapshotFlow() = callbackFlow {
        val listener = addSnapshotListener { value, error ->
            if (error != null) {
                cancel(CancellationException(error.message))
                return@addSnapshotListener
            }
            if (value != null) {
                trySend(value.getHistory())
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    fun getDayHistory(userId: String, lastSnapshot: DocumentSnapshot, dayTimestamp: Timestamp, sendersIds: List<ContactResponse>): Query {
        val nextDate = Date((dayTimestamp.seconds.seconds - 1.days).inWholeDays.days.inWholeMilliseconds)
        val sendersDocumentReference = sendersIds.map { db.collection(USER_COLLECTION).document(it.id) }
        return db.collection(USER_COLLECTION)
            .document(userId)
            .collection(HISTORY_COLLECTION)
            .run {
                if (sendersDocumentReference.isNotEmpty()) whereIn(SENDER_FIELD, sendersDocumentReference)
                else this
            }
            .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
            .whereGreaterThan(DATE_FIELD, nextDate)
            .whereLessThan(DATE_FIELD, dayTimestamp.toDate())
            .startAfter(lastSnapshot)
    }

    fun getFilteredHistoryListByPageQuery(
        sendersIds: List<ContactResponse>,
        userId: String,
        lastSnapshot: DocumentSnapshot?,
        amount: Long = HISTORY_LOAD_AMOUNT
    ): Query {
        val orderedQuery: Query
        val sendersDocumentReference = sendersIds.map { db.collection(USER_COLLECTION).document(it.id) }
        orderedQuery = db.collection(USER_COLLECTION)
            .document(userId)
            .collection(HISTORY_COLLECTION)
            .run {
                if (sendersDocumentReference.isNotEmpty()) whereIn(SENDER_FIELD, sendersDocumentReference)
                else this
            }
            .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
        return if (lastSnapshot != null) {
            orderedQuery.startAfter(lastSnapshot).limit(amount)
        } else
            orderedQuery.limit(amount)
    }

    suspend fun getLastPhotoFromSenders(
        userId: String,
        senders: List<String>
    ): Result<HistoryResponse> {
        val sendersDocs = if (senders.isNotEmpty()) {
            senders.map { id -> db.collection(USER_COLLECTION).document(id) }
        } else {
            val result = getContactsDocs(userId)
            if (result is Result.Success) result.data
            else null
        }

        val photo = if (sendersDocs != null && sendersDocs.isNotEmpty()) {
            db.collection(USER_COLLECTION)
                .document(userId)
                .collection(HISTORY_COLLECTION)
                .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                .whereIn(SENDER_FIELD, sendersDocs)
                .limit(1)
        } else {
            null
        }
        return photo?.getHistoryResponse() ?: Result.Error(NullPointerException())
    }

    private suspend fun Query.getHistoryResponse() = suspendCoroutine<Result<HistoryResponse>> { con ->
        get().addOnSuccessListener {
            val docs = it.documents
            if (docs.isNotEmpty()) {
                val response = it.documents.first().toObject(HistoryResponse::class.java)

                if (response != null) con.resume(Result.Success(response))
                else con.resume(Result.Error(NullPointerException()))
            } else {
                con.resume(Result.Error(EmptyHistoryList("User has no history")))
            }
        }.addOnFailureListener {
            con.resume(Result.Error(it))
        }
    }

    private suspend fun Query.getQueryPhotosSnapshotFlow() = callbackFlow {
        val listener = addSnapshotListener { value, error ->
            if (error != null) {
                cancel(CancellationException(error.message))
                return@addSnapshotListener
            }
            if (value != null) {
                trySend(value.getPhotos())
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    private fun QuerySnapshot.getPhotos(): List<WidgetPhotoModel> {
        val list = documents.mapNotNull { doc ->
            val sender = doc.getDocumentReference(SENDER_FIELD)
            val photo = doc.getString(PHOTO_FIELD)
            if (sender != null && photo != null) {
                WidgetPhotoModel(
                    id = photo,
                    sender = sender.id
                )
            } else null
        }
        return list
    }

    suspend fun hideHistoryById(photoId: String, userId: String) = suspendCoroutine<Result<Unit>> { con ->
        db.collection(USER_COLLECTION)
            .document(userId)
            .collection(HISTORY_COLLECTION)
            .whereEqualTo(PHOTO_FIELD, photoId)
            .get()
            .addOnSuccessListener {
                it.documents.forEach { doc ->
                    doc.reference.delete()
                }
                con.resume(Result.Success(Unit))
            }
            .addOnFailureListener {
                con.resume(Result.Error(it))
            }
    }

    suspend fun getConnectionIds(userId: String) = suspendCoroutine<Result<List<String>>> { con ->
        db.collection(USER_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val connections = document.get("connections") as? List<DocumentReference>
                if (connections == null) {
                    con.resume(Result.Error(TypeCastException("Error cast field to List<DocumentReference>")))
                } else {
                    val ids = connections.map { it.id }
                    con.resume(Result.Success(ids.filter { it != userId }))
                }
            }
            .addOnFailureListener { con.resume(Result.Error(it)) }
    }
}
