package com.example.locketwidget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locketwidget.network.FirebaseFunctionsManager
import org.koin.java.KoinJavaComponent.inject

class UpdateFMCTokenWork(context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TOKEN_KEY = "token"
    }

    private val firebaseFunctionsManager: FirebaseFunctionsManager by inject(FirebaseFunctionsManager::class.java)

    override suspend fun doWork(): Result {
        val token = params.inputData.getString(TOKEN_KEY)
        if (token != null) {
            firebaseFunctionsManager.updateFMCToken(token)
            return Result.success()
        }
        return Result.failure()
    }
}