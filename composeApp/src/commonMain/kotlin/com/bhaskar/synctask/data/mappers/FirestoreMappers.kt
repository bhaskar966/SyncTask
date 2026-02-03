package com.bhaskar.synctask.data.mappers

import com.bhaskar.synctask.domain.model.FirestoreRecurrence
import com.bhaskar.synctask.domain.model.FirestoreReminder
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.model.RecurrenceRule


fun Reminder.toFirestoreReminder(): FirestoreReminder {
    return FirestoreReminder(
        id = id,
        userId = userId,
        title = title,
        description = description,
        dueTime = dueTime,
        deadline = deadline,
        reminderTime = reminderTime,
        targetRemindCount = targetRemindCount,
        currentReminderCount = currentReminderCount,
        status = status.name,
        priority = priority.name,
        recurrence = recurrence?.toFirestoreRecurrence(),
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = true
    )
}

// FirestoreReminder → Reminder
fun FirestoreReminder.toReminder(): Reminder {
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
        recurrence = recurrence?.toRecurrenceRule(),
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = true
    )
}

fun RecurrenceRule.toFirestoreRecurrence(): FirestoreRecurrence {
    return when (this) {
        is RecurrenceRule.Daily -> FirestoreRecurrence(
            type = "DAILY",
            interval = interval,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        is RecurrenceRule.Weekly -> FirestoreRecurrence(
            type = "WEEKLY",
            interval = interval,
            daysOfWeek = daysOfWeek,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        is RecurrenceRule.Monthly -> FirestoreRecurrence(
            type = "MONTHLY",
            interval = interval,
            dayOfMonth = dayOfMonth,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        is RecurrenceRule.Yearly -> FirestoreRecurrence(
            type = "YEARLY",
            interval = interval,
            month = month,
            dayOfMonth = dayOfMonth,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        is RecurrenceRule.CustomDays -> FirestoreRecurrence(
            type = "CUSTOM_DAYS",
            daysOfWeek = daysOfWeek,
            interval = interval,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
    }
}

fun FirestoreRecurrence.toRecurrenceRule(): RecurrenceRule? {
    return when (type) {
        "DAILY" -> RecurrenceRule.Daily(
            interval = interval,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "WEEKLY" -> RecurrenceRule.Weekly(
            interval = interval,
            daysOfWeek = daysOfWeek ?: emptyList(),
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "MONTHLY" -> RecurrenceRule.Monthly(
            interval = interval,
            dayOfMonth = dayOfMonth ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "YEARLY" -> RecurrenceRule.Yearly(
            interval = interval,
            month = month ?: 1,
            dayOfMonth = dayOfMonth ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
            daysOfWeek = daysOfWeek ?: emptyList(),
            interval = interval,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        else -> null
    }
}



// Reminder → Firestore Map
fun Reminder.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "description" to description,
        "dueTime" to dueTime,
        "deadline" to deadline,
        "reminderTime" to reminderTime,
        "targetRemindCount" to targetRemindCount,
        "currentReminderCount" to currentReminderCount,
        "status" to status.name,
        "priority" to priority.name,
        "recurrence" to recurrence?.toFirestoreMap(),
        "snoozeUntil" to snoozeUntil,
        "createdAt" to createdAt,
        "lastModified" to lastModified,
        "completedAt" to completedAt,
        "deviceId" to deviceId,
        "isSynced" to true
    )
}

// Firestore Map → Reminder
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toReminder(): Reminder {
    return Reminder(
        id = this["id"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        title = this["title"] as? String ?: "",
        description = this["description"] as? String,
        dueTime = (this["dueTime"] as? Number)?.toLong() ?: 0L,
        deadline = (this["deadline"] as? Number)?.toLong(),
        reminderTime = (this["reminderTime"] as? Number)?.toLong(),
        targetRemindCount = (this["targetRemindCount"] as? Number)?.toInt(),
        currentReminderCount = (this["currentReminderCount"] as? Number)?.toInt(),
        status = ReminderStatus.valueOf(this["status"] as? String ?: "ACTIVE"),
        priority = Priority.valueOf(this["priority"] as? String ?: "MEDIUM"),
        recurrence = (this["recurrence"] as? Map<String, Any?>)?.toRecurrenceRule(),
        snoozeUntil = (this["snoozeUntil"] as? Number)?.toLong(),
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        lastModified = (this["lastModified"] as? Number)?.toLong() ?: 0L,
        completedAt = (this["completedAt"] as? Number)?.toLong(),
        deviceId = this["deviceId"] as? String,
        isSynced = true
    )
}

// RecurrenceRule → Firestore Map
fun RecurrenceRule.toFirestoreMap(): Map<String, Any?> {
    return when (this) {
        is RecurrenceRule.Daily -> mapOf(
            "type" to "DAILY",
            "interval" to interval,
            "afterCompletion" to afterCompletion,
            "endDate" to endDate,
            "occurrenceCount" to occurrenceCount
        )
        is RecurrenceRule.Weekly -> mapOf(
            "type" to "WEEKLY",
            "interval" to interval,
            "daysOfWeek" to daysOfWeek,
            "afterCompletion" to afterCompletion,
            "endDate" to endDate,
            "occurrenceCount" to occurrenceCount
        )
        is RecurrenceRule.Monthly -> mapOf(
            "type" to "MONTHLY",
            "interval" to interval,
            "dayOfMonth" to dayOfMonth,
            "afterCompletion" to afterCompletion,
            "endDate" to endDate,
            "occurrenceCount" to occurrenceCount
        )
        is RecurrenceRule.Yearly -> mapOf(
            "type" to "YEARLY",
            "interval" to interval,
            "month" to month,
            "dayOfMonth" to dayOfMonth,
            "afterCompletion" to afterCompletion,
            "endDate" to endDate,
            "occurrenceCount" to occurrenceCount
        )
        is RecurrenceRule.CustomDays -> mapOf(
            "type" to "CUSTOM_DAYS",
            "daysOfWeek" to daysOfWeek,
            "interval" to interval,
            "afterCompletion" to afterCompletion,
            "endDate" to endDate,
            "occurrenceCount" to occurrenceCount
        )
    }
}

// Firestore Map → RecurrenceRule
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toRecurrenceRule(): RecurrenceRule? {
    val type = this["type"] as? String ?: return null
    val afterCompletion = this["afterCompletion"] as? Boolean ?: false
    val endDate = (this["endDate"] as? Number)?.toLong()
    val occurrenceCount = (this["occurrenceCount"] as? Number)?.toInt()

    return when (type) {
        "DAILY" -> RecurrenceRule.Daily(
            interval = (this["interval"] as? Number)?.toInt() ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "WEEKLY" -> RecurrenceRule.Weekly(
            interval = (this["interval"] as? Number)?.toInt() ?: 1,
            daysOfWeek = (this["daysOfWeek"] as? List<Number>)
                ?.map { it.toInt() } ?: emptyList(),
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "MONTHLY" -> RecurrenceRule.Monthly(
            interval = (this["interval"] as? Number)?.toInt() ?: 1,
            dayOfMonth = (this["dayOfMonth"] as? Number)?.toInt() ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "YEARLY" -> RecurrenceRule.Yearly(
            interval = (this["interval"] as? Number)?.toInt() ?: 1,
            month = (this["month"] as? Number)?.toInt() ?: 1,
            dayOfMonth = (this["dayOfMonth"] as? Number)?.toInt() ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
            daysOfWeek = (this["daysOfWeek"] as? List<Number>)
                ?.map { it.toInt() } ?: emptyList(),
            interval = (this["interval"] as? Number)?.toInt() ?: 1,
            afterCompletion = afterCompletion,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
        else -> null
    }
}