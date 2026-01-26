package com.bhaskar.synctask

import android.app.Application
import com.bhaskar.synctask.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SyncTaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin {
            androidLogger()
            androidContext(this@SyncTaskApplication)
        }
    }
}
