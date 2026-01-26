package com.bhaskar.synctask.domain

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlin.time.Clock

class CreateReminderUseCase(private val repository: ReminderRepository) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        dueTime: Long,
        priority: Priority,
        recurrence: RecurrenceRule?
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = uuid4(), // Need a UUID generator
            userId = "user_1", // TODO: Get from Auth
            title = title,
            description = description,
            dueTime = dueTime,
            status = ReminderStatus.ACTIVE,
            priority = priority,
            recurrence = recurrence,
            createdAt = now,
            lastModified = now,
            isSynced = false
        )
        repository.createReminder(reminder)
    }
    
    // Simple UUID implementation for KMP (or use a library)
    private fun uuid4(): String = 
        generateUUID()
}


