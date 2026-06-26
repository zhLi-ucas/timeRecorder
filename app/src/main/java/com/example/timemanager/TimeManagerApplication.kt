package com.example.timemanager

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeManagerApplication : Application(), ViewModelStoreOwner {
    private val appViewModelStore: ViewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        appScope.launch {
            val seeder = DefaultDataSeeder(db)
            seeder.seedIfNeeded()
            seeder.seedV1_2IfNeeded()
        }
    }
}
