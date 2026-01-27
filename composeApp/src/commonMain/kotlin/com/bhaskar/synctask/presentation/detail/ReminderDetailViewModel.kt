package com.bhaskar.synctask.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.ReminderRepository
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailEvent
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ReminderDetailViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderDetailState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepository.getReminders().collect { reminders ->
                _state.update { it.copy(allReminders = reminders) }
            }
        }
    }

    fun onEvent(event: ReminderDetailEvent) {
        println("ReminderDetailViewModel: Received event: $event")
        val currentReminder = _state.value.reminder

        when (event) {
            is ReminderDetailEvent.OnToggleComplete -> {
                if (currentReminder == null) return
                viewModelScope.launch {
                    if (currentReminder.status == ReminderStatus.COMPLETED) {
                        reminderRepository.updateReminder(currentReminder.copy(status = ReminderStatus.ACTIVE, completedAt = null))
                    } else {
                        reminderRepository.completeReminder(currentReminder.id)
                    }
                }
            }
            is ReminderDetailEvent.OnDelete -> {
                viewModelScope.launch {
                    withContext(NonCancellable) {
                        reminderRepository.deleteReminder(event.reminderId)
                    }
                }
            }
            is ReminderDetailEvent.OnEdit -> {
                // Navigate to edit
            }
        }
    }
}