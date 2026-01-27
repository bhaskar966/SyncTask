package com.bhaskar.synctask.domain

import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CompleteReminderUseCase(
    private val repository: ReminderRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(id: String) {
        val reminder = repository.getReminderById(id).firstOrNull() ?: return
        val now = Clock.System.now().toEpochMilliseconds()

        // 1. Mark current as completed
        val completedReminder = reminder.copy(
            status = ReminderStatus.COMPLETED,
            completedAt = now,
            isSynced = false
        )
        repository.updateReminder(completedReminder)

        // 2. Check for recurrence
        if (reminder.recurrence != null) {
            val rule = reminder.recurrence

            // Calculate next Due Time (Event Time)
            val nextDueTime = RecurrenceUtils.calculateNextOccurrence(
                rule = rule,
                lastDueTime = reminder.dueTime,
                completedAt = now,
                currentCount = reminder.currentReminderCount ?: 0
            )

            if (nextDueTime != null) {
                val offset = if (reminder.reminderTime != null) {
                    reminder.dueTime - reminder.reminderTime
                } else null

                val nextReminderTime = if (offset != null) nextDueTime - offset else null

                // Initialize count tracking on first completion
                val currentCount = reminder.currentReminderCount ?: 1
                val targetCount = reminder.targetRemindCount ?: rule.occurrenceCount

                // Check if we've reached the limit
                if (targetCount != null && currentCount >= targetCount) {
                    return // Stop creating new instances
                }

                val nextCount = currentCount + 1

                // Create new reminder instance
                val nextReminder = reminder.copy(
                    id = Uuid.random().toString(),
                    dueTime = nextDueTime,
                    reminderTime = nextReminderTime,
                    targetRemindCount = targetCount,
                    currentReminderCount = nextCount,
                    deadline = null,
                    status = ReminderStatus.ACTIVE,
                    recurrence = rule,
                    createdAt = now,
                    lastModified = now,
                    completedAt = null,
                    snoozeUntil = null,
                    isSynced = false
                )
                repository.createReminder(nextReminder)
            }
        }
    }
}
