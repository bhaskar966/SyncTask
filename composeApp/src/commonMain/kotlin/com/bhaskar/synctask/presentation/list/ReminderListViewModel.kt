package com.bhaskar.synctask.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.ReminderRepository
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.list.components.ReminderListEvent
import com.bhaskar.synctask.presentation.list.components.ReminderListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlin.time.Clock

class ReminderListViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderListState())
    val state: StateFlow<ReminderListState> = _state
        .combine(reminderRepository.getReminders()) { state, reminders ->
            processReminders(state, reminders)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderListState(isLoading = true)
        )

    fun onEvent(event: ReminderListEvent) {
        when (event) {
            is ReminderListEvent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is ReminderListEvent.OnFilterChanged -> {
                _state.update { it.copy(selectedFilter = event.filter) }
            }
            is ReminderListEvent.OnCompleteReminder -> {
                viewModelScope.launch {
                    reminderRepository.completeReminder(event.id)
                }
            }
            is ReminderListEvent.OnDeleteReminder -> {
                viewModelScope.launch {
                    reminderRepository.deleteReminder(event.id)
                }
            }
            is ReminderListEvent.OnSnoozeReminder -> {
                // For now, default snooze 1 hour. In real app, show dialog.
                viewModelScope.launch {
                    val snoozeTime = Clock.System.now().toEpochMilliseconds() + 3600000 // +1h
                    reminderRepository.snoozeReminder(event.id, snoozeTime)
                }
            }
            ReminderListEvent.OnRefresh -> {
                viewModelScope.launch {
                    _state.update { it.copy(isSyncing = true) }
                    reminderRepository.sync()
                    _state.update { it.copy(isSyncing = false) }
                }
            }
            ReminderListEvent.OnDismissSyncStatus -> {
                // handled in UI usually
            }
        }
    }

    private fun processReminders(currentState: ReminderListState, allReminders: List<Reminder>): ReminderListState {
        val query = currentState.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allReminders
        } else {
            allReminders.filter {
                it.title.lowercase().contains(query) || (it.description?.lowercase()?.contains(query) == true)
            }
        }

        val now = Clock.System.now().toEpochMilliseconds()
        
        // Grouping
        val overdue = mutableListOf<Reminder>()
        val today = mutableListOf<Reminder>()
        val tomorrow = mutableListOf<Reminder>()
        val later = mutableListOf<Reminder>()
        val snoozed = mutableListOf<Reminder>()
        val completed = mutableListOf<Reminder>()

        filtered.forEach { reminder ->
            if (reminder.status == ReminderStatus.COMPLETED || reminder.status == ReminderStatus.DISMISSED) {
                completed.add(reminder)
            } else if (reminder.status == ReminderStatus.SNOOZED) {
                snoozed.add(reminder)
            } else {
                // Active
                if (reminder.dueTime < now) {
                    overdue.add(reminder)
                } else {
                    // Check if today/tomorrow
                    val timeZone = TimeZone.currentSystemDefault()
                    val dueDate = Instant.fromEpochMilliseconds(reminder.dueTime).toLocalDateTime(timeZone).date
                    val todayDate = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(timeZone).date
                    val tomorrowDate = todayDate.plus(DatePeriod(days = 1))

                    when (dueDate) {
                        todayDate -> today.add(reminder)
                        tomorrowDate -> tomorrow.add(reminder)
                        else -> later.add(reminder)
                    }
                }
            }
        }

        return currentState.copy(
            overdueReminders = overdue.sortedBy { it.dueTime },
            todayReminders = today.sortedBy { it.dueTime },
            tomorrowReminders = tomorrow.sortedBy { it.dueTime },
            laterReminders = later.sortedBy { it.dueTime },
            snoozedReminders = snoozed.sortedBy { it.snoozeUntil ?: it.dueTime },
            completedReminders = completed.sortedByDescending { it.completedAt ?: 0L },
            isLoading = false
        )
    }
}
