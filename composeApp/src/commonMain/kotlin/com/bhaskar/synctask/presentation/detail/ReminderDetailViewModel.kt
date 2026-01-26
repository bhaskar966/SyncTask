package com.bhaskar.synctask.presentation.detail

import androidx.lifecycle.SavedStateHandle
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

class ReminderDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val reminderId: String = checkNotNull(savedStateHandle["reminderId"])
    
    private val _state = MutableStateFlow(ReminderDetailState())
    val state = _state.asStateFlow()

    init {
        loadReminder()
    }

    private fun loadReminder() {
        viewModelScope.launch {
            reminderRepository.getReminderById(reminderId).collect { reminder ->
                _state.update { it.copy(reminder = reminder, isLoading = false) }
            }
        }
    }

    fun onEvent(event: ReminderDetailEvent) {
        val currentReminder = _state.value.reminder ?: return
        
        when (event) {
            ReminderDetailEvent.OnToggleComplete -> {
                viewModelScope.launch {
                    if (currentReminder.status == ReminderStatus.COMPLETED) {
                        // Mark active (revert) - logic not strictly in repo interface yet, assume update
                        reminderRepository.updateReminder(currentReminder.copy(status = ReminderStatus.ACTIVE, completedAt = null))
                    } else {
                        reminderRepository.completeReminder(currentReminder.id)
                    }
                }
            }
            ReminderDetailEvent.OnDelete -> {
                viewModelScope.launch {
                    reminderRepository.deleteReminder(currentReminder.id)
                    // Navigate back handled by UI observing null reminder or side effect
                }
            }
            ReminderDetailEvent.OnEdit -> {
                // Navigate to edit
            }
        }
    }
}
