// shared/src/iosMain/kotlin/com/bhaskar/synctask/di/KoinHelper.kt
package com.bhaskar.synctask

import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.di.initKoin
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.platform.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

fun doInitKoin() {
    initKoin()
}

// ✅ Add these helper functions to the same file
fun handleIOSNotification(reminderId: String, isPreReminder: Boolean) {
    val scheduler = KoinPlatform.getKoin().get<NotificationScheduler>()
    CoroutineScope(Dispatchers.IO).launch {
        scheduler.handleNotificationDelivered(reminderId, isPreReminder)
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