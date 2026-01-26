package com.bhaskar.synctask.presentation.recurrence

import androidx.lifecycle.ViewModel
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceEvent
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceState
import com.bhaskar.synctask.presentation.recurrence.components.Frequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CustomRecurrenceViewModel : ViewModel() {

    private val _state = MutableStateFlow(CustomRecurrenceState())
    val state = _state.asStateFlow()

    fun onEvent(event: CustomRecurrenceEvent) {
        when (event) {
            is CustomRecurrenceEvent.OnFrequencyChanged -> {
                _state.update { it.copy(frequency = event.frequency) }
            }
            is CustomRecurrenceEvent.OnIntervalChanged -> {
                val newInterval = event.interval.coerceAtLeast(1)
                _state.update { it.copy(interval = newInterval) }
            }
            is CustomRecurrenceEvent.OnDayToggled -> {
                _state.update { 
                    val currentDays = it.selectedDays.toMutableSet()
                    if (currentDays.contains(event.day)) {
                        currentDays.remove(event.day)
                    } else {
                        currentDays.add(event.day)
                    }
                    it.copy(selectedDays = currentDays)
                }
            }
            CustomRecurrenceEvent.OnApply -> {
                // Logic to build rule handled by UI callback calling getRule()?
                // Or expose a one-time event/flow
            }

            CustomRecurrenceEvent.OnSetRecurrenceRule -> setRecurrenceRule()
        }
    }

    fun setRecurrenceRule() {
        val s = _state.value
        val rule =  when (s.frequency) {
            Frequency.DAILY -> RecurrenceRule.Daily(s.interval, s.endDate)
            Frequency.WEEKLY -> RecurrenceRule.Weekly(s.interval, s.selectedDays.toList().sorted(), s.endDate)
            Frequency.MONTHLY -> RecurrenceRule.Monthly(s.interval, dayOfMonth = 1, endDate = s.endDate) // Simplified
            Frequency.YEARLY -> RecurrenceRule.Daily(s.interval * 365, s.endDate) // Fallback or implement Yearly
        }
        _state.update {
            it.copy(
                recurrenceRule = rule
            )
        }
    }
}
