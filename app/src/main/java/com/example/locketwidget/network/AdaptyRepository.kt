package com.example.locketwidget.network

import android.app.Activity
import android.content.Context
import com.adapty.Adapty
import com.adapty.models.ProductModel
import com.example.locketwidget.core.NotPremiumException
import com.example.locketwidget.core.Result
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AdaptyRepository(private val context: Context) {
    companion object {
        const val APP_KEY = "hidden"
    }

    fun init(userId: String) {
        Adapty.activate(context.applicationContext, APP_KEY, userId)
    }

    suspend fun getPaywalls() = suspendCoroutine { con ->
        Adapty.getPaywalls { paywalls, _, error ->
            if (error == null) {
                if (paywalls != null) {
                    con.resume(Result.Success(paywalls))
                } else {
                    con.resume(Result.Error(NullPointerException("paywall is null")))
                }
            } else {
                con.resume(Result.Error(error))
            }
        }
    }

    suspend fun isPremium(forced: Boolean = false) = suspendCoroutine { con ->
        Adapty.getPurchaserInfo(forceUpdate = forced) { purchaserInfo, error ->
            val isPremium = purchaserInfo?.accessLevels?.get("premium")?.isActive == true
            con.resume(isPremium)
        }
    }

    suspend fun makePurchase(activity: Activity, selectedProductModel: ProductModel) = suspendCoroutine { con ->
        Adapty.makePurchase(activity, selectedProductModel) { purchaserInfo, _, _, _, error ->
            if (error == null) {
                if (purchaserInfo?.accessLevels?.get("premium")?.isActive == true) {
                    con.resume(Result.Success(Unit))
                } else {
                    con.resume(Result.Error(NotPremiumException()))
                }
            } else {
                con.resume(Result.Error(error))
            }
        }
    }
}