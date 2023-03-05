package com.example.locketwidget.core

import com.adapty.models.PeriodUnit
import com.adapty.models.ProductSubscriptionPeriodModel

fun provideActionIfPremium(isPremium: Boolean?, action: () -> Unit, nonPremiumAction: () -> Unit) {
    if (isPremium == true) {
        action()
    } else {
        nonPremiumAction()
    }
}

data class NotPremiumException(override val message: String = "User is not premium") : Exception(message)

object PremiumUtil {
    const val MAX_FRIENDS_COUNT = 10
    const val SCREEN_SHOW_PERIOD_DAYS = 3
    const val MAX_HISTORY_PERIOD_DAYS = 30

    fun getTrialDays(trialModel: ProductSubscriptionPeriodModel): Int? {
        return when (trialModel.unit) {
            PeriodUnit.D -> trialModel.numberOfUnits
            PeriodUnit.W -> trialModel.numberOfUnits?.let { it * 7 }
            PeriodUnit.M -> trialModel.numberOfUnits?.let { it * 30 }
            PeriodUnit.Y -> trialModel.numberOfUnits?.let { it * 365 }
            else -> null
        }
    }
}