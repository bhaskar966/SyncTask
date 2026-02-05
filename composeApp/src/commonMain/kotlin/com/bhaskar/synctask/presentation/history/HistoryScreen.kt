package com.bhaskar.synctask.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.list.components.ReminderListEvent
import com.bhaskar.synctask.presentation.list.components.ReminderListState
import com.bhaskar.synctask.presentation.list.ui_components.CompletedReminderCard
import com.bhaskar.synctask.presentation.list.ui_components.SectionHeader

@Composable
fun HistoryScreen(
    reminderListState: ReminderListState,
    onReminderScreenEvent: (ReminderListEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    // Filter by actual status to avoid duplicates
    val completedOnly = reminderListState.completedReminders.filter {
        it.status == ReminderStatus.COMPLETED
    }
    val dismissedOnly = reminderListState.completedReminders.filter {
        it.status == ReminderStatus.DISMISSED
    }
    val missedOnly = reminderListState.missedReminders // Already filtered by MISSED status

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Completed Section (ONLY COMPLETED status)
            if (completedOnly.isNotEmpty()) {
                item(key = "header_completed") {
                    SectionHeader(
                        title = "Completed",
                        count = completedOnly.size,
                        color = Color(0xFF10B981), // Green
                        icon = Icons.Filled.CheckCircle
                    )
                }
                items(
                    items = completedOnly,
                    key = { "completed_${it.id}" } // Add prefix to ensure uniqueness
                ) { reminder ->
                    CompletedReminderCard(
                        reminder = reminder,
                        onClick = { onNavigateToDetail(reminder.id) }
                    )
                }

                item(key = "spacer_after_completed") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Dismissed Section (ONLY DISMISSED status)
            if (dismissedOnly.isNotEmpty()) {
                item(key = "header_dismissed") {
                    SectionHeader(
                        title = "Dismissed",
                        count = dismissedOnly.size,
                        color = Color(0xFF6B7280), // Gray
                        icon = Icons.Filled.Block
                    )
                }
                items(
                    items = dismissedOnly,
                    key = { "dismissed_${it.id}" } // Add prefix to ensure uniqueness
                ) { reminder ->
                    CompletedReminderCard(
                        reminder = reminder,
                        onClick = { onNavigateToDetail(reminder.id) }
                    )
                }

                item(key = "spacer_after_dismissed") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Missed Section (ONLY MISSED status)
            if (missedOnly.isNotEmpty()) {
                item(key = "header_missed") {
                    SectionHeader(
                        title = "Missed",
                        count = missedOnly.size,
                        color = Color(0xFFDC2626), // Red
                        icon = Icons.Filled.Warning
                    )
                }
                items(
                    items = missedOnly,
                    key = { "missed_${it.id}" } // Add prefix to ensure uniqueness
                ) { reminder ->
                    CompletedReminderCard(
                        reminder = reminder,
                        onClick = { onNavigateToDetail(reminder.id) }
                    )
                }
            }

            // Empty state
            if (completedOnly.isEmpty() && dismissedOnly.isEmpty() && missedOnly.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📜",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "No History",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
