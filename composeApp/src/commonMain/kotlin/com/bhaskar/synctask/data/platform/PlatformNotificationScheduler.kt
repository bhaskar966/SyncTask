package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.platform.NotificationScheduler

expect class PlatformNotificationScheduler: NotificationScheduler {
    override fun scheduleNext()
    override fun cancelAll()
    override suspend fun handleNotificationDelivered(
        reminderId: String,
        isPreReminder: Boolean
    )

    override fun cancelNotification(reminderId: String)
}