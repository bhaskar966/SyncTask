package com.bhaskar.synctask.util

import com.bhaskar.synctask.platform.NotificationScheduler

class FakeNotificationScheduler : NotificationScheduler {

    var scheduleNextCallCount = 0
        private set

    var cancelAllCallCount = 0
        private set

    var lastHandledReminderId: String? = null
        private set

    override fun scheduleNext() {
        scheduleNextCallCount++
        println("📱 FakeScheduler: scheduleNext() called (#$scheduleNextCallCount)")
    }

    override fun cancelAll() {
        cancelAllCallCount++
        println("📱 FakeScheduler: cancelAll() called (#$cancelAllCallCount)")
    }

    override suspend fun handleNotificationDelivered(reminderId: String, isPreReminder: Boolean) {
        lastHandledReminderId = reminderId
        println("📱 FakeScheduler: Handled notification for $reminderId (preReminder=$isPreReminder)")
    }

    fun reset() {
        scheduleNextCallCount = 0
        cancelAllCallCount = 0
        lastHandledReminderId = null
    }
}