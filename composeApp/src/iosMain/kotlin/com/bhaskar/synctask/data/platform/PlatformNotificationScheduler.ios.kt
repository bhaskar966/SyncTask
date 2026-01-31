package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.domain.NotificationCalculator
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.platform.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual class PlatformNotificationScheduler : NotificationScheduler, KoinComponent {
    private val repository: ReminderRepository by inject()
    private val notificationCalculator: NotificationCalculator by inject()

    actual override fun scheduleNext() {
        CoroutineScope(Dispatchers.Main).launch {
            println("🔵 iOS NotificationScheduler: scheduleNext() called")
            cancelAll()

            val nextNotification = notificationCalculator.getNextNotification() ?: run {
                println("🔴 No upcoming reminders found")
                return@launch
            }

            val content = UNMutableNotificationContent().apply {
                setTitle(nextNotification.title)
                setBody(nextNotification.body)
                setSound(UNNotificationSound.defaultSound)

                // Set category based on reminder type
                setCategoryIdentifier(
                    if(nextNotification.isPreReminder) "PRE_REMINDER" else "NORMAL_REMINDER"
                )

                // ✅ Convert boolean to string
                setUserInfo(
                    mapOf(
                        "reminderId" to nextNotification.reminderId,
                        "isPreReminder" to nextNotification.isPreReminder.toString()
                    )
                )
            }

            val timeInterval = nextNotification.triggerTime / 1000.0
            val date = NSDate.dateWithTimeIntervalSince1970(timeInterval)

            val calendar = NSCalendar.currentCalendar
            val components = calendar.components(
                NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                        NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
                fromDate = date
            )

            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = nextNotification.reminderId,
                content = content,
                trigger = trigger
            )

            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
                if (error != null) {
                    println("❌ iOS: Error scheduling notification: ${error.localizedDescription}")
                } else {
                    println("✅ iOS: Scheduled notification for $date")
                }
            }
        }
    }

    actual override fun cancelAll() {
        println("🔵 iOS: Cancelling all notifications")
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }

    actual override suspend fun handleNotificationDelivered(reminderId: String, isPreReminder: Boolean) {
        println("🔵 iOS: handleNotificationDelivered() called")
        (repository as? ReminderRepositoryImpl)?.handleNotificationDelivered(reminderId, isPreReminder)
        scheduleNext()
    }
}