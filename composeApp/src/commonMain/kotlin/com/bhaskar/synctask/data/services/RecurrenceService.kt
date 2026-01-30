package com.bhaskar.synctask.data.services

import com.bhaskar.synctask.domain.RecurrenceUtils
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecurrenceService {

    fun calculateNextOccurrence(
        rule: RecurrenceRule,
        lastDueTime: Long,
        completedAt: Long?,
        currentCount: Int
    ): Long? {

        return RecurrenceUtils.calculateNextOccurrence(
            rule = rule,
            lastDueTime = lastDueTime,
            completedAt = completedAt,
            currentCount = currentCount
        )

    }

    @OptIn(ExperimentalUuidApi::class)
    fun createNextInstance(
        reminder: Reminder,
        triggeredAt: Long
    ): Reminder? {

        val rule = reminder.recurrence ?: return null

        val nextDueTime = calculateNextOccurrence(
            rule = rule,
            lastDueTime = reminder.dueTime,
            completedAt = reminder.completedAt,
            currentCount = reminder.currentReminderCount ?: 0
        ) ?: return null

        val currentCount = reminder.currentReminderCount ?: 1
        val targetCount = reminder.targetRemindCount ?: rule.occurrenceCount

        if(targetCount != null && currentCount >= targetCount) {
            println("❌ Reminder ${reminder.id} has no more occurrences")
            return null
        }

        val offset = if(reminder.reminderTime != null) {
            reminder.dueTime - reminder.reminderTime
        } else null

        val nextReminderTime = if(offset != null && offset > 0) {
            nextDueTime - offset
        } else null

        return reminder.copy(
            id = Uuid.random().toString(),
            dueTime = nextDueTime,
            reminderTime = nextReminderTime,
            currentReminderCount = currentCount + 1,
            targetRemindCount = targetCount,
            status = ReminderStatus.ACTIVE,
            recurrence = rule,
            createdAt = triggeredAt,
            lastModified = triggeredAt,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

    }

}