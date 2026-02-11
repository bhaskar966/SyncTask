package com.bhaskar.synctask.presentation.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.presentation.components.MaxLimitReachedDialog
import com.bhaskar.synctask.presentation.components.PremiumLimitDialog
import com.bhaskar.synctask.presentation.list.components.ReminderListEvent
import com.bhaskar.synctask.presentation.list.components.ReminderListState
import com.bhaskar.synctask.presentation.list.ui_components.CollapsibleSectionHeader
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuOverlay
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuItem
import com.bhaskar.synctask.presentation.list.ui_components.HeaderSection
import com.bhaskar.synctask.presentation.list.ui_components.ReminderCard

@Composable
fun ReminderListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    reminderListState: ReminderListState,
    onReminderScreenEvent: (ReminderListEvent) -> Unit,
) {

    var activeReminder by remember { mutableStateOf<Reminder?>(null) }
    var activeReminderPosition by remember { mutableStateOf(Offset.Zero) }
    var activeReminderSize by remember { mutableStateOf(IntSize.Zero) }
    var isContextMenuVisible by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(targetValue = if (isContextMenuVisible) 3.dp else 0.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section
            HeaderSection(
                title = "Reminders",
                syncDeviceCount = reminderListState.syncDeviceCount,
                searchQuery = reminderListState.searchQuery,
                onSearchQueryChanged = {
                    onReminderScreenEvent(ReminderListEvent.OnSearchQueryChanged(it))
                },
                onNavigateToSettings = onNavigateToSettings,
                searchPlaceholder = "Search reminders..."
            )

            // Main Content List
            ReminderSectionsList(
                state = reminderListState,
                is24HourFormat = reminderListState.is24HourFormat, // Pass format
                onToggleSection = { sectionId ->
                    onReminderScreenEvent(ReminderListEvent.OnToggleSection(sectionId))
                },
                onReminderClick = onNavigateToDetail,
                onCompleteReminder = {
                    onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(it))
                },
                onSnoozeReminder = { id, minutes ->
                    onReminderScreenEvent(ReminderListEvent.OnSnoozeReminder(id, minutes))
                },
                onSubtaskChecked = { reminderId, subtaskId, isChecked ->
                    onReminderScreenEvent(ReminderListEvent.OnSubtaskCheckedChange(reminderId, subtaskId, isChecked))
                },
                onReminderLongClick = { reminder, position, size ->
                    activeReminder = reminder
                    activeReminderPosition = position
                    activeReminderSize = size
                    isContextMenuVisible = true
                }
            )
        }

        // Context Menu Items
        val contextMenuSurfaceCol = MaterialTheme.colorScheme.onSurface
        val contextMenuErrorCol = MaterialTheme.colorScheme.error
        val contextMenuItems = remember(activeReminder) {
            val items = mutableListOf<ContextMenuItem>()
            
            activeReminder?.let { reminder ->
                val isPinned = reminder.isPinned
                items.add(
                    ContextMenuItem(
                        label = if (isPinned) "Unpin" else "Pin",
                        icon = Icons.Default.PushPin,
                        color = contextMenuSurfaceCol,
                        onClick = {
                            onReminderScreenEvent(ReminderListEvent.OnTogglePin(reminder))
                            isContextMenuVisible = false
                        }
                    )
                )

                items.add(
                    ContextMenuItem(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        color = contextMenuErrorCol,
                        onClick = {
                            onReminderScreenEvent(ReminderListEvent.OnDeleteReminder(reminder.id))
                            isContextMenuVisible = false
                        }
                    )
                )
            }
            items
        }

        ContextMenuOverlay(
            visible = isContextMenuVisible,
            position = activeReminderPosition,
            size = activeReminderSize,
            onDismiss = { isContextMenuVisible = false },
            menuItems = contextMenuItems,
            content = {
                if (activeReminder != null) {
                    ReminderCard(
                        reminder = activeReminder!!,
                        is24HourFormat = reminderListState.is24HourFormat, // Pass format
                        onCheckedChange = {}, // Disable interaction in overlay
                        onSubtaskChecked = { _, _ -> },
                        onClick = {},
                        modifier = Modifier.width(with(LocalDensity.current) { activeReminderSize.width.toDp() })
                    )
                }
            }
        )

        if (reminderListState.showPremiumDialog) {
            if (reminderListState.isMaxLimitReached) {
                MaxLimitReachedDialog(
                    message = reminderListState.premiumDialogMessage,
                    onDismiss = { onReminderScreenEvent(ReminderListEvent.OnDismissPremiumDialog) }
                )
            } else {
               PremiumLimitDialog(
                    message = reminderListState.premiumDialogMessage,
                    onDismiss = { onReminderScreenEvent(ReminderListEvent.OnDismissPremiumDialog) },
                    onUpgrade = {
                        onReminderScreenEvent(ReminderListEvent.OnDismissPremiumDialog)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderSectionsList(
    state: ReminderListState,
    is24HourFormat: Boolean,
    onToggleSection: (String) -> Unit,
    onReminderClick: (String) -> Unit,
    onCompleteReminder: (String) -> Unit,
    onSnoozeReminder: (String, Int) -> Unit,
    onSubtaskChecked: (String, String, Boolean) -> Unit,
    onReminderLongClick: (Reminder, Offset, IntSize) -> Unit
) {
    // Local state for Pinned "Show More"
    var showAllPinned by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Pinned Section
        if (state.pinnedReminders.isNotEmpty()) {
            item(key = "header_pinned") {
                CollapsibleSectionHeader(
                    title = "Pinned",
                    count = state.pinnedReminders.size,
                    isExpanded = state.expandedSections.contains("pinned"),
                    onToggle = { onToggleSection("pinned") },
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (state.expandedSections.contains("pinned")) {
                val visiblePinned = if (showAllPinned) state.pinnedReminders else state.pinnedReminders.take(3)
                items(
                    items = visiblePinned,
                    key = { "pinned_${it.id}" }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
                if (!showAllPinned && state.pinnedReminders.size > 3) {
                    item(key = "pinned_show_more") {
                        TextButton(
                            onClick = { showAllPinned = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Show ${state.pinnedReminders.size - 3} more")
                        }
                    }
                }
            }
        }

        // Overdue Section
        if (state.overdueReminders.isNotEmpty()) {
            item(key = "header_overdue") {
                CollapsibleSectionHeader(
                    title = "Overdue",
                    count = state.overdueReminders.size,
                    isExpanded = state.expandedSections.contains("overdue"),
                    onToggle = { onToggleSection("overdue") },
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.expandedSections.contains("overdue")) {
                items(
                    items = state.overdueReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Snoozed Section
        if (state.snoozedReminders.isNotEmpty()) {
            item(key = "header_snoozed") {
                CollapsibleSectionHeader(
                    title = "Snoozed",
                    count = state.snoozedReminders.size,
                    isExpanded = state.expandedSections.contains("snoozed"),
                    onToggle = { onToggleSection("snoozed") },
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (state.expandedSections.contains("snoozed")) {
                items(
                    items = state.snoozedReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Today Section
        if (state.todayReminders.isNotEmpty()) {
            item(key = "header_today") {
                CollapsibleSectionHeader(
                    title = "Today",
                    count = state.todayReminders.size,
                    isExpanded = state.expandedSections.contains("today"),
                    onToggle = { onToggleSection("today") },
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (state.expandedSections.contains("today")) {
                items(
                    items = state.todayReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Tomorrow Section
        if (state.tomorrowReminders.isNotEmpty()) {
            item(key = "header_tomorrow") {
                CollapsibleSectionHeader(
                    title = "Tomorrow",
                    count = state.tomorrowReminders.size,
                    isExpanded = state.expandedSections.contains("tomorrow"),
                    onToggle = { onToggleSection("tomorrow") },
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (state.expandedSections.contains("tomorrow")) {
                items(
                    items = state.tomorrowReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Upcoming Section (was Later)
        if (state.laterReminders.isNotEmpty()) {
            item(key = "header_upcoming") {
                CollapsibleSectionHeader(
                    title = "Upcoming",
                    count = state.laterReminders.size,
                    isExpanded = state.expandedSections.contains("upcoming"),
                    onToggle = { onToggleSection("upcoming") },
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (state.expandedSections.contains("upcoming")) {
                items(
                    items = state.laterReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        is24HourFormat = is24HourFormat,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder.id, subtask.id, isChecked) },
                        onClick = { onReminderClick(reminder.id) },
                        onLongClick = { itemPos, itemSize -> onReminderLongClick(reminder, itemPos, itemSize) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
