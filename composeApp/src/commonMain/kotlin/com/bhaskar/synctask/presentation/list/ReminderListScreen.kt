package com.bhaskar.synctask.presentation.list

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import com.bhaskar.synctask.presentation.list.components.ReminderListEvent
import com.bhaskar.synctask.presentation.list.components.ReminderListState

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
                onSearchQueryChanged = { onReminderScreenEvent(ReminderListEvent.OnSearchQueryChanged(it)) },
                onNavigateToSettings = onNavigateToSettings
            )

            // Content List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overdue
                if (reminderListState.overdueReminders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Overdue",
                            count = reminderListState.overdueReminders.size,
                            color = Color(0xFFEF4444)
                        )
                    }
                    items(reminderListState.overdueReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onCheckedChange = { onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(reminder.id)) },
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }

                // Today
                if (reminderListState .todayReminders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Today",
                            count = reminderListState .todayReminders.size,
                            color = Indigo500
                        )
                    }
                    items(reminderListState   .todayReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onCheckedChange = { onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(reminder.id)) },
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }
                
                // Snoozed
                if (reminderListState .snoozedReminders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Snoozed",
                            count = reminderListState .snoozedReminders.size,
                            color = Amber500
                        )
                    }
                    items(reminderListState   .snoozedReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onCheckedChange = { onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(reminder.id)) },
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }

                // Later
                if (reminderListState .laterReminders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Later",
                            count = reminderListState .laterReminders.size,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(reminderListState   .laterReminders) { reminder ->
                         ReminderCard(
                            reminder = reminder,
                            onCheckedChange = { onReminderScreenEvent(ReminderListEvent.OnCompleteReminder(reminder.id)) },
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom padding for FAB
            }
        }
    }
}

@Composable
fun HeaderSection(
    syncDeviceCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminders",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                   IconButton(onClick = {}) {
                       Icon(Icons.Filled.FilterList, "Filter")
                   }
                   IconButton(onClick = onNavigateToSettings) {
                       Icon(Icons.Filled.Settings, "Settings")
                   }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Search reminders...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
        
        // Sync Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Indigo500.copy(alpha = 0.1f))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Cloud,
                contentDescription = null, 
                tint = Indigo500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Synced across $syncDeviceCount devices", // Mocked for now
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Indigo500,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
