package com.bhaskar.synctask.presentation.recurrence.components

import com.bhaskar.synctask.domain.model.RecurrenceRule

data class CustomRecurrenceState(
    val frequency: Frequency = Frequency.WEEKLY,
    val interval: Int = 1,
    val selectedDays: Set<Int> = emptySet(), // 1=Mon, 7=Sun
    val endDate: Long? = null,
    val recurrenceRule: RecurrenceRule? = null
)