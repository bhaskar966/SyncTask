package com.bhaskar.synctask.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.repository.ReminderRepository
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
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

class ReminderListViewModel(
    private val reminderRepository: ReminderRepository,
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            (reminderRepository as? ReminderRepositoryImpl)?.checkMissedReminders()
        }
    }

    fun onEvent(event: ReminderListEvent) {
        when (event) {
            is ReminderListEvent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }

            is ReminderListEvent.OnFilterChanged -> {
                _state.update { it.copy(selectedFilter = event.filter) }
            }

            is ReminderListEvent.OnCompleteReminder -> {
                viewModelScope.launch(Dispatchers.IO) {
                    reminderRepository.completeReminder(event.id)
                }
            }

            is ReminderListEvent.OnDeleteReminder -> {
                viewModelScope.launch(Dispatchers.IO) {
                    reminderRepository.deleteReminder(event.id)
                }
            }

            is ReminderListEvent.OnSnoozeReminder -> {
                viewModelScope.launch(Dispatchers.IO) {
                    reminderRepository.snoozeReminder(event.id, snoozeMinutes = event.minutes)
                }
            }

            ReminderListEvent.OnRefresh -> {
                viewModelScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isSyncing = true) }
                    }
                    reminderRepository.sync()
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isSyncing = false) }
                    }
                }
            }

            ReminderListEvent.OnDismissSyncStatus -> {
                // handled in UI
            }
        }
    }

    private fun processReminders(
        currentState: ReminderListState,
        allReminders: List<Reminder>
    ): ReminderListState {
        val query = currentState.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allReminders
        } else {
            allReminders.filter {
                it.title.lowercase().contains(query) ||
                        (it.description?.lowercase()?.contains(query) == true)
            }
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val timeZone = TimeZone.currentSystemDefault()
        val todayDate = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
        val tomorrowDate = todayDate.plus(DatePeriod(days = 1))
        val todayStart = todayDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val todayEnd = todayDate.atTime(23, 59, 59).toInstant(timeZone).toEpochMilliseconds()

        // Grouping lists
        val overdue = mutableListOf<Reminder>()
        val today = mutableListOf<Reminder>()
        val tomorrow = mutableListOf<Reminder>()
        val later = mutableListOf<Reminder>()
        val snoozed = mutableListOf<Reminder>()
        val missed = mutableListOf<Reminder>()
        val completed = mutableListOf<Reminder>()

        filtered.forEach { reminder ->
            when (reminder.status) {
                ReminderStatus.COMPLETED, ReminderStatus.DISMISSED -> {
                    completed.add(reminder)
                }
                ReminderStatus.MISSED -> {
                    missed.add(reminder)
                }
                ReminderStatus.SNOOZED -> {
                    snoozed.add(reminder)
                }
                ReminderStatus.ACTIVE -> {
                    categorizeActiveReminder(
                        reminder = reminder,
                        now = now,
                        todayDate = todayDate,
                        tomorrowDate = tomorrowDate,
                        todayStart = todayStart,
                        todayEnd = todayEnd,
                        timeZone = timeZone,
                        overdue = overdue,
                        today = today,
                        tomorrow = tomorrow,
                        later = later
                    )
                }
            }
        }

        return currentState.copy(
            overdueReminders = overdue.sortedBy { it.dueTime },
            todayReminders = today.sortedBy { it.dueTime },
            tomorrowReminders = tomorrow.sortedBy { it.dueTime },
            laterReminders = later.sortedBy { it.dueTime },
            snoozedReminders = snoozed.sortedBy { it.snoozeUntil ?: it.dueTime },
            missedReminders = missed.sortedByDescending { it.completedAt ?: 0L },
            completedReminders = completed.sortedByDescending { it.completedAt ?: 0L },
            isLoading = false
        )
    }

    private fun categorizeActiveReminder(
        reminder: Reminder,
        now: Long,
        todayDate: LocalDate,
        tomorrowDate: LocalDate,
        todayStart: Long,
        todayEnd: Long,
        timeZone: TimeZone,
        overdue: MutableList<Reminder>,
        today: MutableList<Reminder>,
        tomorrow: MutableList<Reminder>,
        later: MutableList<Reminder>
    ) {
        if (reminder.deadline != null && reminder.recurrence != null) {
            // Deadline reminder with recurrence
            when {
                // Currently in the active interval (dueTime to deadline)
                reminder.dueTime <= now && now <= reminder.deadline -> today.add(reminder)

                // Upcoming interval starts today
                reminder.dueTime in todayStart..todayEnd -> today.add(reminder)

                // Upcoming interval starts tomorrow
                reminder.dueTime > todayEnd -> {
                    val dueDate = Instant.fromEpochMilliseconds(reminder.dueTime)
                        .toLocalDateTime(timeZone).date
                    when (dueDate) {
                        tomorrowDate -> tomorrow.add(reminder)
                        else -> later.add(reminder)
                    }
                }

                // Interval ended (should be MISSED, but handle gracefully)
                now > reminder.deadline -> overdue.add(reminder)
            }
        } else {
            // Normal reminder (no deadline or no recurrence)
            if (reminder.dueTime < now) {
                overdue.add(reminder)
            } else {
                val dueDate = Instant.fromEpochMilliseconds(reminder.dueTime)
                    .toLocalDateTime(timeZone).date
                when (dueDate) {
                    todayDate -> today.add(reminder)
                    tomorrowDate -> tomorrow.add(reminder)
                    else -> later.add(reminder)
                }
            }
        }
    }
}

