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
import kotlinx.coroutines.flow.firstOrNull
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig

class ReminderListViewModel(
    private val reminderRepository: ReminderRepository,
    private val subscriptionRepository: SubscriptionRepository
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

            is ReminderListEvent.OnSubtaskCheckedChange -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val reminder = reminderRepository.getReminderById(event.reminderId).firstOrNull()
                    if (reminder != null) {
                        val updatedSubtasks = reminder.subtasks.map { subtask ->
                            if (subtask.id == event.subtaskId) {
                                subtask.copy(isCompleted = event.isCompleted)
                            } else {
                                subtask
                            }
                        }
                        reminderRepository.updateReminder(reminder.copy(subtasks = updatedSubtasks))
                    }
                }
            }

            is ReminderListEvent.OnToggleSection -> {
                _state.update {
                    val sections = it.expandedSections.toMutableSet()
                    if (sections.contains(event.sectionId)) {
                        sections.remove(event.sectionId)
                    } else {
                        sections.add(event.sectionId)
                    }
                    it.copy(expandedSections = sections)
                }
            }

            is ReminderListEvent.OnTogglePin -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val reminder = event.reminder
                    if (reminder.isPinned) {
                        // Unpinning is always allowed
                        reminderRepository.updateReminder(reminder.copy(isPinned = false))
                    } else {
                        // Pinning requires check
                        val currentPinnedCount = reminderRepository.getPinnedReminderCount(reminder.userId)
                        val isPremium = subscriptionRepository.isPremiumSubscribed.value
                        
                        if (SubscriptionConfig.canPinReminder(currentPinnedCount, isPremium)) {
                            reminderRepository.updateReminder(reminder.copy(isPinned = true))
                        } else {
                            val message = if (isPremium) {
                                "You can only pin up to ${SubscriptionConfig.Limits.PREMIUM_MAX_PINNED_REMINDERS} reminders."
                            } else {
                                SubscriptionConfig.UpgradeMessages.PINNED
                            }
                            
                            _state.update {
                                it.copy(
                                    showPremiumDialog = true,
                                    premiumDialogMessage = message,
                                    isMaxLimitReached = isPremium
                                )
                            }
                        }
                    }
                }
            }

            ReminderListEvent.OnDismissPremiumDialog -> {
                _state.update { it.copy(showPremiumDialog = false, premiumDialogMessage = "", isMaxLimitReached = false) }
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
        val pinned = mutableListOf<Reminder>()
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
                    if (reminder.isPinned) {
                        pinned.add(reminder)
                    } else {
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
        }

        return currentState.copy(
            pinnedReminders = pinned.sortedBy { it.dueTime },
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
        val deadline = reminder.deadline

        if (deadline != null) {
            when {
                // Past deadline -> Overdue
                now > deadline -> overdue.add(reminder)

                // Active interval (Due <= Now <= Deadline) -> Today
                reminder.dueTime <= now -> today.add(reminder)

                // Future Due Date
                else -> {
                    val dueDate = Instant.fromEpochMilliseconds(reminder.dueTime)
                        .toLocalDateTime(timeZone).date
                    when (dueDate) {
                        todayDate -> today.add(reminder)
                        tomorrowDate -> tomorrow.add(reminder)
                        else -> later.add(reminder)
                    }
                }
            }
        } else {
            // No deadline
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
