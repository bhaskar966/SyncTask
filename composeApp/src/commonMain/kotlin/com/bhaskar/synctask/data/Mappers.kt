package com.bhaskar.synctask.data

import com.bhaskar.synctask.db.ReminderEntity
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus

fun ReminderEntity.toDomain(): Reminder {
    val recurrence = if (recurrenceType != null) {
        val commonCount = recurrenceCount
        val commonFromCompletion = recurrenceFromCompletion ?: false

        when (recurrenceType) {
            "DAILY" -> RecurrenceRule.Daily(
                interval = recurrenceInterval ?: 1,
                endDate = recurrenceEndDate,
                occurrenceCount = commonCount,
                afterCompletion = commonFromCompletion
            )
            "WEEKLY" -> RecurrenceRule.Weekly(
                interval = recurrenceInterval ?: 1,
                daysOfWeek = recurrenceDaysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                endDate = recurrenceEndDate,
                occurrenceCount = commonCount,
                afterCompletion = commonFromCompletion
            )
            "MONTHLY" -> RecurrenceRule.Monthly(
                interval = recurrenceInterval ?: 1,
                dayOfMonth = recurrenceDayOfMonth ?: 1,
                endDate = recurrenceEndDate,
                occurrenceCount = commonCount,
                afterCompletion = commonFromCompletion
            )
            "YEARLY" -> RecurrenceRule.Yearly(
                interval = recurrenceInterval ?: 1,
                month = recurrenceMonth ?: 1,
                dayOfMonth = recurrenceDayOfMonth ?: 1,
                endDate = recurrenceEndDate,
                occurrenceCount = commonCount,
                afterCompletion = commonFromCompletion
            )
            "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
                interval = recurrenceInterval ?: 1,
                endDate = recurrenceEndDate,
                occurrenceCount = commonCount,
                afterCompletion = commonFromCompletion
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
        deadline = deadline,
        reminderTime = reminderTime,
        targetRemindCount = targetRemindCount,
        currentReminderCount = currentReminderCount,
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
    var recMonth: Int? = null
    var recEndDate: Long? = null
    var recCount: Int? = null
    var recFromCompletion: Boolean? = null

    if (recurrence != null) {
        recInterval = recurrence.interval
        recEndDate = recurrence.endDate
        recCount = recurrence.occurrenceCount
        recFromCompletion = recurrence.afterCompletion

        when (recurrence) {
            is RecurrenceRule.Daily -> {
                recType = "DAILY"
            }
            is RecurrenceRule.Weekly -> {
                recType = "WEEKLY"
                recDays = recurrence.daysOfWeek.joinToString(",")
            }
            is RecurrenceRule.Monthly -> {
                recType = "MONTHLY"
                recDayOfMonth = recurrence.dayOfMonth
            }
            is RecurrenceRule.Yearly -> {
                recType = "YEARLY"
                recMonth = recurrence.month
                recDayOfMonth = recurrence.dayOfMonth
            }
            is RecurrenceRule.CustomDays -> {
                recType = "CUSTOM_DAYS"
            }
        }
    }

    return ReminderEntity(
        id = id,
        userId = userId,
        title = title,
        description = description,
        dueTime = dueTime,
        deadline = deadline,
        reminderTime = reminderTime,
        status = status.name,
        priority = priority.name,
        recurrenceType = recType,
        recurrenceInterval = recInterval,
        recurrenceDaysOfWeek = recDays,
        recurrenceDayOfMonth = recDayOfMonth,
        recurrenceMonth = recMonth,
        recurrenceEndDate = recEndDate,
        recurrenceCount = recCount,
        recurrenceFromCompletion = recFromCompletion,
        targetRemindCount = targetRemindCount,
        currentReminderCount = currentReminderCount,
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced
    )
}
