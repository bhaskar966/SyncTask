package com.bhaskar.synctask.data.services

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class RecurrenceServiceTest {

    private lateinit var service: RecurrenceService

    @BeforeTest
    fun setup() {
        service = RecurrenceService()
    }

    @Test
    fun `createNextInstance creates reminder for next day`() {
        val now = Clock.System.now().toEpochMilliseconds()

        val reminder = Reminder(
            id = "test-1",
            userId = "user-1",
            title = "Daily Task",
            description = null,
            dueTime = now,
            reminderTime = now - 3600000, // 1 hour before
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(
                interval = 1,
                endDate = null,
                occurrenceCount = null,
                afterCompletion = false
            ),
            targetRemindCount = null,
            currentReminderCount = 1,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val nextReminder = service.createNextInstance(reminder, now)

        assertNotNull(nextReminder)
        assertNotEquals(reminder.id, nextReminder.id, "Should have new ID")
        assertEquals(2, nextReminder.currentReminderCount, "Should increment count")
        assertTrue(nextReminder.dueTime > reminder.dueTime, "Next due time should be later")
        assertEquals(ReminderStatus.ACTIVE, nextReminder.status)
        assertNull(nextReminder.completedAt)
        assertNull(nextReminder.snoozeUntil)
    }

    @Test
    fun `createNextInstance returns null when no recurrence`() {
        val now = Clock.System.now().toEpochMilliseconds()

        val reminder = Reminder(
            id = "test-1",
            userId = "user-1",
            title = "One-time Task",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null, // ← No recurrence
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val nextReminder = service.createNextInstance(reminder, now)

        assertNull(nextReminder, "Should not create instance when no recurrence")
    }

    @Test
    fun `createNextInstance respects occurrence count limit`() {
        val now = Clock.System.now().toEpochMilliseconds()

        val reminder = Reminder(
            id = "test-1",
            userId = "user-1",
            title = "Limited Task",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(
                interval = 1,
                endDate = null,
                occurrenceCount = 5,
                afterCompletion = false
            ),
            targetRemindCount = 5,
            currentReminderCount = 5, // Already at limit
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val nextReminder = service.createNextInstance(reminder, now)

        assertNull(nextReminder, "Should not create instance when limit reached")
    }

    @Test
    fun `createNextInstance preserves reminder time offset`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val offset = 1800000L // 30 minutes

        val reminder = Reminder(
            id = "test-1",
            userId = "user-1",
            title = "Task with Pre-reminder",
            description = null,
            dueTime = now,
            reminderTime = now - offset, // 30 min before due
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(interval = 1),
            targetRemindCount = null,
            currentReminderCount = 1,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val nextReminder = service.createNextInstance(reminder, now)

        assertNotNull(nextReminder)
        assertNotNull(nextReminder.reminderTime, "Should have reminder time")

        val expectedOffset = nextReminder.dueTime - nextReminder.reminderTime
        assertEquals(offset, expectedOffset, "Should preserve 30-minute offset")
    }

    @Test
    fun `createNextInstance sets isSynced to false`() {
        val now = Clock.System.now().toEpochMilliseconds()

        val reminder = Reminder(
            id = "test-1",
            userId = "user-1",
            title = "Synced Task",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(interval = 1),
            targetRemindCount = null,
            currentReminderCount = 1,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = true // Original was synced
        )

        val nextReminder = service.createNextInstance(reminder, now)

        assertNotNull(nextReminder)
        assertFalse(nextReminder.isSynced, "New instance should not be synced")
    }
}