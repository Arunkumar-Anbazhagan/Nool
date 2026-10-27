package com.nool.app

import android.app.Application
import com.nool.app.data.index.IndexManager
import com.nool.app.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NoolApplication)
            modules(appModule)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val indexManager: IndexManager = get()
                indexManager.rebuildIndex()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
