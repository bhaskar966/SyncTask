package com.bhaskar.synctask

import android.app.Application
import com.bhaskar.synctask.data.fcm.FCMInitializer
import com.bhaskar.synctask.di.initKoin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent

class SyncTaskApplication : Application(), KoinComponent {

    private val fcmInitializer: FCMInitializer by inject()

    override fun onCreate() {
        super.onCreate()

        try {
            Firebase.initialize(this)
            println("✅ Firebase initialized successfully")
        } catch (e: Exception) {
            println("⚠️ Firebase error: ${e.message}")
        }
        
        initKoin {
            androidLogger()
            androidContext(this@SyncTaskApplication)
        }

        println("🔥 Forcing FCMInitializer instantiation...")
        fcmInitializer
        println("✅ FCMInitializer instantiated")
    }
}
