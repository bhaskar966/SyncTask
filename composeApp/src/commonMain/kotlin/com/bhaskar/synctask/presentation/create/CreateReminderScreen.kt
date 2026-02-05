package com.bhaskar.synctask.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.domain.model.SubTask
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import com.bhaskar.synctask.presentation.create.ui_components.ColorPickerDialog
import com.bhaskar.synctask.presentation.create.ui_components.CustomRecurrenceSection
import com.bhaskar.synctask.presentation.create.ui_components.DatePickerModal
import com.bhaskar.synctask.presentation.create.ui_components.DateTimePickerBox
import com.bhaskar.synctask.presentation.create.ui_components.GroupAutocompleteField
import com.bhaskar.synctask.presentation.create.ui_components.HorizontalDivider
import com.bhaskar.synctask.presentation.create.ui_components.IconPickerDialog
import com.bhaskar.synctask.presentation.create.ui_components.PriorityChip
import com.bhaskar.synctask.presentation.create.ui_components.RecurrenceModal
import com.bhaskar.synctask.presentation.create.ui_components.ReminderOptionRow
import com.bhaskar.synctask.presentation.create.ui_components.ScrollableRowOfChips
import com.bhaskar.synctask.presentation.create.ui_components.SectionHeader
import com.bhaskar.synctask.presentation.create.ui_components.TagsAutocompleteField
import com.bhaskar.synctask.presentation.create.ui_components.TimePickerModal
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.utils.parseHexColor
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    state: CreateReminderState,
    onEvent: (CreateReminderEvent) -> Unit,
    groups: List<ReminderGroup>,
    tags: List<Tag>,
    onNavigateBack: () -> Unit,
    onNavigateToCustomRecurrence: () -> Unit,
    navController: NavController,
) {
    var showRecurrenceModal by remember { mutableStateOf(false) }

    // Listen for custom recurrence result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val recurrenceResultParam by savedStateHandle?.getStateFlow("recurrence_rule", null)?.collectAsState() ?: mutableStateOf(null)

    LaunchedEffect(recurrenceResultParam) {
        recurrenceResultParam?.let { json ->
            try {
                val rule = Json.decodeFromString(RecurrenceRule.serializer(), json)
                onEvent(CreateReminderEvent.OnRecurrenceSelected(rule))
                savedStateHandle?.remove("recurrence_rule")
            } catch (e: Exception) {
                // Handle serialization error
            }
        }
    }

    // DatePicker Dialogs
    if (state.showDatePicker) {
        DatePickerModal(
            selectedDate = state.selectedDate,
            onDateSelected = { onEvent(CreateReminderEvent.OnDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDatePicker) }
        )
    }

    if (state.showDeadlineDatePicker) {
        DatePickerModal(
            selectedDate = state.deadlineDate
                ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { onEvent(CreateReminderEvent.OnDeadlineDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) }
        )
    }

    if (state.showCustomReminderDatePicker) {
        DatePickerModal(
            selectedDate = state.customReminderDate ?: state.selectedDate,
            onDateSelected = { onEvent(CreateReminderEvent.OnCustomReminderDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) }
        )
    }

    // TimePicker Dialogs
    if (state.showTimePicker) {
        TimePickerModal(
            selectedTime = state.selectedTime,
            onTimeSelected = { onEvent(CreateReminderEvent.OnTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleTimePicker) }
        )
    }

    if (state.showDeadlineTimePicker) {
        TimePickerModal(
            selectedTime = state.deadlineTime ?: LocalTime(0, 0),
            onTimeSelected = { onEvent(CreateReminderEvent.OnDeadlineTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) }
        )
    }

    if (state.showCustomReminderTimePicker) {
        TimePickerModal(
            selectedTime = state.customReminderTime ?: state.selectedTime,
            onTimeSelected = { onEvent(CreateReminderEvent.OnCustomReminderTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) }
        )
    }

    // Recurrence End Date Picker
    if (state.showRecurrenceEndDatePicker) {
        DatePickerModal(
            selectedDate = state.recurrenceEndDate?.let {
                Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
            } ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { onEvent(CreateReminderEvent.OnRecurrenceEndDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleRecurrenceEndDatePicker) }
        )
    }

    // Recurrence Modal
    if (showRecurrenceModal) {
        RecurrenceModal(
            startDate = state.selectedDate,
            onDismissRequest = { showRecurrenceModal = false },
            onRecurrenceSelected = { rule ->
                onEvent(CreateReminderEvent.OnRecurrenceSelected(rule))
                showRecurrenceModal = false
            },
            onCustomSelected = {
                showRecurrenceModal = false
                onEvent(CreateReminderEvent.OnCustomRecurrenceToggled)
            },
            currentRule = state.recurrence
        )
    }

    // ✅ NEW: Icon Picker Dialog
    if (state.showIconPicker) {
        IconPickerDialog(
            selectedIcon = state.icon,
            onIconSelected = {
                onEvent(CreateReminderEvent.OnIconSelected(it))
            },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleIconPicker) }
        )
    }

    // ✅ NEW: Color Picker Dialog
    if (state.showColorPicker) {
        ColorPickerDialog(
            selectedColor = state.colorHex,
            onColorSelected = {
                onEvent(CreateReminderEvent.OnColorSelected(it))
            },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleColorPicker) }
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
                    if (state.isEditing) "Edit Reminder" else "New Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(onClick = {
                    onEvent(CreateReminderEvent.OnSave)
                    if (state.validationError == null) {
                        if (state.title.isNotBlank()) onNavigateBack()
                    }
                }) {
                    Text(
                        if (state.isEditing) "Update" else "Save",
                        fontWeight = FontWeight.SemiBold,
                        color = Indigo500
                    )
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
                value = state.title,
                onValueChange = { onEvent(CreateReminderEvent.OnTitleChanged(it)) },
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
                isError = state.validationError != null,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.validationError != null) {
                Text(
                    text = state.validationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            SectionHeader("Appearance & Options")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Surface(
                        onClick = { onEvent(CreateReminderEvent.OnToggleIconPicker) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.icon ?: "📋",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }

                // Color Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Surface(
                        onClick = { onEvent(CreateReminderEvent.OnToggleColorPicker) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    if (state.colorHex != null) {
                                        parseHexColor(state.colorHex)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.colorHex == null) {
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Pin Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (state.isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Pin reminder",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Switch(
                    checked = state.isPinned,
                    onCheckedChange = { onEvent(CreateReminderEvent.OnPinToggled(it)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // WHEN SECTION
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
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Specific time", style = MaterialTheme.typography.bodyLarge)
                }

                Switch(
                    checked = state.hasSpecificTime,
                    onCheckedChange = { onEvent(CreateReminderEvent.OnHasSpecificTimeToggled(it)) }
                )
            }

            // Date & Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DateTimePickerBox(
                    text = formatDate(state.selectedDate),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onEvent(CreateReminderEvent.OnToggleDatePicker) },
                    modifier = Modifier.weight(1f)
                )

                if (state.hasSpecificTime) {
                    DateTimePickerBox(
                        text = formatTime(state.selectedTime),
                        icon = Icons.Default.AccessTime,
                        onClick = { onEvent(CreateReminderEvent.OnToggleTimePicker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // DEADLINE SECTION
            SectionHeader("Deadline")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Enable deadline", style = MaterialTheme.typography.bodyLarge)
                }

                Switch(
                    checked = state.isDeadlineEnabled,
                    onCheckedChange = { onEvent(CreateReminderEvent.OnDeadlineToggled(it)) }
                )
            }

            if (state.isDeadlineEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateTimePickerBox(
                        text = state.deadlineDate?.let { formatDate(it) } ?: "Set date",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) },
                        modifier = Modifier.weight(1f)
                    )

                    DateTimePickerBox(
                        text = state.deadlineTime?.let { formatTime(it) } ?: "Set time",
                        icon = Icons.Default.AccessTime,
                        onClick = { onEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // REMIND ME SECTION
            SectionHeader("Remind me")

            ReminderOptionRow(
                selected = state.reminderTimeMode == ReminderTimeMode.AT_DUE_TIME,
                text = "At time of event (${formatTime(state.selectedTime)})",
                onClick = { onEvent(CreateReminderEvent.OnReminderTimeModeChanged(ReminderTimeMode.AT_DUE_TIME)) }
            )

            ReminderOptionRow(
                selected = state.reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME,
                text = "Pre-reminder (Before event)",
                onClick = { onEvent(CreateReminderEvent.OnReminderTimeModeChanged(ReminderTimeMode.BEFORE_DUE_TIME)) }
            )

            if (state.reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME) {
                ScrollableRowOfChips(
                    options = listOf(
                        300000L to "5 min",
                        600000L to "10 min",
                        1800000L to "30 min",
                        3600000L to "1 hr",
                        7200000L to "2 hrs",
                        86400000L to "1 day"
                    ),
                    selectedFn = { it == state.beforeDueOffset },
                    onSelect = { onEvent(CreateReminderEvent.OnBeforeDueOffsetChanged(it)) },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            ReminderOptionRow(
                selected = state.reminderTimeMode == ReminderTimeMode.CUSTOM_TIME,
                text = "Custom date & time",
                onClick = { onEvent(CreateReminderEvent.OnReminderTimeModeChanged(ReminderTimeMode.CUSTOM_TIME)) }
            )

            if (state.reminderTimeMode == ReminderTimeMode.CUSTOM_TIME) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateTimePickerBox(
                        text = state.customReminderDate?.let { formatDate(it) } ?: "Set date",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) },
                        modifier = Modifier.weight(1f)
                    )

                    DateTimePickerBox(
                        text = state.customReminderTime?.let { formatTime(it) } ?: "Set time",
                        icon = Icons.Default.AccessTime,
                        onClick = { onEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) },
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
                        isSelected = state.priority == priority,
                        onSelect = { onEvent(CreateReminderEvent.OnPrioritySelected(priority)) }
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
                    text = state.recurrenceText,
                    style = MaterialTheme.typography.bodyLarge
                )

                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            // Custom Recurrence Section
            CustomRecurrenceSection(
                state = state,
                onEvent = onEvent
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ✅ ORGANIZATION SECTION (NEW)
            SectionHeader("Organization")

            // Group selector
            GroupAutocompleteField(
                selectedGroupId = state.selectedGroupId,
                availableGroups = groups,
                searchQuery = state.groupSearchQuery,
                onSearchQueryChanged = { onEvent(CreateReminderEvent.OnGroupSearchQueryChanged(it)) },
                onGroupSelected = { onEvent(CreateReminderEvent.OnGroupSelected(it)) },
                onCreateGroup = { onEvent(CreateReminderEvent.OnCreateGroup(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Tags selector
            TagsAutocompleteField(
                selectedTags = state.selectedTags,
                availableTags = tags,
                searchQuery = state.tagSearchQuery,
                onSearchQueryChanged = { onEvent(CreateReminderEvent.OnTagSearchQueryChanged(it)) },
                onTagToggled = { onEvent(CreateReminderEvent.OnTagToggled(it)) },
                onCreateTag = { onEvent(CreateReminderEvent.OnCreateTag(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ✅ SUBTASKS SECTION (NEW)
            SectionHeader("Subtasks")

            // Subtask input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.subtaskInput,
                    onValueChange = { onEvent(CreateReminderEvent.OnSubtaskInputChanged(it)) },
                    placeholder = { Text("Add subtask...") },
                    leadingIcon = { Icon(Icons.Default.Add, "Add") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { onEvent(CreateReminderEvent.OnAddSubtask) },
                    enabled = state.subtaskInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, "Add subtask")
                }
            }

            // Subtask list
            if (state.subtasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.subtasks.forEach { subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            onToggle = { onEvent(CreateReminderEvent.OnSubtaskToggled(subtask.id)) },
                            onDelete = { onEvent(CreateReminderEvent.OnSubtaskDeleted(subtask.id)) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // NOTES SECTION
            SectionHeader("Notes (optional)")

            TextField(
                value = state.description,
                onValueChange = { onEvent(CreateReminderEvent.OnDescriptionChanged(it)) },
                placeholder = { Text("Add notes...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 10,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ✅ SubtaskItem Component
@Composable
private fun SubtaskItem(
    subtask: SubTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = { onToggle() }
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textDecoration = if (subtask.isCompleted)
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                else null
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
