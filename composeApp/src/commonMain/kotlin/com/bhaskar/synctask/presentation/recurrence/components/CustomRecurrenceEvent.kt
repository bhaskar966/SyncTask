package com.bhaskar.synctask.presentation.recurrence.components

sealed class CustomRecurrenceEvent {
    data class OnFrequencyChanged(val frequency: Frequency) : CustomRecurrenceEvent()
    data class OnIntervalChanged(val interval: Int) : CustomRecurrenceEvent()
    data class OnDayToggled(val day: Int) : CustomRecurrenceEvent()
    data object OnApply : CustomRecurrenceEvent()
    data object OnSetRecurrenceRule : CustomRecurrenceEvent()
}