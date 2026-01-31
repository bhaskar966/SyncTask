package com.bhaskar.synctask.presentation.create

import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.RecurrenceUtils
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.domain.generateUUID
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.create.components.RecurrenceEndMode
import com.bhaskar.synctask.presentation.create.components.RecurrenceFrequency
import com.bhaskar.synctask.presentation.create.components.RecurrenceType
import com.bhaskar.synctask.presentation.list.ui_components.formatRecurrence
import com.bhaskar.synctask.presentation.utils.atStartOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlin.time.Instant

class CreateReminderViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReminderState())
    val state = _state.asStateFlow()

    private var editingReminderId: String? = null

    init {
        resetState()
    }

    fun resetState() {
        val now = Clock.System.now()
        val datetime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        editingReminderId = null
        _state.value = CreateReminderState(
            selectedDate = datetime.date,
            selectedTime = LocalTime(9, 0)
        )
    }

    fun onEvent(event: CreateReminderEvent) {
        when (event) {
            is CreateReminderEvent.OnTitleChanged -> {
                _state.update { it.copy(title = event.title, validationError = null) }
            }

            is CreateReminderEvent.OnDescriptionChanged -> {
                _state.update { it.copy(description = event.description) }
            }

            is CreateReminderEvent.OnDateSelected -> {
                _state.update {
                    val newDate = event.date
                    val oldDate = it.selectedDate
                    var rule = it.recurrence

                    if (rule != null) {
                        rule = when (rule) {
                            is RecurrenceRule.Weekly -> {
                                // If previously Weekly on the old day (single day), update to new day
                                if (rule.interval == 1 && rule.daysOfWeek.size == 1 && rule.daysOfWeek.first() == oldDate.dayOfWeek.isoDayNumber) {
                                    rule.copy(daysOfWeek = listOf(newDate.dayOfWeek.isoDayNumber))
                                } else rule
                            }

                            is RecurrenceRule.Monthly -> {
                                // If previously Monthly on the old day, update to new day
                                if (rule.interval == 1 && rule.dayOfMonth == oldDate.day) {
                                    rule.copy(dayOfMonth = newDate.day)
                                } else rule
                            }

                            is RecurrenceRule.Yearly -> {
                                // If previously Yearly on old date, update to new date
                                if (rule.interval == 1 && rule.month == oldDate.month.number && rule.dayOfMonth == oldDate.day) {
                                    rule.copy(
                                        month = newDate.month.number,
                                        dayOfMonth = newDate.day
                                    )
                                } else rule
                            }

                            else -> rule
                        }
                    }

                    it.copy(
                        selectedDate = newDate,
                        showDatePicker = false,
                        recurrence = rule,
                        recurrenceText = com.bhaskar.synctask.domain.RecurrenceUtils.formatRecurrenceRule(
                            rule
                        )
                    )
                }
            }

            is CreateReminderEvent.OnTimeSelected -> {
                _state.update { it.copy(selectedTime = event.time, showTimePicker = false) }
            }

            is CreateReminderEvent.OnHasSpecificTimeToggled -> {
                _state.update { it.copy(hasSpecificTime = event.enabled) }
            }

            is CreateReminderEvent.OnReminderTimeModeChanged -> {
                _state.update { it.copy(reminderTimeMode = event.mode) }
            }

            is CreateReminderEvent.OnBeforeDueOffsetChanged -> {
                _state.update { it.copy(beforeDueOffset = event.offsetMs) }
            }

            is CreateReminderEvent.OnCustomReminderDateSelected -> {
                _state.update {
                    it.copy(
                        customReminderDate = event.date,
                        showCustomReminderDatePicker = false
                    )
                }
            }

            is CreateReminderEvent.OnCustomReminderTimeSelected -> {
                _state.update {
                    it.copy(
                        customReminderTime = event.time,
                        showCustomReminderTimePicker = false
                    )
                }
            }

            is CreateReminderEvent.OnPrioritySelected -> {
                _state.update { it.copy(priority = event.priority) }
            }

            is CreateReminderEvent.OnRecurrenceTypeSelected -> {
                _state.update {
                    it.copy(
                        recurrenceType = event.type,
                        recurrence = if (event.type == RecurrenceType.NONE) null else it.recurrence
                    )
                }
            }

            is CreateReminderEvent.OnRecurrenceSelected -> {
                _state.update {
                    it.copy(
                        recurrence = event.recurrence,
                        recurrenceType = if (event.recurrence != null) RecurrenceType.CUSTOM else RecurrenceType.NONE,
                        showRecurrencePicker = false,
                        recurrenceText = com.bhaskar.synctask.domain.RecurrenceUtils.formatRecurrenceRule(
                            event.recurrence
                        )
                    )
                }

                populateCustomRecurrenceFromRule(event.recurrence)
            }
            // Deadline
            is CreateReminderEvent.OnDeadlineDateSelected -> {
                _state.update { it.copy(deadlineDate = event.date, showDeadlineDatePicker = false) }
            }

            is CreateReminderEvent.OnDeadlineTimeSelected -> {
                _state.update { it.copy(deadlineTime = event.time, showDeadlineTimePicker = false) }
            }

            is CreateReminderEvent.OnDeadlineToggled -> {
                _state.update {
                    it.copy(
                        isDeadlineEnabled = event.enabled,
                        deadlineDate = if (event.enabled && it.deadlineDate == null) Clock.System.todayIn(
                            TimeZone.currentSystemDefault()
                        ) else it.deadlineDate,
                        deadlineTime = if (event.enabled && it.deadlineTime == null) LocalTime(
                            0,
                            0
                        ) else it.deadlineTime
                    )
                }
            }
            // Dialog Toggles
            CreateReminderEvent.OnToggleDatePicker -> {
                _state.update { it.copy(showDatePicker = !it.showDatePicker) }
            }

            CreateReminderEvent.OnToggleTimePicker -> {
                _state.update { it.copy(showTimePicker = !it.showTimePicker) }
            }

            CreateReminderEvent.OnToggleDeadlineDatePicker -> {
                _state.update { it.copy(showDeadlineDatePicker = !it.showDeadlineDatePicker) }
            }

            CreateReminderEvent.OnToggleDeadlineTimePicker -> {
                _state.update { it.copy(showDeadlineTimePicker = !it.showDeadlineTimePicker) }
            }

            CreateReminderEvent.OnToggleCustomReminderDatePicker -> {
                _state.update { it.copy(showCustomReminderDatePicker = !it.showCustomReminderDatePicker) }
            }

            CreateReminderEvent.OnToggleCustomReminderTimePicker -> {
                _state.update { it.copy(showCustomReminderTimePicker = !it.showCustomReminderTimePicker) }
            }

            CreateReminderEvent.OnToggleRecurrencePicker -> {
                _state.update { it.copy(showRecurrencePicker = !it.showRecurrencePicker) }
            }

            CreateReminderEvent.OnSave -> {
                saveReminder()
            }

            is CreateReminderEvent.OnCustomRecurrenceToggled -> {
                _state.update {
                    it.copy(
                        customRecurrenceMode = !it.customRecurrenceMode,
                        // ✅ Initialize with current date info if toggling on for the first time
                        recurrenceDayOfMonth = if (!it.customRecurrenceMode && it.recurrence == null) {
                            it.selectedDate.day
                        } else {
                            it.recurrenceDayOfMonth
                        },
                        recurrenceMonth = if (!it.customRecurrenceMode && it.recurrence == null) {
                            it.selectedDate.month.number
                        } else {
                            it.recurrenceMonth
                        }
                    )
                }
            }

            is CreateReminderEvent.OnRecurrenceFrequencyChanged -> {
                _state.update { it.copy(recurrenceFrequency = event.frequency) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceIntervalChanged -> {
                val newInterval = event.interval.coerceAtLeast(1)
                _state.update { it.copy(recurrenceInterval = newInterval) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceDayToggled -> {
                val newDays = if (event.day in _state.value.recurrenceSelectedDays) {
                    _state.value.recurrenceSelectedDays - event.day
                } else {
                    _state.value.recurrenceSelectedDays + event.day
                }
                _state.update { it.copy(recurrenceSelectedDays = newDays) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceDayOfMonthChanged -> {
                _state.update { it.copy(recurrenceDayOfMonth = event.day.coerceIn(1, 31)) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceMonthChanged -> {
                _state.update { it.copy(recurrenceMonth = event.month.coerceIn(1, 12)) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceEndModeChanged -> {
                _state.update { it.copy(recurrenceEndMode = event.mode) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceEndDateSelected -> {
                _state.update {
                    it.copy(
                        recurrenceEndDate = event.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                        showRecurrenceEndDatePicker = false
                    )
                }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceOccurrenceCountChanged -> {
                val newCount = event.count.coerceAtLeast(1)
                _state.update { it.copy(recurrenceOccurrenceCount = newCount) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceFromCompletionToggled -> {
                _state.update { it.copy(recurrenceFromCompletion = event.enabled) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnToggleRecurrenceEndDatePicker -> {
                _state.update { it.copy(showRecurrenceEndDatePicker = !it.showRecurrenceEndDatePicker) }
            }

//            is CreateReminderEvent.OnCustomRecurrenceApply -> {
//                buildCustomRecurrenceRule()
//                _state.update { it.copy(customRecurrenceMode = false) }
//            }
        }
    }

    private fun saveReminder() {
        viewModelScope.launch(Dispatchers.IO) { // ✅ IO dispatcher
            val error = validateState(state.value)
            if (error != null) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(validationError = error) }
                }
                return@launch
            }

            val currentState = state.value

            withContext(Dispatchers.Main) {
                _state.update { it.copy(isSaving = true) }
            }

            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val reminder = Reminder(
                    id = editingReminderId ?: generateUUID(),
                    userId = "user_1",
                    title = currentState.title,
                    description = currentState.description.takeIf { it.isNotBlank() },
                    dueTime = currentState.getDueTime(),
                    reminderTime = currentState.getReminderTime(),
                    deadline = currentState.getDeadline(),
                    status = ReminderStatus.ACTIVE,
                    priority = currentState.priority,
                    recurrence = currentState.recurrence,
                    targetRemindCount = currentState.recurrence?.occurrenceCount,
                    currentReminderCount = if (currentState.recurrence != null) 1 else null,
                    createdAt = now,
                    lastModified = now,
                    isSynced = false
                )

                if (editingReminderId != null) {
                    reminderRepository.updateReminder(reminder)
                } else {
                    reminderRepository.createReminder(reminder)
                }

                println("✅ Reminder saved!")

                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false) }
                }

            } catch (e: Exception) {
                println("❌ Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false, validationError = e.message) }
                }
            }
        }
    }

    fun loadReminder(id: String) {
        viewModelScope.launch(Dispatchers.IO) { // ✅ IO dispatcher
            val reminder = reminderRepository.getReminderById(id).firstOrNull() ?: return@launch
            editingReminderId = reminder.id

            val dueDateTime = Instant.fromEpochMilliseconds(reminder.dueTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        isEditing = true,
                        title = reminder.title,
                        description = reminder.description ?: "",
                        priority = reminder.priority,
                        selectedDate = dueDateTime.date,
                        selectedTime = dueDateTime.time,
                        hasSpecificTime = true,
                        recurrence = reminder.recurrence,
                        recurrenceType = if (reminder.recurrence != null) RecurrenceType.CUSTOM else RecurrenceType.NONE,
                        recurrenceText = RecurrenceUtils.formatRecurrenceRule(reminder.recurrence),
                        isDeadlineEnabled = reminder.deadline != null,
                        deadlineDate = reminder.deadline?.let { dl ->
                            Instant.fromEpochMilliseconds(dl).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        },
                        deadlineTime = reminder.deadline?.let { dl ->
                            Instant.fromEpochMilliseconds(dl).toLocalDateTime(TimeZone.currentSystemDefault()).time
                        }
                    )
                }

                populateCustomRecurrenceFromRule(reminder.recurrence)
            }
        }
    }

    private fun validateState(state: CreateReminderState): String? {
        val now = Clock.System.now().toEpochMilliseconds()

        if (state.title.isBlank()) {
            return "Title is required"
        }

        val dueTime = state.getDueTime()
        if (dueTime < now) {
            return "Due time must be in the future"
        }

        val reminderTime = state.getReminderTime()
        if (reminderTime != null && reminderTime < now) {
            return "Reminder time must be in the future"
        }

        if (reminderTime != null && reminderTime > dueTime) {
            return "Reminder time must be before due time"
        }

        val deadline = state.getDeadline()
        if (deadline != null && deadline < dueTime) {
            return "Deadline must be after due time"
        }

        return null
    }

    private fun buildCustomRecurrenceRule() {
        val s = _state.value
        val endDate = if (s.recurrenceEndMode == RecurrenceEndMode.DATE) s.recurrenceEndDate else null
        val count = if (s.recurrenceEndMode == RecurrenceEndMode.COUNT) s.recurrenceOccurrenceCount else null

        val rule = when (s.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> RecurrenceRule.Daily(
                interval = s.recurrenceInterval,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.WEEKLY -> RecurrenceRule.Weekly(
                interval = s.recurrenceInterval,
                daysOfWeek = s.recurrenceSelectedDays.toList().sorted(),
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.MONTHLY -> RecurrenceRule.Monthly(
                interval = s.recurrenceInterval,
                dayOfMonth = s.recurrenceDayOfMonth,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.YEARLY -> RecurrenceRule.Yearly(
                interval = s.recurrenceInterval,
                month = s.recurrenceMonth,
                dayOfMonth = s.recurrenceDayOfMonth,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
        }

        _state.update {
            it.copy(
                recurrence = rule,
                recurrenceText = formatRecurrence(rule)
                // ✅ NO LONGER CLOSING customRecurrenceMode here
            )
        }
    }

    private fun populateCustomRecurrenceFromRule(rule: RecurrenceRule?) {
        if (rule == null) {
            return
        }

        when (rule) {
            is RecurrenceRule.Daily -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.DAILY,
                        recurrenceInterval = rule.interval,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Weekly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.WEEKLY,
                        recurrenceInterval = rule.interval,
                        recurrenceSelectedDays = rule.daysOfWeek.toSet(),
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Monthly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.MONTHLY,
                        recurrenceInterval = rule.interval,
                        recurrenceDayOfMonth = rule.dayOfMonth,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Yearly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.YEARLY,
                        recurrenceInterval = rule.interval,
                        recurrenceMonth = rule.month,
                        recurrenceDayOfMonth = rule.dayOfMonth,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.CustomDays -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.WEEKLY,  // Treat as weekly
                        recurrenceInterval = rule.interval,
                        recurrenceSelectedDays = rule.daysOfWeek.toSet(),
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
        }
    }

}
