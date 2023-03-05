package com.example.locketwidget

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewUseCase(context: Context) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    private val request = reviewManager.requestReviewFlow()

    fun startReviewFlow(activity: Activity) {
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val flow = reviewManager.launchReviewFlow(activity, task.result)
                flow.addOnCompleteListener { _ ->
                    Log.d("ReviewUseCase", "app review was shown")
                }
            } else Log.e("ReviewUseCase", "task is not Successful")
        }
    }
}