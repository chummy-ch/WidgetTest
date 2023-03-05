package com.example.locketwidget.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner


class CustomLifecycleScope : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private lateinit var scope: LifecycleOwner
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner
    private lateinit var savedStateRegistryOwner: SavedStateRegistryOwner

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryOwner.savedStateRegistry

    fun setScope(lifeScope: LifecycleOwner) {
        scope = lifeScope
        require(lifeScope is ViewModelStoreOwner) { "Scope should implement ViewModelStoreOwner" }
        viewModelStoreOwner = lifeScope

        require(lifeScope is SavedStateRegistryOwner) { "Scope should implement SavedStateRegistryOwner" }
        savedStateRegistryOwner = lifeScope
    }

    override fun getLifecycle(): Lifecycle {
        return scope.lifecycle
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStoreOwner.viewModelStore
    }
}