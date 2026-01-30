package com.bhaskar.synctask.domain

import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object RecurrenceUtils {

    fun calculateNextOccurrence(
        rule: RecurrenceRule,
        lastDueTime: Long,
        completedAt: Long? = null,
        currentCount: Int = 0 // How many times it has already occurred
    ): Long? {
        // ✅ Only check occurrence count at the start
        if (rule.occurrenceCount != null && currentCount >= rule.occurrenceCount!!) return null

        val timeZone = TimeZone.currentSystemDefault()

        val baseTimeInstant = if (rule.afterCompletion && completedAt != null) {
            // Use completion time for both date AND time
            Instant.fromEpochMilliseconds(completedAt)
        } else {
            // Use original due time for fixed schedule
            Instant.fromEpochMilliseconds(lastDueTime)
        }

        val baseDateTime = baseTimeInstant.toLocalDateTime(timeZone)

        // Calculate next occurrence
        val nextOccurrence = when (rule) {
            is RecurrenceRule.Daily -> {
                val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.DAY)
                next.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
            }
            is RecurrenceRule.Weekly -> {
                if (rule.daysOfWeek.isEmpty() || rule.afterCompletion) {
                    // Simple interval or dynamic from completion
                    val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.WEEK)
                    next.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
                } else {
                    // Fixed schedule with specific days (e.g., Mon, Wed, Fri)
                    val currentDayOfWeek = baseDateTime.dayOfWeek.ordinal + 1 // 1=Mon, 7=Sun
                    val sortedDays = rule.daysOfWeek.sorted()

                    // Find next valid day in current week
                    val nextDayThisWeek = sortedDays.firstOrNull { it > currentDayOfWeek }

                    if (nextDayThisWeek != null) {
                        // Same week
                        val daysToAdd = nextDayThisWeek - currentDayOfWeek
                        val nextDate = baseDateTime.date.plus(daysToAdd, DateTimeUnit.DAY)
                        nextDate.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
                    } else {
                        // Jump to next interval week, first valid day
                        val weeksToAdd = rule.interval
                        val nextWeekStart = baseDateTime.date.plus(weeksToAdd, DateTimeUnit.WEEK)

                        // Calculate days to add to reach first target day
                        val nextWeekDayOfWeek = nextWeekStart.dayOfWeek.ordinal + 1
                        val firstTargetDay = sortedDays.first()
                        val daysToAdd = (firstTargetDay - nextWeekDayOfWeek + 7) % 7

                        val finalDate = nextWeekStart.plus(daysToAdd, DateTimeUnit.DAY)
                        finalDate.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
                    }
                }
            }
            is RecurrenceRule.Monthly -> {
                if (rule.afterCompletion) {
                    // Use completion time
                    val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.MONTH)
                    next.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
                } else {
                    // Use original reminder time for fixed schedule
                    val targetDay = rule.dayOfMonth
                    val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.MONTH)

                    val yearMonth = YearMonth(next.year, next.month)
                    val lastDayOfMonth = yearMonth.lastDay.day
                    val actualDay = minOf(targetDay, lastDayOfMonth)

                    val finalDate = LocalDate(next.year, next.month, actualDay)
                    val originalTime = Instant.fromEpochMilliseconds(lastDueTime).toLocalDateTime(timeZone).time
                    finalDate.atTime(originalTime).toInstant(timeZone).toEpochMilliseconds()
                }
            }
            is RecurrenceRule.Yearly -> {
                val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.YEAR)
                next.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
            }
            is RecurrenceRule.CustomDays -> {
                val next = baseDateTime.date.plus(rule.interval, DateTimeUnit.DAY)
                next.atTime(baseDateTime.time).toInstant(timeZone).toEpochMilliseconds()
            }
        }

        // ✅ Check end date AFTER calculating next occurrence
        if (rule.endDate != null && nextOccurrence > rule.endDate!!) {
            return null
        }

        return nextOccurrence
    }
    fun formatRecurrenceRule(rule: RecurrenceRule?): String {
        if (rule == null) return "Never"
        val base = when (rule) {
            is RecurrenceRule.Daily -> if (rule.interval == 1) "Daily" else "Every ${rule.interval} days"
            is RecurrenceRule.Weekly -> {
                val days = rule.daysOfWeek.joinToString(", ") { day ->
                    when (day) {
                        1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"
                        5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> ""
                    }
                }
                if (rule.interval == 1) "Weekly on $days" else "Every ${rule.interval} weeks on $days"
            }
            is RecurrenceRule.Monthly -> if (rule.interval == 1) "Monthly on day ${rule.dayOfMonth}" else "Every ${rule.interval} months on day ${rule.dayOfMonth}"
            is RecurrenceRule.Yearly -> if (rule.interval == 1) "Yearly on ${monthName(rule.month)} ${rule.dayOfMonth}" else "Every ${rule.interval} years on ${monthName(rule.month)} ${rule.dayOfMonth}"
            is RecurrenceRule.CustomDays -> "Every ${rule.interval} days"
        }
        val completion = if (rule.afterCompletion) " (after completion)" else ""
        return base + completion
    }

    private fun monthName(month: Int): String {
        return when (month) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> ""
        }
    }
}
