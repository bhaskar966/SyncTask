package com.bhaskar.synctask.presentation.recurrence.components

import com.bhaskar.synctask.domain.model.RecurrenceRule

data class CustomRecurrenceState(
    val frequency: Frequency = Frequency.WEEKLY,
    val interval: Int = 1,
    val selectedDays: Set<Int> = emptySet(), // 1=Mon, 7=Sun
    val endDate: Long? = null,
    val occurrenceCount: Int? = null,
    val fromCompletion: Boolean = false,
    val endMode: EndMode = EndMode.NEVER,
    val recurrenceRule: RecurrenceRule? = null
)

enum class EndMode {
    NEVER, DATE, COUNT
}

fun CustomRecurrenceState.toRecurrenceRule(): RecurrenceRule {
    val finalEndDate = if (endMode == EndMode.DATE) endDate else null
    val finalCount = if (endMode == EndMode.COUNT) occurrenceCount else null

    return when (frequency) {
        Frequency.DAILY -> RecurrenceRule.Daily(
            interval = interval,
            endDate = finalEndDate,
            occurrenceCount = finalCount,
            fromCompletion = fromCompletion
        )
        Frequency.WEEKLY -> RecurrenceRule.Weekly(
            interval = interval,
            daysOfWeek = selectedDays.toList().sorted(),
            endDate = finalEndDate,
            occurrenceCount = finalCount,
            fromCompletion = fromCompletion
        )
        Frequency.MONTHLY -> RecurrenceRule.Monthly(
            interval = interval,
            dayOfMonth = 1, // Default to 1st
            endDate = finalEndDate,
            occurrenceCount = finalCount,
            fromCompletion = fromCompletion
        )
        Frequency.YEARLY -> RecurrenceRule.Yearly(
            interval = interval,
            month = 1, // Default to Jan
            dayOfMonth = 1, // Default to 1st
            endDate = finalEndDate,
            occurrenceCount = finalCount,
            fromCompletion = fromCompletion
        )
    }
}