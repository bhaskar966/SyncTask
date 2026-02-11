package com.bhaskar.synctask.presentation.utils

import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.Instant

fun formatRecurrence(recurrence: RecurrenceRule): String {
    return when (recurrence) {
        is RecurrenceRule.Daily -> {
            if (recurrence.interval == 1) "Daily"
            else "Every ${recurrence.interval} days"
        }

        is RecurrenceRule.Weekly -> {
            if (recurrence.interval == 1) "Weekly"
            else "Every ${recurrence.interval} weeks"
        }

        is RecurrenceRule.Monthly -> {
            if (recurrence.interval == 1) "Monthly"
            else "Every ${recurrence.interval} months"
        }

        is RecurrenceRule.Yearly -> {
            if (recurrence.interval == 1) "Yearly"
            else "Every ${recurrence.interval} years"
        }
        
        // Handling other cases or new ones if added later
        else -> "Custom"
    }
}

fun formatDateTime(timestamp: Long, deadline: Long? = null, is24HourFormat: Boolean = false): String {
    val timeZone = TimeZone.currentSystemDefault()
    val startInstant = Instant.fromEpochMilliseconds(timestamp)
    val startDateTime = startInstant.toLocalDateTime(timeZone)
    val now = Clock.System.now().toLocalDateTime(timeZone)

    fun formatTime(hour: Int, minute: Int): String {
        return if (is24HourFormat) {
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
            "$hour12:${minute.toString().padStart(2, '0')} $amPm"
        }
    }

    val startTimeStr = formatTime(startDateTime.hour, startDateTime.minute)

    val datePart = when (startDateTime.date) {
        now.date -> "Today"
        now.date.plus(DatePeriod(days = 1)) -> "Tomorrow"
        now.date.minus(DatePeriod(days = 1)) -> "Yesterday"
        else -> "${
            startDateTime.date.month.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } ${startDateTime.date.day}"
    }

    if (deadline != null) {
        val endInstant = Instant.fromEpochMilliseconds(deadline)
        val endDateTime = endInstant.toLocalDateTime(timeZone)
        val endTimeStr = formatTime(endDateTime.hour, endDateTime.minute)

        return "$datePart $startTimeStr - $endTimeStr"
    }

    return "$datePart $startTimeStr"
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""}"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        else -> "less than a minute"
    }
}
