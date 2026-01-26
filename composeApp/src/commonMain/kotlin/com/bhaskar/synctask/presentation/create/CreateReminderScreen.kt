package com.bhaskar.synctask.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import com.bhaskar.synctask.presentation.recurrence.RecurrenceModal
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.AlertDialog

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
                // Handle serialization error, maybe log it
            }
        }
    }

    // DatePicker Dialog - FIXED
    if (createReminderState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onCreateReminderEvent(CreateReminderEvent.OnDateSelected(date))
                    }
                    onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker) }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Dialog - FIXED
    if (createReminderState.showTimePicker) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { onCreateReminderEvent(CreateReminderEvent.OnToggleTimePicker) },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime(timePickerState.hour, timePickerState.minute)
                    onCreateReminderEvent(CreateReminderEvent.OnTimeSelected(time))
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleTimePicker) }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    if (showRecurrenceModal) {
        RecurrenceModal(
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
                    "New Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(50.dp))
            }
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        onCreateReminderEvent(CreateReminderEvent.OnSave)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Title
            TextField(
                value = createReminderState.title,
                onValueChange = { onCreateReminderEvent(CreateReminderEvent.OnTitleChanged(it)) },
                placeholder = {
                    Text(
                        "What needs to be done?",
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
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            TextField(
                value = createReminderState.description,
                onValueChange = {
                    onCreateReminderEvent(
                        CreateReminderEvent.OnDescriptionChanged(
                            it
                        )
                    )
                },
                placeholder = { Text("Add notes, URL, or details...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date & Time
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionButton(
                    icon = Icons.Default.CalendarMonth,
                    label = "Date",
                    value = createReminderState.date?.toString() ?: "Select",
                    onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleDatePicker) },
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    icon = Icons.Default.AccessTime,
                    label = "Time",
                    value = createReminderState.time?.let { formatTime(it) } ?: "Select",
                    onClick = { onCreateReminderEvent(CreateReminderEvent.OnToggleTimePicker) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recurrence
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionButton(
                    icon = Icons.Default.Repeat,
                    label = "Repeat",
                    value = if (createReminderState.recurrence == null) "Never" else "Repeating",
                    onClick = { showRecurrenceModal = true },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Priority
            Text(
                "Priority",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { priority ->
                    val isSelected = createReminderState.priority == priority
                    val color = if (isSelected) Indigo500 else Color.Transparent
                    val textColor =
                        if (isSelected) Indigo500 else MaterialTheme.colorScheme.onSurface
                    val borderColor =
                        if (isSelected) Indigo500.copy(alpha = 0.2f) else Color.Transparent
                    val containerColor =
                        if (isSelected) Indigo500.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                onCreateReminderEvent(
                                    CreateReminderEvent.OnPrioritySelected(
                                        priority
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format time in 12-hour format
private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val period = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $period"
}


@Composable
fun SelectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Indigo500, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        text = { content() }
    )
}
