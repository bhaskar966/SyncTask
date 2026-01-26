package com.bhaskar.synctask.data

import com.bhaskar.synctask.db.ReminderEntity
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus

fun ReminderEntity.toDomain(): Reminder {
    val recurrence = if (recurrenceType != null) {
        when (recurrenceType) {
            "DAILY" -> RecurrenceRule.Daily(
                interval = recurrenceInterval ?: 1,
                endDate = recurrenceEndDate
            )
            "WEEKLY" -> RecurrenceRule.Weekly(
                interval = recurrenceInterval ?: 1,
                daysOfWeek = recurrenceDaysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                endDate = recurrenceEndDate
            )
            "MONTHLY" -> RecurrenceRule.Monthly(
                interval = recurrenceInterval ?: 1,
                dayOfMonth = recurrenceDayOfMonth ?: 1,
                endDate = recurrenceEndDate
            )
            "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
                interval = recurrenceInterval ?: 1,
                endDate = recurrenceEndDate
            )
            else -> null
        }
    } else null

    return Reminder(
        id = id,
        userId = userId,
        title = title,
        description = description,
        dueTime = dueTime,
        status = ReminderStatus.valueOf(status),
        priority = Priority.valueOf(priority),
        recurrence = recurrence,
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced
    )
}

fun Reminder.toEntity(): ReminderEntity {
    var recType: String? = null
    var recInterval: Int? = null
    var recDays: String? = null
    var recDayOfMonth: Int? = null
    var recEndDate: Long? = null

    when (recurrence) {
        is RecurrenceRule.Daily -> {
            recType = "DAILY"
            recInterval = recurrence.interval
            recEndDate = recurrence.endDate
        }
        is RecurrenceRule.Weekly -> {
            recType = "WEEKLY"
            recInterval = recurrence.interval
            recDays = recurrence.daysOfWeek.joinToString(",")
            recEndDate = recurrence.endDate
        }
        is RecurrenceRule.Monthly -> {
            recType = "MONTHLY"
            recInterval = recurrence.interval
            recDayOfMonth = recurrence.dayOfMonth
            recEndDate = recurrence.endDate
        }
        is RecurrenceRule.CustomDays -> {
            recType = "CUSTOM_DAYS"
            recInterval = recurrence.interval
            recEndDate = recurrence.endDate
        }
        null -> {}
    }

    return ReminderEntity(
        id = id,
        userId = userId,
        title = title,
        description = description,
        dueTime = dueTime,
        status = status.name,
        priority = priority.name,
        recurrenceType = recType,
        recurrenceInterval = recInterval,
        recurrenceDaysOfWeek = recDays,
        recurrenceDayOfMonth = recDayOfMonth,
        recurrenceEndDate = recEndDate,
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced
    )
}
