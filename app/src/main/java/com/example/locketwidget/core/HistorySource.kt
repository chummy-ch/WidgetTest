package com.example.locketwidget.core

import android.icu.text.SimpleDateFormat
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.HistoryModel
import com.example.locketwidget.data.HistoryResponse
import com.example.locketwidget.network.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed class HistoryPaging {
    object NonPremiumButton : HistoryPaging()
    data class Page(
        val day: String,
        val history: List<HistoryModel>
    ) : HistoryPaging()
}

data class HistoryPageModel(
    val historyResponse: List<HistoryResponse>,
    val lastDoc: DocumentSnapshot?
)

class HistorySource(
    private val firestoreDataSource: FirestoreDataSource,
    private val isPremium: Boolean,
    private val selectedUsersIds: List<ContactResponse>
) : PagingSource<DocumentSnapshot, HistoryPaging>() {

    var isOutOfPremiumRange = false

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, HistoryPaging> {
        if (!isPremium && isOutOfPremiumRange) return LoadResult.Error(NotPremiumException())

        val nextDoc = params.key
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val historyQuery: Query = firestoreDataSource.getFilteredHistoryListByPageQuery(selectedUsersIds, userId, nextDoc)
        val historyResult = historyQuery.awaitHistoryResponse()
        return when (historyResult) {
            is Result.Success -> {
                var lastDoc = historyResult.data.lastDoc
                val historyModels = historyResult.data.historyResponse.map { it.mapToHistoryModel() }
                if (lastDoc != null) {
                    val lastDayHistoryList = firestoreDataSource
                        .getDayHistory(userId, lastDoc, historyModels.last().date, selectedUsersIds)
                        .awaitHistoryResponse()

                    val groupedList = lastDayHistoryList.let {
                        if (it is Result.Success) {
                            it.data.lastDoc?.let { lastDoc = it }
                            historyModels.plus(it.data.historyResponse.map { it.mapToHistoryModel() })
                        } else {
                            historyModels
                        }
                    }.groupByDay()


                    val pagingModel: List<HistoryPaging> = if (isPremium) {
                        groupedList.map { HistoryPaging.Page(it.first, it.second) }
                    } else {
                        val currentDate = Date().time.milliseconds
                        groupedList.mapNotNull {
                            val historyDate = it.second.first().date.seconds.seconds
                            if (currentDate - historyDate <= PremiumUtil.MAX_HISTORY_PERIOD_DAYS.days) {
                                HistoryPaging.Page(it.first, it.second)
                            } else {
                                isOutOfPremiumRange = true
                                null
                            }
                        }.run {
                            if (isOutOfPremiumRange) plus(HistoryPaging.NonPremiumButton)
                            else this
                        }
                    }
                    LoadResult.Page(
                        pagingModel,
                        prevKey = nextDoc,
                        nextKey = lastDoc
                    )
                } else LoadResult.Error(NullPointerException())
            }
            is Result.Error -> {
                LoadResult.Error(historyResult.exception)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, HistoryPaging>): DocumentSnapshot? {
        if (state.anchorPosition == null) return null
        return state.pages[state.anchorPosition!!].nextKey
    }

    private fun List<HistoryModel>.groupByDay() = groupBy {
        SimpleDateFormat(
            "MMMM dd",
            Locale.US
        ).format(it.date.seconds * 1000L)
    }.toList()

    private suspend fun Query.awaitHistoryResponse() = suspendCoroutine<Result<HistoryPageModel>> { con ->
        get().addOnSuccessListener { snaphost ->
            val docs = snaphost.documents
            val list = docs.mapNotNull { it.toObject(HistoryResponse::class.java) }
            if (list.isNotEmpty()) {
                val page = HistoryPageModel(
                    historyResponse = list,
                    lastDoc = docs.lastOrNull()
                )
                con.resume(Result.Success(page))
            } else {
                con.resume(Result.Error(Exception("empty list")))
            }
        }.addOnFailureListener {
            con.resume(Result.Error(it))
        }
    }
}