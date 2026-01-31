package com.bhaskar.synctask.presentation.create.components

import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.utils.atStartOfDay
import com.bhaskar.synctask.presentation.utils.atTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class CreateReminderState(
    // Basic
    val title: String = "",
    val description: String = "",

    // Date & Time (Primary Reminder / Due Time)
    val selectedDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val selectedTime: LocalTime = LocalTime(9, 0),
    val hasSpecificTime: Boolean = true,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,

    // Deadline
    val isDeadlineEnabled: Boolean = false,
    val deadlineDate: LocalDate? = null,
    val deadlineTime: LocalTime? = null,
    val showDeadlineDatePicker: Boolean = false,
    val showDeadlineTimePicker: Boolean = false,

    // Reminder Time (when to notify)
    val reminderTimeMode: ReminderTimeMode = ReminderTimeMode.AT_DUE_TIME,
    val beforeDueOffset: Long = 3600000L, // 1 hour in ms
    val customReminderDate: LocalDate? = null,
    val customReminderTime: LocalTime? = null,
    val showCustomReminderDatePicker: Boolean = false,
    val showCustomReminderTimePicker: Boolean = false,

    // Priority
    val priority: Priority = Priority.MEDIUM,

    // Recurrence
    val recurrence: RecurrenceRule? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val showRecurrencePicker: Boolean = false,
    val recurrenceText: String = "Never",

    // ✅ MERGED: Custom Recurrence State
    val customRecurrenceMode: Boolean = false,  // Shows custom UI in modal
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.WEEKLY,
    val recurrenceInterval: Int = 1,
    val recurrenceSelectedDays: Set<Int> = emptySet(), // 1=Mon, 7=Sun
    val recurrenceDayOfMonth: Int = 1,  // For Monthly
    val recurrenceMonth: Int = 1,       // For Yearly
    val recurrenceEndMode: RecurrenceEndMode = RecurrenceEndMode.NEVER,
    val recurrenceEndDate: Long? = null,
    val recurrenceOccurrenceCount: Int? = null,
    val recurrenceFromCompletion: Boolean = false,
    val showRecurrenceEndDatePicker: Boolean = false,

    // UI State
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val validationError: String? = null,
) {
    fun getDueTime(): Long {
        return if (hasSpecificTime) {
            selectedDate.atTime(selectedTime)
        } else {
            selectedDate.atStartOfDay()
        }
    }

    fun getReminderTime(): Long? {
        return when (reminderTimeMode) {
            ReminderTimeMode.AT_DUE_TIME -> null
            ReminderTimeMode.BEFORE_DUE_TIME -> {
                getDueTime() - beforeDueOffset
            }
            ReminderTimeMode.CUSTOM_TIME -> {
                if (customReminderDate != null && customReminderTime != null) {
                    customReminderDate.atTime(customReminderTime)
                } else {
                    null
                }
            }
        }
    }

    fun getDeadline(): Long? {
        return if (isDeadlineEnabled && deadlineDate != null && deadlineTime != null) {
            deadlineDate.atTime(deadlineTime)
        } else {
            null
        }
    }
}

enum class ReminderTimeMode {
    AT_DUE_TIME, BEFORE_DUE_TIME, CUSTOM_TIME
}

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
}

// ✅ MERGED: Frequency enum
enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

// ✅ MERGED: End mode enum
enum class RecurrenceEndMode {
    NEVER, DATE, COUNT
}