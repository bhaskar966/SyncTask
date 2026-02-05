package com.bhaskar.synctask.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "ReminderEntity",
    indices = [
        Index("userId", "status"),
        Index("dueTime"),
        Index("isSynced"),
        Index("groupId"),
        Index("isPinned")
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val dueTime: Long,
    val deadline: Long?,
    val reminderTime: Long?,
    val status: String,
    val priority: String,
    val recurrenceType: String?,
    val recurrenceInterval: Int?,
    val recurrenceDaysOfWeek: String?,
    val recurrenceDayOfMonth: Int?,
    val recurrenceMonth: Int?,
    val recurrenceEndDate: Long?,
    val recurrenceCount: Int?,
    val recurrenceFromCompletion: Boolean?,
    val targetRemindCount: Int?,
    val currentReminderCount: Int?,
    val snoozeUntil: Long?,
    val createdAt: Long,
    val lastModified: Long,
    val completedAt: Long?,
    val deviceId: String?,
    val isSynced: Boolean,
    val groupId: String?,
    val tagIds: String?,
    val icon: String?,
    val colorHex: String?,
    val isPinned: Boolean,
    val subtasks: String?
)