package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FirestoreReminder(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val dueTime: Long = 0L,
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val targetRemindCount: Int? = null,
    val currentReminderCount: Int? = null,
    val status: String = "ACTIVE",
    val priority: String = "MEDIUM",
    val recurrence: FirestoreRecurrence? = null,
    val snoozeUntil: Long? = null,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L,
    val completedAt: Long? = null,
    val deviceId: String? = null,
    val isSynced: Boolean = true,
    val groupId: String? = null,
    val tagIds: List<String> = emptyList(),
    val icon: String? = null,
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val subtasks: List<FirestoreSubTask> = emptyList()
)

@Serializable
data class FirestoreRecurrence(
    val type: String,
    val interval: Int = 1,
    val afterCompletion: Boolean = false,
    val endDate: Long? = null,
    val occurrenceCount: Int? = null,
    // Weekly/CustomDays
    val daysOfWeek: List<Int>? = null,
    // Monthly/Yearly
    val dayOfMonth: Int? = null,
    // Yearly
    val month: Int? = null
)

@Serializable
data class FirestoreSubTask(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val order: Int = 0
)