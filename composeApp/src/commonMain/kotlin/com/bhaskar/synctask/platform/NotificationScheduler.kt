package com.bhaskar.synctask.platform

interface NotificationScheduler {
    fun scheduleNext()
    fun cancelAll()
    suspend fun handleNotificationDelivered(reminderId: String, isPreReminder: Boolean)

    fun cancelNotification(reminderId: String)
}
