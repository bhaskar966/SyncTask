package com.bhaskar.synctask.presentation.create.components

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class CreateReminderEvent {
    data class OnTitleChanged(val title: String) : CreateReminderEvent()
    data class OnDescriptionChanged(val description: String) : CreateReminderEvent()

    // Date & Time
    data class OnDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnTimeSelected(val time: LocalTime) : CreateReminderEvent()
    data class OnHasSpecificTimeToggled(val enabled: Boolean) : CreateReminderEvent()

    // Deadline
    data class OnDeadlineDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnDeadlineTimeSelected(val time: LocalTime) : CreateReminderEvent()
    data class OnDeadlineToggled(val enabled: Boolean) : CreateReminderEvent()

    // Reminder Time (Notification)
    data class OnReminderTimeModeChanged(val mode: ReminderTimeMode) : CreateReminderEvent()
    data class OnBeforeDueOffsetChanged(val offsetMs: Long) : CreateReminderEvent()
    data class OnCustomReminderDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnCustomReminderTimeSelected(val time: LocalTime) : CreateReminderEvent()

    // Priority & Recurrence
    data class OnPrioritySelected(val priority: Priority) : CreateReminderEvent()
    data class OnRecurrenceTypeSelected(val type: RecurrenceType) : CreateReminderEvent()
    data class OnRecurrenceSelected(val recurrence: RecurrenceRule?) : CreateReminderEvent()

    // ✅ MERGED: Custom Recurrence Events
    data object OnCustomRecurrenceToggled : CreateReminderEvent()
    data class OnRecurrenceFrequencyChanged(val frequency: RecurrenceFrequency) : CreateReminderEvent()
    data class OnRecurrenceIntervalChanged(val interval: Int) : CreateReminderEvent()
    data class OnRecurrenceDayToggled(val day: Int) : CreateReminderEvent()
    data class OnRecurrenceDayOfMonthChanged(val day: Int) : CreateReminderEvent()
    data class OnRecurrenceMonthChanged(val month: Int) : CreateReminderEvent()
    data class OnRecurrenceEndModeChanged(val mode: RecurrenceEndMode) : CreateReminderEvent()
    data class OnRecurrenceEndDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnRecurrenceOccurrenceCountChanged(val count: Int) : CreateReminderEvent()
    data class OnRecurrenceFromCompletionToggled(val enabled: Boolean) : CreateReminderEvent()
//    data object OnCustomRecurrenceApply : CreateReminderEvent()

    // Dialogs
    data object OnToggleDatePicker : CreateReminderEvent()
    data object OnToggleTimePicker : CreateReminderEvent()
    data object OnToggleDeadlineDatePicker : CreateReminderEvent()
    data object OnToggleDeadlineTimePicker : CreateReminderEvent()
    data object OnToggleCustomReminderDatePicker : CreateReminderEvent()
    data object OnToggleCustomReminderTimePicker : CreateReminderEvent()
    data object OnToggleRecurrencePicker : CreateReminderEvent()
    data object OnToggleRecurrenceEndDatePicker : CreateReminderEvent()  // ✅ NEW

    data object OnSave : CreateReminderEvent()
}