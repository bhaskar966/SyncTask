package com.bhaskar.synctask.presentation.create

import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import com.bhaskar.synctask.presentation.recurrence.RecurrenceModal
import com.bhaskar.synctask.presentation.theme.Indigo500
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import com.bhaskar.synctask.presentation.create.components.ui_components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomRecurrence: () -> Unit,
    navController: NavController,
    createReminderState: CreateReminderState,
    onCreateReminderEvent: (CreateReminderEvent) -> Unit,
) {
    var showRecurrenceModal by remember { mutableStateOf(false) }

    // Listen for custom recurrence result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle

    val recurrenceResultParam by savedStateHandle?.getStateFlow<String?>("recurrence_rule", null)?.collectAsState() ?: mutableStateOf(null)

    LaunchedEffect(recurrenceResultParam) {
        recurrenceResultParam?.let { json ->
            try {
                val rule = Json.decodeFromString(RecurrenceRule.serializer(), json)
                onCreateReminderEvent(CreateReminderEvent.OnRecurrenceSelected(rule))
                savedStateHandle?.remove<String>("recurrence_rule")
            } catch (e: Exception) {
                // Handle serialization error
            }
        }
    }

    // DatePicker Dialogs
    if (createReminderState.showDatePicker) {
        DatePickerModal(
            selectedDate = createReminderState.selectedDate,
            onDateSelected = { onCreateReminderEvent(CreateReminderEvent.OnDateSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker) }
        )
    }
    if (createReminderState.showDeadlineDatePicker) {
        DatePickerModal(
            selectedDate = createReminderState.deadlineDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { onCreateReminderEvent(CreateReminderEvent.OnDeadlineDateSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) }
        )
    }
    if (createReminderState.showCustomReminderDatePicker) {
        DatePickerModal(
            selectedDate = createReminderState.customReminderDate ?: createReminderState.selectedDate,
            onDateSelected = { onCreateReminderEvent(CreateReminderEvent.OnCustomReminderDateSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) }
        )
    }

    // TimePicker Dialogs
    if (createReminderState.showTimePicker) {
        TimePickerModal(
            selectedTime = createReminderState.selectedTime,
            onTimeSelected = { onCreateReminderEvent(CreateReminderEvent.OnTimeSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleTimePicker) }
        )
    }
    if (createReminderState.showDeadlineTimePicker) {
        TimePickerModal(
            selectedTime = createReminderState.deadlineTime ?: LocalTime(0, 0),
            onTimeSelected = { onCreateReminderEvent(CreateReminderEvent.OnDeadlineTimeSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) }
        )
    }
    if (createReminderState.showCustomReminderTimePicker) {
        TimePickerModal(
            selectedTime = createReminderState.customReminderTime ?: createReminderState.selectedTime,
            onTimeSelected = { onCreateReminderEvent(CreateReminderEvent.OnCustomReminderTimeSelected(it)) },
            onDismiss = { onCreateReminderEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) }
        )
    }

    if (showRecurrenceModal) {
        RecurrenceModal(
            startDate = createReminderState.selectedDate,
            onDismissRequest = { showRecurrenceModal = false },
            onRecurrenceSelected = { rule ->
                onCreateReminderEvent(CreateReminderEvent.OnRecurrenceSelected(rule))
                showRecurrenceModal = false
            },
            onCustomSelected = {
                showRecurrenceModal = false
                onNavigateToCustomRecurrence()
            },
            currentRule = createReminderState.recurrence
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (createReminderState.isEditing) "Edit Reminder" else "New Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { 
                     onCreateReminderEvent(CreateReminderEvent.OnSave)
                     if (createReminderState.validationError == null) {
                         if (createReminderState.title.isNotBlank()) onNavigateBack()
                     }
                }) {
                    Text(if (createReminderState.isEditing) "Update" else "Save", fontWeight = FontWeight.SemiBold, color = Indigo500)
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Title
            TextField(
                value = createReminderState.title,
                onValueChange = { onCreateReminderEvent(CreateReminderEvent.OnTitleChanged(it)) },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.Gray)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                isError = createReminderState.validationError != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (createReminderState.validationError != null) {
                Text(
                    text = createReminderState.validationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // WHEN SECTION (Primary / Reminder Time)
            SectionHeader("When")

            // Specific Time Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Specific time", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = createReminderState.hasSpecificTime,
                    onCheckedChange = { onCreateReminderEvent(CreateReminderEvent.OnHasSpecificTimeToggled(it)) }
                )
            }
            
            // Date & Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date
                DateTimePickerBox(
                    text = formatDate(createReminderState.selectedDate),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker) },
                    modifier = Modifier.weight(1f)
                )
                
                // Time
                if (createReminderState.hasSpecificTime) {
                    DateTimePickerBox(
                        text = formatTime(createReminderState.selectedTime),
                        icon = Icons.Default.AccessTime,
                        onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleTimePicker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // DEADLINE SECTION (New)
            SectionHeader("Deadline")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                     Spacer(modifier = Modifier.width(16.dp))
                     Text("Enable deadline", style = MaterialTheme.typography.bodyLarge)
                 }
                 Switch(
                     checked = createReminderState.isDeadlineEnabled,
                     onCheckedChange = { onCreateReminderEvent(CreateReminderEvent.OnDeadlineToggled(it)) }
                 )
            }

            if (createReminderState.isDeadlineEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Deadline Date
                    DateTimePickerBox(
                        text = createReminderState.deadlineDate?.let { formatDate(it) } ?: "Set date",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) },
                        modifier = Modifier.weight(1f)
                    )
                    // Deadline Time
                    DateTimePickerBox(
                        text = createReminderState.deadlineTime?.let { formatTime(it) } ?: "Set time",
                        icon = Icons.Default.AccessTime,
                        onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // REMIND ME SECTION
            SectionHeader("Remind me")
            
            // At due time
            ReminderOptionRow(
                selected = createReminderState.reminderTimeMode == ReminderTimeMode.AT_DUE_TIME,
                text = "At time of event (${formatTime(createReminderState.selectedTime)})",
                onClick = { onCreateReminderEvent(CreateReminderEvent.OnReminderTimeModeChanged(
                    ReminderTimeMode.AT_DUE_TIME)) }
            )
            
            // Before due time (Pre-reminder)
            ReminderOptionRow(
                selected = createReminderState.reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME,
                text = "Pre-reminder (Before event)",
                onClick = { onCreateReminderEvent(CreateReminderEvent.OnReminderTimeModeChanged(
                    ReminderTimeMode.BEFORE_DUE_TIME)) }
            )
            
            if (createReminderState.reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME) {
                // Offset Dropdown/Selection
                ScrollableRowOfChips(
                    options = listOf(
                        300000L to "5 min",
                        600000L to "10 min",
                        1800000L to "30 min",
                        3600000L to "1 hr",
                        7200000L to "2 hrs",
                        86400000L to "1 day"
                    ),
                    selectedFn = { it == createReminderState.beforeDueOffset },
                    onSelect = { onCreateReminderEvent(CreateReminderEvent.OnBeforeDueOffsetChanged(it)) },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Custom time
            ReminderOptionRow(
                selected = createReminderState.reminderTimeMode == ReminderTimeMode.CUSTOM_TIME,
                text = "Custom date & time",
                onClick = { onCreateReminderEvent(CreateReminderEvent.OnReminderTimeModeChanged(
                    ReminderTimeMode.CUSTOM_TIME)) }
            )
            
            if (createReminderState.reminderTimeMode == ReminderTimeMode.CUSTOM_TIME) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Custom Date
                    DateTimePickerBox(
                        text = createReminderState.customReminderDate?.let { formatDate(it) } ?: "Set date",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) },
                        modifier = Modifier.weight(1f)
                    )
                    // Custom Time
                    DateTimePickerBox(
                        text = createReminderState.customReminderTime?.let { formatTime(it) } ?: "Set time",
                        icon = Icons.Default.AccessTime,
                        onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // PRIORITY SECTION
            SectionHeader("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Priority.entries.forEach { priority ->
                    PriorityChip(
                        priority = priority,
                        isSelected = createReminderState.priority == priority,
                        onSelect = { onCreateReminderEvent(CreateReminderEvent.OnPrioritySelected(priority)) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // REPEAT SECTION
            SectionHeader("Repeat")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRecurrenceModal = true }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = createReminderState.recurrenceText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // NOTES SECTION
            SectionHeader("Notes (optional)")
            TextField(
                value = createReminderState.description,
                onValueChange = { onCreateReminderEvent(CreateReminderEvent.OnDescriptionChanged(it)) },
                placeholder = { Text("Add notes...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 10,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Helpers
private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val period = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $period"
}

private fun formatDate(date: LocalDate): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        else -> "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"
    }
}
