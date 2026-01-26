package com.bhaskar.synctask.presentation.create.components

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class CreateReminderEvent {
    data class OnTitleChanged(val title: String) : CreateReminderEvent()
    data class OnDescriptionChanged(val description: String) : CreateReminderEvent()
    data class OnDateSelected(val date: LocalDate?) : CreateReminderEvent() // Null to clear? Or just non-nullable in UI logic
    data class OnTimeSelected(val time: LocalTime?) : CreateReminderEvent()
    data class OnPrioritySelected(val priority: Priority) : CreateReminderEvent()
    data class OnRecurrenceSelected(val recurrence: RecurrenceRule?) : CreateReminderEvent()
    data object OnSave : CreateReminderEvent()
    data object OnToggleDatePicker : CreateReminderEvent()
    data object OnToggleTimePicker : CreateReminderEvent()
    data object OnToggleRecurrencePicker : CreateReminderEvent()
}