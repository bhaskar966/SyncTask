package com.bhaskar.synctask.presentation.recurrence

enum class Frequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class CustomRecurrenceState(
    val frequency: Frequency = Frequency.WEEKLY,
    val interval: Int = 1,
    val selectedDays: Set<Int> = emptySet(), // 1=Mon, 7=Sun
    val endDate: Long? = null
)

sealed class CustomRecurrenceEvent {
    data class OnFrequencyChanged(val frequency: Frequency) : CustomRecurrenceEvent()
    data class OnIntervalChanged(val interval: Int) : CustomRecurrenceEvent()
    data class OnDayToggled(val day: Int) : CustomRecurrenceEvent()
    data object OnApply : CustomRecurrenceEvent()
}
