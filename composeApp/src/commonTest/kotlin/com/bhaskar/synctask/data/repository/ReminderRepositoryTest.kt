@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bhaskar.synctask.data.repository

import app.cash.turbine.test
import com.bhaskar.synctask.data.services.RecurrenceService
import com.bhaskar.synctask.data.toEntity
import com.bhaskar.synctask.db.ReminderEntity
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.util.FakeFirestoreDataSource
import com.bhaskar.synctask.util.FakeNotificationScheduler
import com.bhaskar.synctask.util.FakeReminderDao
import com.bhaskar.synctask.util.TestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class ReminderRepositoryTest {

    private lateinit var repository: ReminderRepository
    private lateinit var fakeDao: FakeReminderDao
    private lateinit var fakeFirestore: FakeFirestoreDataSource
    private lateinit var fakeScheduler: FakeNotificationScheduler
    private lateinit var testDatabase: TestDatabase
    private lateinit var recurrenceService: RecurrenceService
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeDao = FakeReminderDao()
        fakeFirestore = FakeFirestoreDataSource()
        fakeScheduler = FakeNotificationScheduler()
        testDatabase = TestDatabase(fakeDao)
        recurrenceService = RecurrenceService()

        repository = ReminderRepositoryImpl(
            database = testDatabase,
            firestoreDataSource = fakeFirestore,
            notificationScheduler = fakeScheduler,
            recurrenceService = recurrenceService,
            scope = CoroutineScope(testDispatcher + SupervisorJob())
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        testDatabase.clearAllTables()
        fakeFirestore.clear()
        fakeScheduler.reset()
    }

    @Test
    fun `createReminder saves to database and schedules notification`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Test Reminder",
            description = "Test description",
            dueTime = now + 3600000,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        // Verify saved to database
        repository.getReminders().test {
            val reminders = awaitItem()
            assertEquals(1, reminders.size)
            assertEquals("Test Reminder", reminders[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify scheduler called
        assertEquals(1, fakeScheduler.scheduleNextCallCount)
    }

    @Test
    fun `updateReminder updates database and reschedules notification`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Original Title",
            description = null,
            dueTime = now + 3600000,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        fakeScheduler.reset()

        // Update
        val updated = reminder.copy(
            title = "Updated Title",
            priority = Priority.HIGH,
            lastModified = now + 1000
        )
        repository.updateReminder(updated)
        advanceUntilIdle()

        repository.getReminderById("test-1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Updated Title", result.title)
            assertEquals(Priority.HIGH, result.priority)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeScheduler.scheduleNextCallCount)
    }

    @Test
    fun `completeReminder marks as completed`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Task to Complete",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        repository.completeReminder("test-1")
        advanceUntilIdle()

        repository.getReminderById("test-1").test {
            val completed = awaitItem()
            assertNotNull(completed)
            assertEquals(ReminderStatus.COMPLETED, completed.status)
            assertNotNull(completed.completedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeReminder with afterCompletion true creates next instance`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Recurring After Completion",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(
                interval = 1,
                afterCompletion = true
            ),
            targetRemindCount = null,
            currentReminderCount = 1,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        repository.completeReminder("test-1")
        advanceUntilIdle()

        repository.getReminders().test {
            val reminders = awaitItem()

            // Original (completed)
            val original = reminders.find { it.id == "test-1" }
            assertNotNull(original)
            assertEquals(ReminderStatus.COMPLETED, original.status)

            // Next instance (active)
            val nextInstance = reminders.find { it.id != "test-1" }
            assertNotNull(nextInstance, "Should create next recurring instance")
            assertEquals(ReminderStatus.ACTIVE, nextInstance.status)
            assertEquals(2, nextInstance.currentReminderCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeReminder with afterCompletion false does not create next`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Recurring Time-Based",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(
                interval = 1,
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

        repository.createReminder(reminder)
        advanceUntilIdle()

        repository.completeReminder("test-1")
        advanceUntilIdle()

        repository.getReminders().test {
            val reminders = awaitItem()

            // Should only have 1 reminder (the completed one)
            assertEquals(1, reminders.size)
            assertEquals("test-1", reminders[0].id)
            assertEquals(ReminderStatus.COMPLETED, reminders[0].status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snoozeReminder updates status and snoozeUntil`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Task to Snooze",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        repository.snoozeReminder("test-1", snoozeMinutes = 60)
        advanceUntilIdle()

        repository.getReminderById("test-1").test {
            val snoozed = awaitItem()
            assertNotNull(snoozed)
            assertEquals(ReminderStatus.SNOOZED, snoozed.status)
            assertNotNull(snoozed.snoozeUntil)
            assertTrue(snoozed.snoozeUntil > now)

            // Verify it's 60 minutes later (with small tolerance)
            val expectedSnoozeTime = now + (60 * 60 * 1000)
            val difference = kotlin.math.abs(snoozed.snoozeUntil - expectedSnoozeTime)
            assertTrue(difference < 1000, "Snooze time should be ~60 minutes")

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(2, fakeScheduler.scheduleNextCallCount)
    }

    @Test
    fun `deleteReminder removes from database`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Task to Delete",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        repository.deleteReminder("test-1")
        advanceUntilIdle()

        repository.getReminders().test {
            val reminders = awaitItem()
            assertTrue(reminders.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Should also delete from Firestore
        assertFalse(fakeFirestore.contains("test-1"))
    }

    @Test
    fun `handleNotificationDelivered creates next instance for time-based recurring`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Time-Based Recurring",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(
                interval = 1,
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

        repository.createReminder(reminder)
        advanceUntilIdle()

        // Simulate alarm firing
        (repository as ReminderRepositoryImpl).handleNotificationDelivered("test-1", isPreReminder = false)
        advanceUntilIdle()

        repository.getReminders().test {
            val reminders = awaitItem()

            // Should have 2 reminders now
            assertEquals(2, reminders.size)

            // Original still ACTIVE
            val original = reminders.find { it.id == "test-1" }
            assertNotNull(original)
            assertEquals(ReminderStatus.ACTIVE, original.status)

            // New instance created
            val nextInstance = reminders.find { it.id != "test-1" }
            assertNotNull(nextInstance)
            assertEquals(2, nextInstance.currentReminderCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleNotificationDelivered does nothing for pre-reminder`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val reminder = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Task with Pre-Reminder",
            description = null,
            dueTime = now + 3600000,
            reminderTime = now,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = RecurrenceRule.Daily(interval = 1, afterCompletion = false),
            targetRemindCount = null,
            currentReminderCount = 1,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        repository.createReminder(reminder)
        advanceUntilIdle()

        // Simulate pre-reminder alarm firing
        (repository as ReminderRepositoryImpl).handleNotificationDelivered("test-1", isPreReminder = true)
        advanceUntilIdle()

        repository.getReminders().test {
            val reminders = awaitItem()

            // Should still only have 1 reminder
            assertEquals(1, reminders.size)
            assertEquals("test-1", reminders[0].id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sync uploads unsynced reminders to Firestore`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()

        val reminder1 = Reminder(
            id = "test-1",
            userId = "user_1",
            title = "Unsynced 1",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val reminder2 = reminder1.copy(
            id = "test-2",
            title = "Unsynced 2"
        )

        // Insert directly to DAO (bypasses repository's auto-sync)
        fakeDao.insertReminder(reminder1.toEntity()) // ← Use toEntity()
        fakeDao.insertReminder(reminder2.toEntity())
        advanceUntilIdle()

        // Verify Firestore is empty before sync
        assertFalse(fakeFirestore.contains("test-1"), "test-1 should not be in Firestore yet")
        assertFalse(fakeFirestore.contains("test-2"), "test-2 should not be in Firestore yet")

        // Now sync
        repository.sync()
        advanceUntilIdle()

        // Both should now be in Firestore
        assertTrue(fakeFirestore.contains("test-1"), "test-1 should be in Firestore after sync")
        assertTrue(fakeFirestore.contains("test-2"), "test-2 should be in Firestore after sync")
    }

    @Test
    fun `getActiveReminders only returns active reminders`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()

        val active = Reminder(
            id = "active-1",
            userId = "user_1",
            title = "Active",
            description = null,
            dueTime = now,
            reminderTime = null,
            deadline = null,
            status = ReminderStatus.ACTIVE,
            priority = Priority.MEDIUM,
            recurrence = null,
            targetRemindCount = null,
            currentReminderCount = null,
            createdAt = now,
            lastModified = now,
            completedAt = null,
            snoozeUntil = null,
            isSynced = false
        )

        val completed = active.copy(
            id = "completed-1",
            title = "Completed",
            status = ReminderStatus.COMPLETED,
            completedAt = now
        )

        val snoozed = active.copy(
            id = "snoozed-1",
            title = "Snoozed",
            status = ReminderStatus.SNOOZED,
            snoozeUntil = now + 3600000
        )

        repository.createReminder(active)
        repository.createReminder(completed)
        repository.createReminder(snoozed)
        advanceUntilIdle()

        repository.getActiveReminders().test {
            val reminders = awaitItem()

            // Only active reminder
            assertEquals(1, reminders.size)
            assertEquals("active-1", reminders[0].id)
            assertEquals(ReminderStatus.ACTIVE, reminders[0].status)

            cancelAndIgnoreRemainingEvents()
        }
    }
}