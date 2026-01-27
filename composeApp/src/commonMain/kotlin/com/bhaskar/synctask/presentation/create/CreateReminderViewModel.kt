package com.bhaskar.synctask.presentation.create

import CreateReminderEvent
import CreateReminderState
import ReminderTimeMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.CreateReminderUseCase
import com.bhaskar.synctask.domain.model.RecurrenceRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class CreateReminderViewModel(
    private val createReminderUseCase: CreateReminderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReminderState())
    val state = _state.asStateFlow()

    init {
        val now = Clock.System.now()
        val datetime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        _state.update { 
            it.copy(
                selectedDate = datetime.date,
                selectedTime = LocalTime(9, 0) // Default to 9:00 AM
            ) 
        }
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
                                 if (rule.interval == 1 && rule.dayOfMonth == oldDate.dayOfMonth) {
                                     rule.copy(dayOfMonth = newDate.dayOfMonth)
                                 } else rule
                             }
                             is RecurrenceRule.Yearly -> {
                                 // If previously Yearly on old date, update to new date
                                 if (rule.interval == 1 && rule.month == oldDate.monthNumber && rule.dayOfMonth == oldDate.dayOfMonth) {
                                     rule.copy(month = newDate.monthNumber, dayOfMonth = newDate.dayOfMonth)
                                 } else rule
                             }
                             else -> rule
                         }
                     }

                     it.copy(
                         selectedDate = newDate, 
                         showDatePicker = false,
                         recurrence = rule,
                         recurrenceText = formatRecurrenceRule(rule)
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
                _state.update { it.copy(customReminderDate = event.date, showCustomReminderDatePicker = false) }
            }
            is CreateReminderEvent.OnCustomReminderTimeSelected -> {
                _state.update { it.copy(customReminderTime = event.time, showCustomReminderTimePicker = false) }
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
                         recurrenceType = if(event.recurrence != null) RecurrenceType.CUSTOM else RecurrenceType.NONE,
                         showRecurrencePicker = false,
                         recurrenceText = formatRecurrenceRule(event.recurrence)
                     ) 
                 }
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
                        deadlineDate = if (event.enabled && it.deadlineDate == null) Clock.System.todayIn(TimeZone.currentSystemDefault()) else it.deadlineDate,
                        deadlineTime = if (event.enabled && it.deadlineTime == null) LocalTime(0, 0) else it.deadlineTime
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
        }
    }

    private fun saveReminder() {
        if (_state.value.title.isBlank()) {
            _state.update { it.copy(validationError = "Title is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val state = _state.value

            val eventDueTime = calculateDueTime(state.selectedDate, state.selectedTime, state.hasSpecificTime)
            
            val reminderNotificationTime = calculateNotificationTime(
                 dueTime = eventDueTime,
                 mode = state.reminderTimeMode,
                 offset = state.beforeDueOffset,
                 customDate = state.customReminderDate,
                 customTime = state.customReminderTime
            )

            // Calculate Deadline
            val finalDeadline = if (state.isDeadlineEnabled && state.deadlineDate != null) {
                val time = state.deadlineTime ?: LocalTime(0, 0)
                state.deadlineDate.atTime(time).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            } else {
                null
            }

            // Construct RecurrenceRule
            val finalRecurrence = if (state.recurrenceType != RecurrenceType.CUSTOM && state.recurrenceType != RecurrenceType.NONE) {
                 when (state.recurrenceType) {
                     RecurrenceType.DAILY -> RecurrenceRule.Daily(interval = 1)
                     RecurrenceType.WEEKLY -> RecurrenceRule.Weekly(interval = 1, daysOfWeek = listOf(state.selectedDate.dayOfWeek.isoDayNumber))
                     RecurrenceType.MONTHLY -> RecurrenceRule.Monthly(interval = 1, dayOfMonth = state.selectedDate.dayOfMonth)
                     else -> null
                 }
            } else {
                state.recurrence
            }

            createReminderUseCase(
                title = state.title.trim(),
                description = state.description.takeIf { it.isNotBlank() },
                dueTime = eventDueTime,
                reminderTime = reminderNotificationTime,
                deadline = finalDeadline,
                priority = state.priority,
                recurrence = finalRecurrence
            )
            
            _state.update { it.copy(isSaving = false) }
        }
    }
    
    private fun calculateDueTime(date: LocalDate, time: LocalTime, hasSpecificTime: Boolean): Long {
        val instant = if (hasSpecificTime) {
            date.atTime(time).toInstant(TimeZone.currentSystemDefault())
        } else {
             // If no specific time, set to start of day or end of day? 
             // Usually all-day events are stored as start of day for sorting.
            date.atTime(0, 0).toInstant(TimeZone.currentSystemDefault())
        }
        return instant.toEpochMilliseconds()
    }

    private fun calculateNotificationTime(
        dueTime: Long,
        mode: ReminderTimeMode,
        offset: Long,
        customDate: LocalDate?,
        customTime: LocalTime?
    ): Long {
        return when (mode) {
            ReminderTimeMode.AT_DUE_TIME -> dueTime
            ReminderTimeMode.BEFORE_DUE_TIME -> dueTime - offset
            ReminderTimeMode.CUSTOM_TIME -> {
                if (customDate != null && customTime != null) {
                    customDate.atTime(customTime).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                } else {
                    dueTime // Fallback
                }
            }
        }
    }
    private fun formatRecurrenceRule(rule: RecurrenceRule?): String {
        if (rule == null) return "Never"
        val base = when (rule) {
            is RecurrenceRule.Daily -> if (rule.interval == 1) "Daily" else "Every ${rule.interval} days"
            is RecurrenceRule.Weekly -> {
                val days = rule.daysOfWeek.joinToString(", ") { day ->
                    when (day) {
                        1 -> "Mon"
                        2 -> "Tue"
                        3 -> "Wed"
                        4 -> "Thu"
                        5 -> "Fri"
                        6 -> "Sat"
                        7 -> "Sun"
                        else -> ""
                    }
                }
                if (rule.interval == 1) "Weekly on $days" else "Every ${rule.interval} weeks on $days"
            }
            is RecurrenceRule.Monthly -> if (rule.interval == 1) "Monthly on day ${rule.dayOfMonth}" else "Every ${rule.interval} months on day ${rule.dayOfMonth}"
            is RecurrenceRule.Yearly -> if (rule.interval == 1) "Yearly on ${monthName(rule.month)} ${rule.dayOfMonth}" else "Every ${rule.interval} years on ${monthName(rule.month)} ${rule.dayOfMonth}"
            is RecurrenceRule.CustomDays -> "Every ${rule.interval} days"
        }
        val completion = if (rule.fromCompletion) " (after completion)" else ""
        return base + completion
    }

    private fun monthName(month: Int): String {
        return when (month) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> ""
        }
    }
}
