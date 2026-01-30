package com.bhaskar.synctask.util

import com.bhaskar.synctask.domain.RecurrenceUtils
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class RecurrenceUtilsTest {

    @Test
    fun `daily recurrence calculates next day correctly`() {
        val rule = RecurrenceRule.Daily(
            interval = 1,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        val lastDueTime = LocalDateTime(2026, 1, 29, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(
            rule = rule,
            lastDueTime = lastDueTime,
            completedAt = null,
            currentCount = 1
        )

        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(2026, nextDateTime.year)
        assertEquals(1, nextDateTime.month.number)
        assertEquals(30, nextDateTime.day) // Next day
        assertEquals(10, nextDateTime.hour)
        assertEquals(0, nextDateTime.minute)
    }

    @Test
    fun `daily recurrence with interval 2 skips one day`() {
        val rule = RecurrenceRule.Daily(
            interval = 2,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        val lastDueTime = LocalDateTime(2026, 1, 29, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)
        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(31, nextDateTime.day) // 2 days later
    }

    @Test
    fun `daily recurrence respects end date`() {
        // End date: Jan 30, 2026 at 11:59 PM
        val endDate = LocalDateTime(2026, 1, 30, 23, 59, 59)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val rule = RecurrenceRule.Daily(
            interval = 1,
            endDate = endDate,
            occurrenceCount = null,
            afterCompletion = false
        )

        // Last occurrence: Jan 30 at 10:00 AM
        val lastDueTime = LocalDateTime(2026, 1, 30, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)

        // Next would be Jan 31 at 10:00 AM, which exceeds end date
        assertNull(next, "Should return null when next occurrence exceeds end date")
    }


    @Test
    fun `weekly recurrence calculates next week correctly`() {
        val rule = RecurrenceRule.Weekly(
            interval = 1,
            daysOfWeek = listOf(4), // ← Thursday (not Wednesday!)
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        // Jan 29, 2026 is a THURSDAY
        val lastDueTime = LocalDateTime(2026, 1, 29, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)
        assertNotNull(next, "Should calculate next occurrence")

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        println("Last: Thu Jan 29, 2026 10:00 AM")
        println("Next: ${nextDateTime.dayOfWeek} ${nextDateTime.month} ${nextDateTime.day}, ${nextDateTime.year}")

        // Next Thursday = Feb 5, 2026
        assertEquals(2026, nextDateTime.year)
        assertEquals(2, nextDateTime.month.number) // February
        assertEquals(5, nextDateTime.day)
        assertEquals(DayOfWeek.THURSDAY, nextDateTime.dayOfWeek)
        assertEquals(10, nextDateTime.hour)
        assertEquals(0, nextDateTime.minute)
    }


    @Test
    fun `weekly recurrence with multiple days picks nearest`() {
        val rule = RecurrenceRule.Weekly(
            interval = 1,
            daysOfWeek = listOf(1, 3, 5), // Mon, Wed, Fri
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        // Jan 29, 2026 is THURSDAY
        val lastDueTime = LocalDateTime(2026, 1, 29, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)
        assertNotNull(next, "Should calculate next occurrence")

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        println("Last: Thu Jan 29, 2026")
        println("Next: ${nextDateTime.dayOfWeek} ${nextDateTime.month} ${nextDateTime.day}")

        // After Thu Jan 29, next in [Mon=1, Wed=3, Fri=5] should be Fri Jan 30
        assertEquals(2026, nextDateTime.year)
        assertEquals(1, nextDateTime.month.number) // Still January
        assertEquals(30, nextDateTime.day) // Friday Jan 30
        assertEquals(DayOfWeek.FRIDAY, nextDateTime.dayOfWeek)
    }

    @Test
    fun `monthly recurrence calculates next month correctly`() {
        val rule = RecurrenceRule.Monthly(
            interval = 1,
            dayOfMonth = 15,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        val lastDueTime = LocalDateTime(2026, 1, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)
        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(2026, nextDateTime.year)
        assertEquals(2, nextDateTime.month.number) // February
        assertEquals(15, nextDateTime.day)
    }

    @Test
    fun `yearly recurrence calculates next year correctly`() {
        val rule = RecurrenceRule.Yearly(
            interval = 1,
            month = 1, // January
            dayOfMonth = 29,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false
        )

        val lastDueTime = LocalDateTime(2026, 1, 29, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, 1)
        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(2027, nextDateTime.year) // Next year
        assertEquals(1, nextDateTime.month.number)
        assertEquals(29, nextDateTime.day)
    }

    @Test
    fun `afterCompletion true uses completion time not due time`() {
        val rule = RecurrenceRule.Daily(
            interval = 1,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = true // ← Key difference
        )

        val dueTime = LocalDateTime(2026, 1, 29, 8, 0) // Due at 8 AM
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val completedAt = LocalDateTime(2026, 1, 29, 10, 30) // Completed at 10:30 AM (2.5 hours late)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(
            rule = rule,
            lastDueTime = dueTime,
            completedAt = completedAt, // Uses this!
            currentCount = 1
        )

        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(30, nextDateTime.day) // Next day
        assertEquals(10, nextDateTime.hour) // Same time as completion
        assertEquals(30, nextDateTime.minute)
    }

    @Test
    fun `afterCompletion false uses due time`() {
        val rule = RecurrenceRule.Daily(
            interval = 1,
            endDate = null,
            occurrenceCount = null,
            afterCompletion = false // Time-based
        )

        val dueTime = LocalDateTime(2026, 1, 29, 8, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val completedAt = LocalDateTime(2026, 1, 29, 10, 30)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val next = RecurrenceUtils.calculateNextOccurrence(
            rule = rule,
            lastDueTime = dueTime,
            completedAt = completedAt, // Ignored
            currentCount = 1
        )

        assertNotNull(next)

        val nextDateTime = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        assertEquals(30, nextDateTime.day)
        assertEquals(8, nextDateTime.hour) // Original due time
        assertEquals(0, nextDateTime.minute)
    }

    @Test
    fun `recurrence respects occurrence count limit`() {
        val rule = RecurrenceRule.Daily(
            interval = 1,
            endDate = null,
            occurrenceCount = 5, // Max 5 occurrences
            afterCompletion = false
        )

        val lastDueTime = Clock.System.now().toEpochMilliseconds()

        // Count 5 already reached
        val next = RecurrenceUtils.calculateNextOccurrence(rule, lastDueTime, null, currentCount = 5)

        assertNull(next, "Should not create 6th occurrence when limit is 5")
    }
}