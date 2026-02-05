package com.bhaskar.synctask.data

import com.bhaskar.synctask.db.ReminderEntity
import com.bhaskar.synctask.db.ReminderGroupEntity
import com.bhaskar.synctask.db.TagEntity
import com.bhaskar.synctask.domain.model.FirestoreGroup
import com.bhaskar.synctask.domain.model.FirestoreRecurrence
import com.bhaskar.synctask.domain.model.FirestoreReminder
import com.bhaskar.synctask.domain.model.FirestoreSubTask
import com.bhaskar.synctask.domain.model.FirestoreTag
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.model.SubTask
import com.bhaskar.synctask.domain.model.Tag
import kotlinx.serialization.json.Json


// REMINDER MAPPERS
fun ReminderEntity.toDomain(): Reminder {
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
        recurrence = mapRecurrence(),
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced,
        groupId = groupId,
        tagIds = tagIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        icon = icon,
        colorHex = colorHex,
        isPinned = isPinned,
        subtasks = subtasks?.let {
            try {
                Json.decodeFromString<List<SubTask>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    )
}

fun Reminder.toEntity(): ReminderEntity {
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
        recurrenceType = recurrence?.let { getRecurrenceType(it) },
        recurrenceInterval = recurrence?.interval,
        recurrenceDaysOfWeek = when (recurrence) {
            is RecurrenceRule.Weekly -> recurrence.daysOfWeek.joinToString(",")
            is RecurrenceRule.CustomDays -> recurrence.daysOfWeek.joinToString(",")
            else -> null
        },
        recurrenceDayOfMonth = when (recurrence) {
            is RecurrenceRule.Monthly -> recurrence.dayOfMonth
            is RecurrenceRule.Yearly -> recurrence.dayOfMonth
            else -> null
        },
        recurrenceMonth = when (recurrence) {
            is RecurrenceRule.Yearly -> recurrence.month
            else -> null
        },
        recurrenceEndDate = recurrence?.endDate,
        recurrenceCount = recurrence?.occurrenceCount,
        recurrenceFromCompletion = recurrence?.afterCompletion,
        targetRemindCount = targetRemindCount,
        currentReminderCount = currentReminderCount,
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced,
        groupId = groupId,
        tagIds = if (tagIds.isEmpty()) null else tagIds.joinToString(","),
        icon = icon,
        colorHex = colorHex,
        isPinned = isPinned,
        subtasks = if (subtasks.isEmpty()) null else Json.encodeToString(subtasks)
    )
}

fun Reminder.toFirestore(): FirestoreReminder {
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
        recurrence = recurrence?.toFirestore(),
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced,
        groupId = groupId,
        tagIds = tagIds,
        icon = icon,
        colorHex = colorHex,
        isPinned = isPinned,
        subtasks = subtasks.map { it.toFirestore() }
    )
}

fun FirestoreReminder.toDomain(): Reminder {
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
        recurrence = recurrence?.toDomain(),
        snoozeUntil = snoozeUntil,
        createdAt = createdAt,
        lastModified = lastModified,
        completedAt = completedAt,
        deviceId = deviceId,
        isSynced = isSynced,
        groupId = groupId,
        tagIds = tagIds,
        icon = icon,
        colorHex = colorHex,
        isPinned = isPinned,
        subtasks = subtasks.map { it.toDomain() }
    )
}

// SUBTASK MAPPERS
fun SubTask.toFirestore(): FirestoreSubTask {
    return FirestoreSubTask(
        id = id,
        title = title,
        isCompleted = isCompleted,
        order = order
    )
}

fun FirestoreSubTask.toDomain(): SubTask {
    return SubTask(
        id = id,
        title = title,
        isCompleted = isCompleted,
        order = order
    )
}

// GROUP MAPPERS
fun ReminderGroupEntity.toDomain(): ReminderGroup {
    return ReminderGroup(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        colorHex = colorHex,
        order = order,
        createdAt = createdAt,
        lastModified = lastModified,
        isSynced = isSynced
    )
}

fun ReminderGroup.toEntity(): ReminderGroupEntity {
    return ReminderGroupEntity(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        colorHex = colorHex,
        order = order,
        createdAt = createdAt,
        lastModified = lastModified,
        isSynced = isSynced
    )
}

fun ReminderGroup.toFirestore(): FirestoreGroup {
    return FirestoreGroup(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        colorHex = colorHex,
        order = order,
        createdAt = createdAt,
        lastModified = lastModified
    )
}

