package com.bhaskar.synctask.presentation.create.components

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class CreateReminderEvent {
    data class OnTitleChanged(val title: String) : CreateReminderEvent()
    data class OnDescriptionChanged(val description: String) : CreateReminderEvent()

    // Visual & Organization Events
    data class OnIconSelected(val icon: String?) : CreateReminderEvent()
    data class OnColorSelected(val color: String?) : CreateReminderEvent()
    data class OnPinToggled(val pinned: Boolean) : CreateReminderEvent()

    // Toggle picker dialogs
    data object OnToggleIconPicker : CreateReminderEvent()
    data object OnToggleColorPicker : CreateReminderEvent()

    // Group autocomplete events
    data class OnGroupSearchQueryChanged(val query: String) : CreateReminderEvent()
    data class OnGroupSelected(val groupId: String?) : CreateReminderEvent()
    data class OnCreateGroup(val name: String) : CreateReminderEvent()

    // Tag autocomplete events
    data class OnTagSearchQueryChanged(val query: String) : CreateReminderEvent()
    data class OnTagToggled(val tagId: String) : CreateReminderEvent()
    data class OnCreateTag(val name: String) : CreateReminderEvent()

    // Subtasks
    data class OnSubtaskInputChanged(val input: String) : CreateReminderEvent()
    data object OnAddSubtask : CreateReminderEvent()
    data class OnSubtaskToggled(val subtaskId: String) : CreateReminderEvent()
    data class OnSubtaskDeleted(val subtaskId: String) : CreateReminderEvent()

    // Date & Time
    data class OnDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnTimeSelected(val time: LocalTime) : CreateReminderEvent()
    data class OnHasSpecificTimeToggled(val enabled: Boolean) : CreateReminderEvent()

    // Deadline
    data class OnDeadlineDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnDeadlineTimeSelected(val time: LocalTime) : CreateReminderEvent()
    data class OnDeadlineToggled(val enabled: Boolean) : CreateReminderEvent()

    // Reminder Time
    data class OnReminderTimeModeChanged(val mode: ReminderTimeMode) : CreateReminderEvent()
    data class OnBeforeDueOffsetChanged(val offsetMs: Long) : CreateReminderEvent()
    data class OnCustomReminderDateSelected(val date: LocalDate) : CreateReminderEvent()
    data class OnCustomReminderTimeSelected(val time: LocalTime) : CreateReminderEvent()

    // Priority & Recurrence
    data class OnPrioritySelected(val priority: Priority) : CreateReminderEvent()
    data class OnRecurrenceTypeSelected(val type: RecurrenceType) : CreateReminderEvent()
    data class OnRecurrenceSelected(val recurrence: RecurrenceRule?) : CreateReminderEvent()

    // Custom Recurrence Events
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

    // Dialogs
    data object OnToggleDatePicker : CreateReminderEvent()
    data object OnToggleTimePicker : CreateReminderEvent()
    data object OnToggleDeadlineDatePicker : CreateReminderEvent()
    data object OnToggleDeadlineTimePicker : CreateReminderEvent()
    data object OnToggleCustomReminderDatePicker : CreateReminderEvent()
    data object OnToggleCustomReminderTimePicker : CreateReminderEvent()
    data object OnToggleRecurrencePicker : CreateReminderEvent()
    data object OnToggleRecurrenceEndDatePicker : CreateReminderEvent()

    data object OnSave : CreateReminderEvent()
}
