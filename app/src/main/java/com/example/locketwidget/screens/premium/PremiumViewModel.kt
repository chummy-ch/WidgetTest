package com.example.locketwidget.screens.premium

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel
import com.airbnb.mvrx.*
import com.example.locketwidget.R
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.Event
import com.example.locketwidget.di.dataStore
import com.example.locketwidget.network.AdaptyRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*

data class PremiumState(
    val paywall: Async<PaywallModel> = Uninitialized,
    val selectedProduct: Async<ProductModel> = Uninitialized,
    val event: Event? = null,
    val isPremium: Async<Boolean> = Uninitialized
) : MavericksState

class PremiumViewModel(
    initState: PremiumState,
    private val dataStore: DataStore<Preferences>,
    private val adaptyRepository: AdaptyRepository
) : MavericksViewModel<PremiumState>(initState) {
    init {
        viewModelScope.launch {
            val paywalls = adaptyRepository.getPaywalls()
            when (paywalls) {
                is Result.Success -> {
                    setState {
                        copy(
                            paywall = Success(paywalls.data.first()),
                            selectedProduct = Success(paywalls.data.first().products.first())
                        )
                    }
                }
                is Result.Error -> setState { copy(paywall = Fail(paywalls.exception)) }
            }
        }
        restorePurchase(false)
    }

    fun changeSelectedProduct(selectedProduct: ProductModel) {
        setState { copy(selectedProduct = Success(selectedProduct)) }
    }

    fun clearEvent() = setState { copy(event = null) }

    fun savePremiumScreenShownTime() {
        viewModelScope.launch {
            dataStore.edit { pref ->
                pref[PREMIUM_SCREEN_SHOWN_TIME_KEY] = Date().time
            }
        }
    }

    fun restorePurchase(forceUpdate: Boolean = true) {
        viewModelScope.launch {
            setState { copy(isPremium = Loading()) }
            val isPremium = adaptyRepository.isPremium(forceUpdate)
            setState { copy(isPremium = Success(isPremium)) }
        }
    }

    fun makePurchase(activity: Activity) {
        val selectedProduct = withState(this) { it.selectedProduct.invoke() }
        if (selectedProduct == null) {
            val event = Event.Message(R.string.unknown_error)
            setState { copy(event = event) }
        } else {
            viewModelScope.launch {
                val purchaseResult = adaptyRepository.makePurchase(activity, selectedProduct)
                if (purchaseResult is Result.Success) setState { copy(isPremium = Success(true)) }
                else setState { copy(event = Event.Message(R.string.unknown_error)) }
            }
        }
    }

    companion object : MavericksViewModelFactory<PremiumViewModel, PremiumState> {
        val PREMIUM_SCREEN_SHOWN_TIME_KEY = longPreferencesKey("premium_screen_shown_time")

        override fun create(viewModelContext: ViewModelContext, state: PremiumState): PremiumViewModel {
            val dataStore: DataStore<Preferences> = viewModelContext.activity.dataStore
            val adapty: AdaptyRepository by viewModelContext.activity.inject()
            return PremiumViewModel(state, dataStore, adapty)
        }
    }
}