fun FirestoreGroup.toDomain(): ReminderGroup {
    return ReminderGroup(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        colorHex = colorHex,
        order = order,
        createdAt = createdAt,
        lastModified = lastModified,
        isSynced = true
    )
}


// TAG MAPPERS
fun TagEntity.toDomain(): Tag {
    return Tag(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt,
        isSynced = isSynced
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt,
        isSynced = isSynced
    )
}

fun Tag.toFirestore(): FirestoreTag {
    return FirestoreTag(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt
    )
}

fun FirestoreTag.toDomain(): Tag {
    return Tag(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt,
        isSynced = true
    )
}

// RECURRENCE MAPPERS
private fun ReminderEntity.mapRecurrence(): RecurrenceRule? {
    return when (recurrenceType) {
        "DAILY" -> RecurrenceRule.Daily(
            interval = recurrenceInterval ?: 1,
            endDate = recurrenceEndDate,
            occurrenceCount = recurrenceCount,
            afterCompletion = recurrenceFromCompletion ?: false
        )
        "WEEKLY" -> RecurrenceRule.Weekly(
            interval = recurrenceInterval ?: 1,
            daysOfWeek = recurrenceDaysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
            endDate = recurrenceEndDate,
            occurrenceCount = recurrenceCount,
            afterCompletion = recurrenceFromCompletion ?: false
        )
        "MONTHLY" -> RecurrenceRule.Monthly(
            interval = recurrenceInterval ?: 1,
            dayOfMonth = recurrenceDayOfMonth ?: 1,
            endDate = recurrenceEndDate,
            occurrenceCount = recurrenceCount,
            afterCompletion = recurrenceFromCompletion ?: false
        )
        "YEARLY" -> RecurrenceRule.Yearly(
            interval = recurrenceInterval ?: 1,
            month = recurrenceMonth ?: 1,
            dayOfMonth = recurrenceDayOfMonth ?: 1,
            endDate = recurrenceEndDate,
            occurrenceCount = recurrenceCount,
            afterCompletion = recurrenceFromCompletion ?: false
        )
        "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
            interval = recurrenceInterval ?: 1,
            daysOfWeek = recurrenceDaysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
            endDate = recurrenceEndDate,
            occurrenceCount = recurrenceCount,
            afterCompletion = recurrenceFromCompletion ?: false
        )
        else -> null
    }
}

private fun getRecurrenceType(rule: RecurrenceRule): String {
    return when (rule) {
        is RecurrenceRule.Daily -> "DAILY"
        is RecurrenceRule.Weekly -> "WEEKLY"
        is RecurrenceRule.Monthly -> "MONTHLY"
        is RecurrenceRule.Yearly -> "YEARLY"
        is RecurrenceRule.CustomDays -> "CUSTOM_DAYS"
    }
}

private fun RecurrenceRule.toFirestore(): FirestoreRecurrence {
    return when (this) {
        is RecurrenceRule.Daily -> FirestoreRecurrence(
            type = "DAILY",
            interval = interval,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        is RecurrenceRule.Weekly -> FirestoreRecurrence(
            type = "WEEKLY",
            interval = interval,
            daysOfWeek = daysOfWeek,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        is RecurrenceRule.Monthly -> FirestoreRecurrence(
            type = "MONTHLY",
            interval = interval,
            dayOfMonth = dayOfMonth,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        is RecurrenceRule.Yearly -> FirestoreRecurrence(
            type = "YEARLY",
            interval = interval,
            month = month,
            dayOfMonth = dayOfMonth,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        is RecurrenceRule.CustomDays -> FirestoreRecurrence(
            type = "CUSTOM_DAYS",
            interval = interval,
            daysOfWeek = daysOfWeek,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
    }
}

private fun FirestoreRecurrence.toDomain(): RecurrenceRule {
    return when (type) {
        "DAILY" -> RecurrenceRule.Daily(
            interval = interval,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        "WEEKLY" -> RecurrenceRule.Weekly(
            interval = interval,
            daysOfWeek = daysOfWeek ?: emptyList(),
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        "MONTHLY" -> RecurrenceRule.Monthly(
            interval = interval,
            dayOfMonth = dayOfMonth ?: 1,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        "YEARLY" -> RecurrenceRule.Yearly(
            interval = interval,
            month = month ?: 1,
            dayOfMonth = dayOfMonth ?: 1,
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        "CUSTOM_DAYS" -> RecurrenceRule.CustomDays(
            interval = interval,
            daysOfWeek = daysOfWeek ?: emptyList(),
            endDate = endDate,
            occurrenceCount = occurrenceCount,
            afterCompletion = afterCompletion
        )
        else -> RecurrenceRule.Daily(interval = 1)
    }
}
