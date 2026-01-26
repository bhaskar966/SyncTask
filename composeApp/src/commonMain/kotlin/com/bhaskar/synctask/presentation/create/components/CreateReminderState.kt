package com.bhaskar.synctask.presentation.create.components

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CreateReminderState(
    val title: String = "",
    val description: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: RecurrenceRule? = null,
    val isSaving: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showRecurrencePicker: Boolean = false
)