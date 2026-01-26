package com.bhaskar.synctask.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "ReminderEntity",
    indices = [
        Index("userId", "status"),
        Index("dueTime"),
        Index("isSynced")
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val dueTime: Long,
    val status: String,
    val priority: String,
    val recurrenceType: String?,
    val recurrenceInterval: Int?,
    val recurrenceDaysOfWeek: String?,
    val recurrenceDayOfMonth: Int?,
    val recurrenceEndDate: Long?,
    val snoozeUntil: Long?,
    val createdAt: Long,
    val lastModified: Long,
    val completedAt: Long?,
    val deviceId: String?,
    val isSynced: Boolean
)
