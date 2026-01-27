package com.bhaskar.synctask.presentation.recurrence

import androidx.lifecycle.ViewModel
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceEvent
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceState
import com.bhaskar.synctask.presentation.recurrence.components.EndMode
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
            is CustomRecurrenceEvent.OnFromCompletionToggled -> {
                _state.update { it.copy(fromCompletion = event.enabled) }
            }
            is CustomRecurrenceEvent.OnEndModeChanged -> {
                _state.update { it.copy(endMode = event.mode) }
            }
            is CustomRecurrenceEvent.OnEndDateChanged -> {
                _state.update { it.copy(endDate = event.dateMillis) }
            }
            is CustomRecurrenceEvent.OnOccurrenceCountChanged -> {
                _state.update { it.copy(occurrenceCount = event.count) }
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
        val endDate = if (s.endMode == EndMode.DATE) s.endDate else null
        val count = if (s.endMode == EndMode.COUNT) s.occurrenceCount else null
        
        val rule = when (s.frequency) {
            Frequency.DAILY -> RecurrenceRule.Daily(
                interval = s.interval,
                endDate = endDate,
                occurrenceCount = count,
                fromCompletion = s.fromCompletion
            )
            Frequency.WEEKLY -> RecurrenceRule.Weekly(
                interval = s.interval,
                daysOfWeek = s.selectedDays.toList().sorted(),
                endDate = endDate,
                occurrenceCount = count,
                fromCompletion = s.fromCompletion
            )
            Frequency.MONTHLY -> RecurrenceRule.Monthly(
                interval = s.interval,
                dayOfMonth = 1, // TODO: Should match start date or be selectable
                endDate = endDate,
                occurrenceCount = count,
                fromCompletion = s.fromCompletion
            )
            Frequency.YEARLY -> RecurrenceRule.Yearly(
                interval = s.interval,
                month = 1, // TODO: Should match start date
                dayOfMonth = 1,
                endDate = endDate,
                occurrenceCount = count,
                fromCompletion = s.fromCompletion
            )
        }
        _state.update {
            it.copy(
                recurrenceRule = rule
            )
        }
    }
}
