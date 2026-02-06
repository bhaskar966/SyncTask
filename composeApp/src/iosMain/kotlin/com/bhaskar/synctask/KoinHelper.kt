// shared/src/iosMain/kotlin/com/bhaskar/synctask/di/KoinHelper.kt
package com.bhaskar.synctask

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.di.initKoin
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.platform.NotificationScheduler
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

fun doInitKoin() {
    initKoin()
}

/**
 * Get the current Firebase user ID if logged in.
 * Returns null if not authenticated.
 */
fun getFirebaseUserId(): String? {
    return try {
        val authManager = KoinPlatform.getKoin().get<AuthManager>()
        authManager.currentUserId
    } catch (e: Exception) {
        println("⚠️ Could not get Firebase user ID: ${e.message}")
        null
    }
}

/**
 * Configure RevenueCat SDK for iOS.
 * This should be called from Swift after Koin is initialized.
 * @param apiKey The RevenueCat iOS API key
 * @param userId Optional Firebase user ID if already logged in
 */
fun configureRevenueCat(apiKey: String, userId: String?) {
    if (apiKey.isBlank()) {
        println("⚠️ RevenueCat API key is empty")
        return
    }
    
    try {
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(apiKey = apiKey) {
            appUserId = userId  // Will be null if not logged in (anonymous user)
        }
        
        if (userId != null) {
            println("✅ RevenueCat SDK configured for iOS with Firebase UID: $userId")
        } else {
            println("✅ RevenueCat SDK configured for iOS (anonymous user)")
        }
        
        // Initialize SubscriptionRepository after SDK is configured
        initializeSubscriptionRepository()
    } catch (e: Exception) {
        println("❌ Failed to configure RevenueCat: ${e.message}")
    }
}

/**
 * Initialize SubscriptionRepository after RevenueCat SDK is configured.
 * This sets up the delegate to receive subscription updates.
 */
private fun initializeSubscriptionRepository() {
    try {
        val repo = KoinPlatform.getKoin().get<SubscriptionRepository>()
        (repo as? com.bhaskar.synctask.data.repository.SubscriptionRepositoryImpl)?.initialize()
    } catch (e: Exception) {
        println("⚠️ Failed to initialize SubscriptionRepository: ${e.message}")
    }
}


fun handleIOSNotification(reminderId: String, isPreReminder: Boolean) {
    val scheduler = KoinPlatform.getKoin().get<NotificationScheduler>()
    CoroutineScope(Dispatchers.IO).launch {
        scheduler.handleNotificationDelivered(reminderId, isPreReminder)
    }
}

fun rescheduleNotifications() {
    val scheduler = KoinPlatform.getKoin().get<NotificationScheduler>()
    CoroutineScope(Dispatchers.IO).launch {
        scheduler.scheduleNext()
        println("✅ iOS: Rescheduled notifications")
    }
}


fun checkIOSMissedReminders() {
    val repository = KoinPlatform.getKoin().get<ReminderRepository>()
    CoroutineScope(Dispatchers.IO).launch {
        (repository as? ReminderRepositoryImpl)?.checkMissedReminders()
    }
}

fun processDeliveredNotifications(deliveredIds: List<String>) {
    println("🍎 Processing ${deliveredIds.size} delivered notifications from iOS")
    val scheduler = KoinPlatform.getKoin().get<NotificationScheduler>()

    CoroutineScope(Dispatchers.IO).launch {
        deliveredIds.forEach { reminderId ->
            println("   Processing delivered: $reminderId")
            // isPreReminder = false because main notifications are what we care about
            scheduler.handleNotificationDelivered(reminderId, isPreReminder = false)
        }
    }
}

fun handleIOSComplete(reminderId: String) {
    val repository = KoinPlatform.getKoin().get<ReminderRepository>()
    CoroutineScope(Dispatchers.IO).launch {
        repository.completeReminder(reminderId)
        println("✅ iOS: Completed reminder $reminderId")
    }
}

fun handleIOSDismiss(reminderId: String) {
    val repository = KoinPlatform.getKoin().get<ReminderRepository>()
    CoroutineScope(Dispatchers.IO).launch {
        repository.dismissReminder(reminderId)
        println("🚫 iOS: Dismissed reminder $reminderId")
    }
}

/**
 * Get SubscriptionRepository for iOS RevenueCat initialization.
 * This is called from Swift to set up the delegate after SDK configuration.
 */
fun getSubscriptionRepository(): SubscriptionRepository {
    return KoinPlatform.getKoin().get()
}