package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val dueTime: Long,
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val targetRemindCount: Int? = null, 
    val currentReminderCount: Int? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: RecurrenceRule? = null,
    val snoozeUntil: Long? = null,
    val createdAt: Long,
    val lastModified: Long,
    val completedAt: Long? = null,
    val deviceId: String? = null,
    val isSynced: Boolean = false
)
