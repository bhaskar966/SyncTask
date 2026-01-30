package com.bhaskar.synctask.domain.model

/**
 * Represents the next notification to be scheduled
 * Used by NotificationScheduler to determine when and what to show
 */
data class NextNotification(
    val reminderId: String,
    val triggerTime: Long,
    val isPreReminder: Boolean,
    val title: String,
    val body: String
)