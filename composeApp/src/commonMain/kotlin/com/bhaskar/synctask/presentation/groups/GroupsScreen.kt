package com.bhaskar.synctask.presentation.groups


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.bhaskar.synctask.presentation.groups.components.GroupsEvent
import com.bhaskar.synctask.presentation.groups.ui_components.CreateGroupDialog
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.utils.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onNavigateToReminder: (String) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val ungroupedReminders by viewModel.ungroupedReminders.collectAsState()

    // Show create/edit dialog
    if (state.isDialogVisible) {
        CreateGroupDialog(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(GroupsEvent.HideDialog) }
        )
    }

    // Show premium dialog
    if (state.showPremiumDialog) {
        PremiumLimitDialog(
            message = state.premiumDialogMessage,
            onDismiss = { viewModel.onEvent(GroupsEvent.DismissPremiumDialog) },
            onUpgrade = {
                viewModel.onEvent(GroupsEvent.DismissPremiumDialog)
                onNavigateToSubscription()
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Group count indicator
                Text(
                    text = "${groups.size}/${SubscriptionConfig.getMaxGroups()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(GroupsEvent.ShowCreateDialog) },
                containerColor = Indigo500,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Group")
            }
        }
    ) { paddingValues ->
        if (groups.isEmpty() && ungroupedReminders.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📁",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "No Groups Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create groups to organize your reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User's groups
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        isExpanded = group.id in state.expandedGroupIds,
                        viewModel = viewModel,
                        onToggleExpand = { viewModel.onEvent(GroupsEvent.ToggleGroupExpanded(group.id)) },
                        onEdit = { viewModel.onEvent(GroupsEvent.ShowEditDialog(group)) },
                        onDelete = { viewModel.onEvent(GroupsEvent.DeleteGroup(group.id)) },
                        onReminderClick = onNavigateToReminder
                    )
                }

                // Ungrouped reminders section
                if (ungroupedReminders.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        UngroupedSection(
                            reminders = ungroupedReminders,
                            isExpanded = "ungrouped" in state.expandedGroupIds,
                            onToggleExpand = { viewModel.onEvent(GroupsEvent.ToggleGroupExpanded("ungrouped")) },
                            onReminderClick = onNavigateToReminder
                        )
                    }
                }

                // Bottom padding for FAB
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: ReminderGroup,
    isExpanded: Boolean,
    viewModel: GroupsViewModel,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReminderClick: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val reminders by remember(group.id) {
        viewModel.getRemindersForGroup(group.id)
    }.collectAsState()

    val reminderCount = reminders.size

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete Group?") },
            text = {
                Text(
                    if (reminderCount > 0) {
                        "$reminderCount reminder(s) will be moved to 'Ungrouped'."
                    } else {
                        "Are you sure you want to delete '${group.name}'?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = parseHexColor(group.colorHex).copy(alpha = 0.1f)
        )
    ) {
        Column {
            // Group Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onToggleExpand,
                        onLongClick = onEdit
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(group.colorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Name and count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$reminderCount reminder${if (reminderCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded reminders
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider()
                    if (reminders.isEmpty()) {
                        Text(
                            text = "No reminders in this group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        reminders.forEach { reminder ->
                            ReminderListItem(
                                reminder = reminder,
                                onClick = { onReminderClick(reminder.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UngroupedSection(
    reminders: List<Reminder>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onReminderClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ungrouped",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${reminders.size} reminder${if (reminders.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded reminders
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider()
                    reminders.forEach { reminder ->
                        ReminderListItem(
                            reminder = reminder,
                            onClick = { onReminderClick(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListItem(
    reminder: Reminder,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon (if available)
            if (reminder.icon != null) {
                Text(
                    text = reminder.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                Icon(
                    Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reminder info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (reminder.description != null) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Pin indicator
            if (reminder.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ✅ Simple Premium Dialog (you can enhance this later)
@Composable
private fun PremiumLimitDialog(
    message: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Upgrade to Premium") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}
