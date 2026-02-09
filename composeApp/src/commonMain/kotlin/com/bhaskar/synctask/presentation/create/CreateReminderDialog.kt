package com.bhaskar.synctask.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.presentation.components.MaxLimitReachedDialog
import com.bhaskar.synctask.presentation.components.PremiumLimitDialog
import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import com.bhaskar.synctask.presentation.create.ui_components.*
import com.bhaskar.synctask.presentation.create.utils.ReminderUtils
import com.bhaskar.synctask.presentation.utils.parseHexColor
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderDialog(
    state: CreateReminderState,
    onEvent: (CreateReminderEvent) -> Unit,
    groups: List<ReminderGroup>,
    tags: List<Tag>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            CreateReminderContent(
                state = state,
                onEvent = onEvent,
                groups = groups,
                tags = tags,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun CreateReminderContent(
    state: CreateReminderState,
    onEvent: (CreateReminderEvent) -> Unit,
    groups: List<ReminderGroup>,
    tags: List<Tag>,
    onDismiss: () -> Unit
) {
    var showRecurrenceModal by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showRemindMeDialog by remember { mutableStateOf(false) }
    var isSubtaskInputVisible by remember { mutableStateOf(false) }

    // --- DIALOGS ---
    if (state.showDatePicker) {
        DatePickerModal(
            selectedDate = state.selectedDate,
            onDateSelected = { onEvent(CreateReminderEvent.OnDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDatePicker) }
        )
    }
    if (state.showTimePicker) {
        TimePickerModal(
            selectedTime = state.selectedTime,
            onTimeSelected = { onEvent(CreateReminderEvent.OnTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleTimePicker) }
        )
    }
    if (state.showDeadlineDatePicker) {
        DatePickerModal(
            selectedDate = state.deadlineDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { onEvent(CreateReminderEvent.OnDeadlineDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) }
        )
    }
    if (state.showDeadlineTimePicker) {
        TimePickerModal(
            selectedTime = state.deadlineTime ?: LocalTime(0, 0),
            onTimeSelected = { onEvent(CreateReminderEvent.OnDeadlineTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) }
        )
    }
    if (state.showCustomReminderDatePicker) {
        DatePickerModal(
            selectedDate = state.customReminderDate ?: state.selectedDate,
            onDateSelected = { onEvent(CreateReminderEvent.OnCustomReminderDateSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) }
        )
    }
    if (state.showCustomReminderTimePicker) {
        TimePickerModal(
            selectedTime = state.customReminderTime ?: state.selectedTime,
            onTimeSelected = { onEvent(CreateReminderEvent.OnCustomReminderTimeSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) }
        )
    }
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
    if (state.showIconPicker) {
        IconPickerDialog(
            selectedIcon = state.icon,
            onIconSelected = { onEvent(CreateReminderEvent.OnIconSelected(it)) },
            onDismiss = { onEvent(CreateReminderEvent.OnToggleIconPicker) }
        )
    }
    if (showRecurrenceModal) {
        RecurrenceSelectionDialog(
            currentRule = state.recurrence,
            startDate = state.selectedDate,
            onRecurrenceSelected = { rule -> onEvent(CreateReminderEvent.OnRecurrenceSelected(rule)) },
            onCustomSelected = { onEvent(CreateReminderEvent.OnCustomRecurrenceToggled) },
            onDismissRequest = { showRecurrenceModal = false }
        )
    }
    if (state.showPremiumDialog) {
        if (state.isMaxLimitReached) {
            MaxLimitReachedDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(CreateReminderEvent.OnDismissPremiumDialog) }
            )
        } else {
            PremiumLimitDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(CreateReminderEvent.OnDismissPremiumDialog) },
                onUpgrade = {
                     onEvent(CreateReminderEvent.OnNavigateToSubscription)
                }
            )
        }
    }
    if (showPriorityDialog) {
        PrioritySelectionDialog(
            selectedPriority = state.priority,
            onPrioritySelected = { onEvent(CreateReminderEvent.OnPrioritySelected(it)) },
            onDismissRequest = { showPriorityDialog = false }
        )
    }
    if (showRemindMeDialog) {
        RemindMeSelectionDialog(
            selectedMode = state.reminderTimeMode,
            onModeSelected = { onEvent(CreateReminderEvent.OnReminderTimeModeChanged(it)) },
            onDismissRequest = { showRemindMeDialog = false }
        )
    }

    LaunchedEffect(state.isSaveSuccess) {
        if (state.isSaveSuccess) {
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .padding(bottom = 16.dp)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = if (state.isEditing) "Edit reminder" else "New reminder",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                enabled = !state.isSaving,
                onClick = {
                    onEvent(CreateReminderEvent.OnSave)
                    // Removed immediate onDismiss, relying on LaunchedEffect
                }
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (state.isEditing) "Update" else "Save",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // --- MAIN CARD ---
            MainInputCard(state, onEvent, groups, tags)
            if (state.titleError != null) {
                Text(
                    text = state.titleError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            // --- SECTIONS ---
            
            // Due Date
            SectionHeader("Due Date")
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     DateTimePickerBox(
                         text = formatDate(state.selectedDate),
                         icon = Icons.Default.CalendarMonth,
                         onClick = { onEvent(CreateReminderEvent.OnToggleDatePicker) },
                         modifier = Modifier.weight(1f),
                         isError = state.dueDateTimeError != null
                     )
                     DateTimePickerBox(
                         text = if(state.hasSpecificTime) formatTime(state.selectedTime) else "Set Time",
                         icon = Icons.Default.AccessTime,
                         onClick = { onEvent(CreateReminderEvent.OnToggleTimePicker) },
                         modifier = Modifier.weight(1f),
                         isError = state.dueDateTimeError != null
                     )
                }
                if (state.dueDateTimeError != null) {
                    Text(
                        text = state.dueDateTimeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Subtasks
            SectionHeader(
                text = "Subtasks",
                trailingContent = {
                     Icon(
                         imageVector = Icons.Default.Add,
                         contentDescription = "Add Subtask",
                         tint = MaterialTheme.colorScheme.primary,
                         modifier = Modifier
                             .size(24.dp)
                             .clickable { isSubtaskInputVisible = true }
                     )
                }
            )
           
            if (isSubtaskInputVisible || state.subtaskInput.isNotEmpty()) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.subtaskInput,
                        onValueChange = { onEvent(CreateReminderEvent.OnSubtaskInputChanged(it)) },
                        placeholder = { Text("Add subtask...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            onEvent(CreateReminderEvent.OnAddSubtask)
                        },
                        enabled = state.subtaskInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, "Add subtask")
                    }
                }
            }
            if (state.subtasks.isNotEmpty()) {
                 Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                     state.subtasks.forEach { subtask ->
                         SubtaskItemShort(
                             subtask = subtask,
                             onToggle = { onEvent(CreateReminderEvent.OnSubtaskToggled(subtask.id)) },
                             onDelete = { onEvent(CreateReminderEvent.OnSubtaskDeleted(subtask.id)) }
                         )
                     }
                 }
            }

            // Deadline
            SectionHeader(
                text = "Deadline",
                trailingContent = {
                    Switch(
                        checked = state.isDeadlineEnabled,
                        onCheckedChange = { onEvent(CreateReminderEvent.OnDeadlineToggled(it)) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            )
            if (state.isDeadlineEnabled) {
                 Column {
                     Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         DateTimePickerBox(
                             text = state.deadlineDate?.let { formatDate(it) } ?: "Set date",
                             icon = Icons.Default.CalendarMonth,
                             onClick = { onEvent(CreateReminderEvent.OnToggleDeadlineDatePicker) },
                             modifier = Modifier.weight(1f),
                             isError = state.deadlineError != null
                         )
                         DateTimePickerBox(
                             text = state.deadlineTime?.let { formatTime(it) } ?: "Set time",
                             icon = Icons.Default.AccessTime,
                             onClick = { onEvent(CreateReminderEvent.OnToggleDeadlineTimePicker) },
                             modifier = Modifier.weight(1f),
                             isError = state.deadlineError != null
                         )
                     }
                     if (state.deadlineError != null) {
                        Text(
                            text = state.deadlineError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                 }
            }

            // Remind me
            Column {
                CompactSelectableRow(
                    title = "Remind Me",
                    selectedOption = when (state.reminderTimeMode) {
                        ReminderTimeMode.AT_DUE_TIME -> "At time"
                        ReminderTimeMode.BEFORE_DUE_TIME -> "Before event"
                        ReminderTimeMode.CUSTOM_TIME -> "Custom"
                    },
                    onClick = { showRemindMeDialog = true },
                    icon = Icons.Default.Notifications
                )

                if (state.reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME) {
                         ScrollableRowOfChips(
                         options = ReminderUtils.REMINDER_OFFSETS,
                         selectedFn = { it == state.beforeDueOffset },
                         onSelect = { onEvent(CreateReminderEvent.OnBeforeDueOffsetChanged(it)) },
                         modifier = Modifier.padding(start = 16.dp)
                     )
                }
                 if (state.reminderTimeMode == ReminderTimeMode.CUSTOM_TIME) {
                     Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         DateTimePickerBox(
                             text = state.customReminderDate?.let { formatDate(it) } ?: "Set date",
                             icon = Icons.Default.CalendarMonth,
                             onClick = { onEvent(CreateReminderEvent.OnToggleCustomReminderDatePicker) },
                             modifier = Modifier.weight(1f),
                             isError = state.reminderTimeError != null
                         )
                         DateTimePickerBox(
                             text = state.customReminderTime?.let { formatTime(it) } ?: "Set time",
                             icon = Icons.Default.AccessTime,
                             onClick = { onEvent(CreateReminderEvent.OnToggleCustomReminderTimePicker) },
                             modifier = Modifier.weight(1f),
                             isError = state.reminderTimeError != null
                         )
                     }
                 }
                 if (state.reminderTimeError != null) {
                    Text(
                        text = state.reminderTimeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Priority
            CompactSelectableRow(
                title = "Priority", // User used "Task Urgency" in screenshot
                selectedOption = state.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showPriorityDialog = true },
                icon = Icons.Default.Flag
            )
            
            // Repeat
            CompactSelectableRow(
                title = "Repeat",
                selectedOption = state.recurrenceText,
                onClick = { showRecurrenceModal = true },
                icon = Icons.Default.Repeat
            )
            
            if (state.customRecurrenceMode) {
                CustomRecurrenceSection(state = state, onEvent = onEvent)
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun MainInputCard(
    state: CreateReminderState,
    onEvent: (CreateReminderEvent) -> Unit,
    groups: List<ReminderGroup>,
    tags: List<Tag>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group Pill
            GroupPill(
                selectedClientId = state.selectedGroupId,
                groups = groups,
                onSelectGroup = { onEvent(CreateReminderEvent.OnGroupSelected(it)) },
                onCreateGroup = { onEvent(CreateReminderEvent.OnCreateGroup(it)) }
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Title Input
            TextField(
                value = state.title,
                onValueChange = { onEvent(CreateReminderEvent.OnTitleChanged(it)) },
                placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            
            // Note Input
            TextField(
                value = state.description,
                onValueChange = { onEvent(CreateReminderEvent.OnDescriptionChanged(it)) },
                placeholder = { Text("Note", style = MaterialTheme.typography.bodyLarge) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Decorations Row (Emoji, Color, Tags)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Emoji
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onEvent(CreateReminderEvent.OnToggleIconPicker) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.icon ?: "💪", style = MaterialTheme.typography.titleMedium)
                }
                
                // Color Picker (Anchored Popup)
                ColorPickerAnchor(
                    selectedColorHex = state.colorHex,
                    onColorSelected = { onEvent(CreateReminderEvent.OnColorSelected(it)) }
                )
                
                // Tags
                TagsRow(
                    tags = tags, 
                    selectedTagIds = state.selectedTags,
                    onTagToggle = { onEvent(CreateReminderEvent.OnTagToggled(it)) },
                    onCreateTag = { onEvent(CreateReminderEvent.OnCreateTag(it)) }
                )
            }
        }
    }
}

@Composable
fun GroupPill(
    selectedClientId: String?,
    groups: List<ReminderGroup>,
    onSelectGroup: (String?) -> Unit,
    onCreateGroup: (String) -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }
    val selectedGroup = groups.find { it.id == selectedClientId }
    
    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = selectedGroup?.colorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showPopup = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = selectedGroup?.name ?: "Group",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedGroup != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                if (selectedGroup != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove group",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onSelectGroup(null) }
                    )
                }
            }
        }
        
        if (showPopup) {
            GroupSelectionPopup(
                groups = groups,
                onDismissRequest = { showPopup = false },
                onGroupSelected = { 
                    onSelectGroup(it)
                    showPopup = false
                },
                onCreateGroup = {
                    onCreateGroup(it)
                    showPopup = false
                }
            )
        }
    }
}

@Composable
fun GroupSelectionPopup(
    groups: List<ReminderGroup>,
    onDismissRequest: () -> Unit,
    onGroupSelected: (String) -> Unit,
    onCreateGroup: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val density = LocalDensity.current
    
    Popup(
        alignment = Alignment.TopStart,
        offset = with(density) { IntOffset(0, 40.dp.roundToPx()) },
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(300.dp)
                .padding(top = 16.dp) // Offset from top
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or create group") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                val filtered = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
                
               LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(filtered) { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupSelected(group.id) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(group.colorHex.let { parseHexColor(it) }, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(group.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    if (searchQuery.isNotBlank() && filtered.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                       item {
                           Row(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .clickable { onCreateGroup(searchQuery) }
                                   .padding(vertical = 12.dp, horizontal = 8.dp),
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                               Spacer(Modifier.width(8.dp))
                               Text("Create \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                           }
                       }
                    }
                }
            }
        }
    }
}




@Composable
fun TagsRow(
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         // Selected Tags
         items(tags.filter { selectedTagIds.contains(it.id) }) { tag ->
             Surface(
                 shape = RoundedCornerShape(50),
                 color = MaterialTheme.colorScheme.secondaryContainer,
                 modifier = Modifier.clickable { onTagToggle(tag.id) }
             ) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                 ) {
                     Text(tag.name, style = MaterialTheme.typography.labelSmall)
                     Spacer(Modifier.width(4.dp))
                     Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                 }
             }
         }
         
         // Add Tag Button
         item {
             Box {
                 Box(
                     modifier = Modifier
                         .clip(RoundedCornerShape(50))
                         .background(Color.Transparent)
                         .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                         .clickable { showPopup = true }
                         .padding(horizontal = 12.dp, vertical = 6.dp)
                 ) {
                     Text(
                         "Add tag", 
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.primary
                     )
                 }
                 
                 if (showPopup) {
                     TagSelectionPopup(
                         tags = tags,
                         onDismissRequest = { showPopup = false },
                         onTagToggle = onTagToggle,
                         onCreateTag = {
                             onCreateTag(it)
                             showPopup = false
                         }
                     )
                 }
             }
         }
    }
}

