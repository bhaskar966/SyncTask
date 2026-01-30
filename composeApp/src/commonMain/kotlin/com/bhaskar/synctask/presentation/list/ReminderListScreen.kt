package com.bhaskar.synctask.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.presentation.list.ui_components.ReminderCard
import com.bhaskar.synctask.presentation.list.ui_components.SectionHeader
import com.bhaskar.synctask.presentation.theme.Amber500
import com.bhaskar.synctask.presentation.theme.Indigo500
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.list.components.ReminderFilter
import com.bhaskar.synctask.presentation.list.components.ReminderListEvent
import com.bhaskar.synctask.presentation.list.components.ReminderListState
import com.bhaskar.synctask.presentation.list.ui_components.CompletedReminderCard
import com.bhaskar.synctask.presentation.list.ui_components.HeaderSection
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import org.koin.compose.koinInject
import kotlin.coroutines.EmptyCoroutineContext.get
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ReminderListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    reminderListState: ReminderListState,
    onReminderScreenEvent: (ReminderListEvent) -> Unit,
) {

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Indigo500,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section
            HeaderSection(
                syncDeviceCount = reminderListState.syncDeviceCount,
                searchQuery = reminderListState.searchQuery,
                onSearchQueryChanged = {
                    onReminderScreenEvent(ReminderListEvent.OnSearchQueryChanged(it))
                },
                onNavigateToSettings = onNavigateToSettings
            )

            // Filter Chips
            FilterSection(
                selectedFilter = reminderListState.selectedFilter,
                onFilterChanged = {
                    onReminderScreenEvent(ReminderListEvent.OnFilterChanged(it))
                }
            )

            // Main Content List
            ReminderSectionsList(
                state = reminderListState,
                onReminderClick = onNavigateToDetail,
                onCompleteReminder = {
                    onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(it))
                },
                onSnoozeReminder = { id, minutes ->
                    onReminderScreenEvent(ReminderListEvent.OnSnoozeReminder(id, minutes))
                }
            )
        }
    }
}

@Composable
private fun FilterSection(
    selectedFilter: ReminderFilter,
    onFilterChanged: (ReminderFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == ReminderFilter.ALL,
            onClick = { onFilterChanged(ReminderFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedFilter == ReminderFilter.ACTIVE,
            onClick = { onFilterChanged(ReminderFilter.ACTIVE) },
            label = { Text("Active") }
        )
        FilterChip(
            selected = selectedFilter == ReminderFilter.COMPLETED,
            onClick = { onFilterChanged(ReminderFilter.COMPLETED) },
            label = { Text("History") }
        )
    }
}

@Composable
private fun ReminderSectionsList(
    state: ReminderListState,
    onReminderClick: (String) -> Unit,
    onCompleteReminder: (String) -> Unit,
    onSnoozeReminder: (String, Int) -> Unit
) {
    val showActive = state.selectedFilter != ReminderFilter.COMPLETED
    val showCompleted = state.selectedFilter == ReminderFilter.ALL ||
            state.selectedFilter == ReminderFilter.COMPLETED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ACTIVE SECTIONS
        if (showActive) {
            // Missed Section
            if (state.missedReminders.isNotEmpty()) {
                item(key = "header_missed") {
                    SectionHeader(
                        title = "Missed",
                        count = state.missedReminders.size,
                        color = Color(0xFFDC2626),
                        icon = Icons.Filled.Warning
                    )
                }
                items(
                    items = state.missedReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }

            // Overdue Section
            if (state.overdueReminders.isNotEmpty()) {
                item(key = "header_overdue") {
                    SectionHeader(
                        title = "Overdue",
                        count = state.overdueReminders.size,
                        color = Color(0xFFEF4444),
                        icon = Icons.Filled.Warning
                    )
                }
                items(
                    items = state.overdueReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }

            // Snoozed Section
            if (state.snoozedReminders.isNotEmpty()) {
                item(key = "header_snoozed") {
                    SectionHeader(
                        title = "Snoozed",
                        count = state.snoozedReminders.size,
                        color = Amber500,
                        icon = Icons.Filled.Notifications
                    )
                }
                items(
                    items = state.snoozedReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }

            // Today Section
            if (state.todayReminders.isNotEmpty()) {
                item(key = "header_today") {
                    SectionHeader(
                        title = "Today",
                        count = state.todayReminders.size,
                        color = Indigo500,
                        icon = Icons.Filled.DateRange
                    )
                }
                items(
                    items = state.todayReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }

            // Tomorrow Section
            if (state.tomorrowReminders.isNotEmpty()) {
                item(key = "header_tomorrow") {
                    SectionHeader(
                        title = "Tomorrow",
                        count = state.tomorrowReminders.size,
                        color = Color(0xFF8B5CF6),
                        icon = Icons.Filled.DateRange
                    )
                }
                items(
                    items = state.tomorrowReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }

            // Later Section
            if (state.laterReminders.isNotEmpty()) {
                item(key = "header_later") {
                    SectionHeader(
                        title = "Later",
                        count = state.laterReminders.size,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Filled.DateRange
                    )
                }
                items(
                    items = state.laterReminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onCheckedChange = { onCompleteReminder(reminder.id) },
                        onClick = { onReminderClick(reminder.id) },
                        onSnooze = { minutes -> onSnoozeReminder(reminder.id, minutes) }
                    )
                }
            }
        }

        // COMPLETED SECTION
        if (showCompleted && state.completedReminders.isNotEmpty()) {
            item(key = "header_completed") {
                SectionHeader(
                    title = "Completed",
                    count = state.completedReminders.size,
                    color = Color(0xFF10B981),
                    icon = Icons.Filled.CheckCircle
                )
            }
            items(
                items = state.completedReminders,
                key = { it.id }
            ) { reminder ->
                CompletedReminderCard(
                    reminder = reminder,
                    onClick = { onReminderClick(reminder.id) }
                )
            }
        }

        // Empty state
        if (showActive &&
            state.overdueReminders.isEmpty() &&
            state.todayReminders.isEmpty() &&
            state.tomorrowReminders.isEmpty() &&
            state.laterReminders.isEmpty() &&
            state.snoozedReminders.isEmpty() &&
            state.missedReminders.isEmpty()) {
            item(key = "empty_state") {
                EmptyState()
            }
        }

        // Bottom spacing for FAB
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF10B981).copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All caught up!",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No reminders for now",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
