package com.example.timemanager

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class TimeManagerApplication : Application(), ViewModelStoreOwner {
    private val appViewModelStore: ViewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}