@Composable
fun TagSelectionPopup(
    tags: List<Tag>,
    onDismissRequest: () -> Unit,
    onTagToggle: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Popup(
        alignment = Alignment.BottomStart, // Position near the button
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(250.dp)
                .padding(bottom = 50.dp) // Lift up
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or create tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(8.dp))
                
                val filtered = tags.filter { it.name.contains(searchQuery, ignoreCase = true) }
                 LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(filtered) { tag ->
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTagToggle(tag.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Icon(Icons.Default.Label, null, modifier = Modifier.size(16.dp))
                             Spacer(Modifier.width(8.dp))
                             Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (searchQuery.isNotBlank() && filtered.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                        item {
                           Row(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .clickable { onCreateTag(searchQuery) }
                                   .padding(vertical = 12.dp),
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                               Spacer(Modifier.width(8.dp))
                               Text("Create \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                           }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubtaskItemShort(
    subtask: com.bhaskar.synctask.domain.model.SubTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
         Checkbox(
             checked = subtask.isCompleted,
             onCheckedChange = { onToggle() },
             modifier = Modifier.size(24.dp)
         )
         Spacer(Modifier.width(8.dp))
         Text(
             text = subtask.title,
             style = MaterialTheme.typography.bodyMedium,
             modifier = Modifier.weight(1f)
         )
         IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
             Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
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

fun formatDate(date: LocalDate): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        else -> "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"
    }
}
