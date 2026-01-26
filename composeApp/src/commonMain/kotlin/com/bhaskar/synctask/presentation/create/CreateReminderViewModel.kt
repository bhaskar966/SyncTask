package com.bhaskar.synctask.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.CreateReminderUseCase
import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import kotlin.time.Clock

class CreateReminderViewModel(
    private val createReminderUseCase: CreateReminderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReminderState())
    val state = _state.asStateFlow()

    init {
        val now = Clock.System.now()
        val datetime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        _state.update { it.copy(date = datetime.date, time = datetime.time) }
    }

    fun onEvent(event: CreateReminderEvent) {
        when (event) {
            is CreateReminderEvent.OnTitleChanged -> {
                _state.update { it.copy(title = event.title) }
            }
            is CreateReminderEvent.OnDescriptionChanged -> {
                _state.update { it.copy(description = event.description) }
            }
            is CreateReminderEvent.OnDateSelected -> {
                _state.update { it.copy(date = event.date, showDatePicker = false) }
            }
            is CreateReminderEvent.OnTimeSelected -> {
                _state.update { it.copy(time = event.time, showTimePicker = false) }
            }
            is CreateReminderEvent.OnPrioritySelected -> {
                _state.update { it.copy(priority = event.priority) }
            }
            is CreateReminderEvent.OnRecurrenceSelected -> {
                _state.update { it.copy(recurrence = event.recurrence, showRecurrencePicker = false) }
            }
            CreateReminderEvent.OnToggleDatePicker -> {
                _state.update { it.copy(showDatePicker = !it.showDatePicker) }
            }
            CreateReminderEvent.OnToggleTimePicker -> {
                _state.update { it.copy(showTimePicker = !it.showTimePicker) }
            }
            CreateReminderEvent.OnToggleRecurrencePicker -> {
                _state.update { it.copy(showRecurrencePicker = !it.showRecurrencePicker) }
            }
            CreateReminderEvent.OnSave -> {
                saveReminder()
            }
        }
    }

    private fun saveReminder() {
        if (_state.value.title.isBlank()) return // Validate

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val state = _state.value
            val date = state.date ?: return@launch
            val time = state.time ?: return@launch // Or default

            val dateTime = LocalDateTime(date, time)
            // We need to convert back to kotlin.time.Instant
            // LocalDateTime.toInstant returns kotlinx.datetime.Instant.
            // We can get epoch millis from that.
            val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
            
            createReminderUseCase(
                title = state.title,
                description = state.description.ifBlank { null },
                dueTime = instant.toEpochMilliseconds(),
                priority = state.priority,
                recurrence = state.recurrence
            )
            // Navigate back (Handled by UI observing effect or callback)
            _state.update { it.copy(isSaving = false) }
        }
    }
}
