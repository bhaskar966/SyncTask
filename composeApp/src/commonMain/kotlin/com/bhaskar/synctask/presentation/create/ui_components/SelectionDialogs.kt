package com.bhaskar.synctask.presentation.create.ui_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

@Composable
fun <T> SingleSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    labelProvider: (T) -> String
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismissRequest() }, // Manual scrim
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Slightly less than full width
                    .clickable(enabled = false) {} // Consume clicks inside
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    options.forEach { option ->
                        val isSelected = option == selectedOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(option) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = labelProvider(option),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (option != options.last()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrioritySelectionDialog(
    selectedPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    onDismissRequest: () -> Unit
) {
    SingleSelectionDialog(
        title = "Task Urgency", // User requested terminology or similar
        options = Priority.entries,
        selectedOption = selectedPriority,
        onOptionSelected = {
            onPrioritySelected(it)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        labelProvider = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
    )
}

@Composable
fun RemindMeSelectionDialog(
    selectedMode: ReminderTimeMode,
    onModeSelected: (ReminderTimeMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    SingleSelectionDialog(
        title = "Remind Me",
        options = ReminderTimeMode.entries,
        selectedOption = selectedMode,
        onOptionSelected = {
            onModeSelected(it)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        labelProvider = { 
            when(it) {
                ReminderTimeMode.AT_DUE_TIME -> "At time of event"
                ReminderTimeMode.BEFORE_DUE_TIME -> "Pre-reminder (Before event)"
                ReminderTimeMode.CUSTOM_TIME -> "Custom date & time"
            }
        }
    )
}

// Enum defined outside the function
enum class RecurrencePreset { NEVER, DAILY, WEEKLY, MONTHLY, WEEKDAYS, CUSTOM }

@Composable
fun RecurrenceSelectionDialog(
    currentRule: RecurrenceRule?,
    startDate: LocalDate,
    onRecurrenceSelected: (RecurrenceRule?) -> Unit,
    onCustomSelected: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val dayOfWeekName = startDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val dayOfMonth = startDate.day
    
    val preset = when (currentRule) {
        null -> RecurrencePreset.NEVER
        is RecurrenceRule.Daily if currentRule.interval == 1 -> RecurrencePreset.DAILY
        is RecurrenceRule.Weekly if currentRule.interval == 1 && currentRule.daysOfWeek == listOf(startDate.dayOfWeek.isoDayNumber) -> RecurrencePreset.WEEKLY
        is RecurrenceRule.Monthly if currentRule.interval == 1 && currentRule.dayOfMonth == dayOfMonth -> RecurrencePreset.MONTHLY
        is RecurrenceRule.Weekly if currentRule.interval == 1 && currentRule.daysOfWeek == listOf(1,2,3,4,5) -> RecurrencePreset.WEEKDAYS
        else -> RecurrencePreset.CUSTOM
    }

    SingleSelectionDialog(
        title = "Repeat",
        options = RecurrencePreset.entries,
        selectedOption = preset,
        onOptionSelected = { selected ->
            when (selected) {
                RecurrencePreset.NEVER -> onRecurrenceSelected(null)
                RecurrencePreset.DAILY -> onRecurrenceSelected(RecurrenceRule.Daily(1))
                RecurrencePreset.WEEKLY -> onRecurrenceSelected(RecurrenceRule.Weekly(1, listOf(startDate.dayOfWeek.isoDayNumber)))
                RecurrencePreset.MONTHLY -> onRecurrenceSelected(RecurrenceRule.Monthly(1, dayOfMonth))
                RecurrencePreset.WEEKDAYS -> onRecurrenceSelected(RecurrenceRule.Weekly(1, listOf(1,2,3,4,5)))
                RecurrencePreset.CUSTOM -> {
                    onCustomSelected()
                }
            }
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        labelProvider = { 
            when(it) {
                RecurrencePreset.NEVER -> "Never"
                RecurrencePreset.DAILY -> "Daily"
                RecurrencePreset.WEEKLY -> "Weekly on $dayOfWeekName"
                RecurrencePreset.MONTHLY -> "Monthly on day $dayOfMonth"
                RecurrencePreset.WEEKDAYS -> "Every weekday (Mon-Fri)"
                RecurrencePreset.CUSTOM -> "Custom..."
            }
        }
    )
}
