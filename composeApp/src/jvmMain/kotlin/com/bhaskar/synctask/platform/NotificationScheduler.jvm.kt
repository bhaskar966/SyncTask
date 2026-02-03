package com.bhaskar.synctask.platform

actual class NotificationScheduler {
    actual fun scheduleNext() {
        println("NotificationScheduler.scheduleNext() not supported on JVM")
    }
    actual fun cancelAll() {
        println("NotificationScheduler.cancelAll() not supported on JVM")
    }
}
