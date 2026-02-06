package com.bhaskar.synctask

import android.app.Application
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.fcm.FCMInitializer
import com.bhaskar.synctask.di.initKoin
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent

class SyncTaskApplication : Application(), KoinComponent {

    private val fcmInitializer: FCMInitializer by inject()
    private val authManager: AuthManager by inject()
    private val subscriptionRepository: SubscriptionRepository by inject()

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
        
        // Initialize RevenueCat SDK
        initializeRevenueCat()
    }
    
    private fun initializeRevenueCat() {
        val apiKey = BuildConfig.REVENUECAT_API_KEY
        if (apiKey.isBlank()) {
            println("⚠️ RevenueCat API key not configured")
            return
        }
        
        // Get Firebase UID if user is already logged in
        val firebaseUserId = authManager.currentUserId
        
        // Configure RevenueCat SDK with optional user ID
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(apiKey = apiKey) {
            appUserId = firebaseUserId  // Will be null if not logged in (anonymous user)
        }
        
        if (firebaseUserId != null) {
            println("✅ RevenueCat SDK configured with Firebase UID: $firebaseUserId")
        } else {
            println("✅ RevenueCat SDK configured (anonymous user)")
        }
        
        // Initialize SubscriptionRepository after SDK is configured
        (subscriptionRepository as? com.bhaskar.synctask.data.repository.SubscriptionRepositoryImpl)?.initialize()
    }
}
