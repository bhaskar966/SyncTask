import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
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

    // UI State
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val validationError: String? = null,
)

enum class ReminderTimeMode {
    AT_DUE_TIME, BEFORE_DUE_TIME, CUSTOM_TIME
}

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
}