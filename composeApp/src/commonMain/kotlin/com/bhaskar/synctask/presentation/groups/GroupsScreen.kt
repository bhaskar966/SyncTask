package com.bhaskar.synctask.presentation.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.bhaskar.synctask.presentation.utils.parseHexColor
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.model.SubTask
import com.bhaskar.synctask.presentation.components.MaxLimitReachedDialog
import com.bhaskar.synctask.presentation.components.PremiumLimitDialog
import com.bhaskar.synctask.presentation.list.ui_components.ReminderCard
import com.bhaskar.synctask.presentation.groups.ui_components.GroupItem
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuOverlay
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuItem
import com.bhaskar.synctask.presentation.list.ui_components.HeaderSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    state: com.bhaskar.synctask.presentation.groups.components.GroupsState,
    onEvent: (GroupsEvent) -> Unit,
    onNavigateToReminder: (String) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Context Menu State
    var activeReminder by remember { mutableStateOf<Reminder?>(null) }
    var activeGroup by remember { mutableStateOf<ReminderGroup?>(null) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSize by remember { mutableStateOf(IntSize.Zero) }
    var isContextMenuVisible by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(targetValue = if (isContextMenuVisible) 16.dp else 0.dp)

    // Show create/edit dialog
    if (state.isDialogVisible) {
        CreateGroupDialog(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(GroupsEvent.HideDialog) }
        )
    }

    // Show premium dialog
    if (state.showPremiumDialog) {
        if (state.isMaxLimitReached) {
            MaxLimitReachedDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(GroupsEvent.DismissPremiumDialog) }
            )
        } else {
            PremiumLimitDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(GroupsEvent.DismissPremiumDialog) },
                onUpgrade = {
                    onEvent(GroupsEvent.DismissPremiumDialog)
                    onNavigateToSubscription()
                }
            )
        }
    }

    // Context Menu Overlay
    val contextMenuSurfaceCol = MaterialTheme.colorScheme.surface
    val contextMenuErrorCol = MaterialTheme.colorScheme.error
    val contextMenuItems = remember(activeReminder, activeGroup) {
        val items = mutableListOf<ContextMenuItem>()
        
        if (activeReminder != null) {
            val isPinned = activeReminder!!.isPinned
            items.add(
                ContextMenuItem(
                    label = if (isPinned) "Unpin" else "Pin",
                    icon = Icons.Default.PushPin,
                    color = contextMenuSurfaceCol,
                    onClick = {
                        onEvent(GroupsEvent.TogglePin(activeReminder!!))
                        isContextMenuVisible = false
                    }
                )
            )

            items.add(
                ContextMenuItem(
                    label = "Delete Reminder",
                    icon = Icons.Default.Delete,
                    color = contextMenuErrorCol,
                    onClick = {
                        onEvent(GroupsEvent.DeleteReminder(activeReminder!!.id))
                        isContextMenuVisible = false
                    }
                )
            )
        } else if (activeGroup != null) {
             items.add(
                ContextMenuItem(
                    label = "Delete Group",
                    icon = Icons.Default.Delete,
                    color = contextMenuErrorCol,
                    onClick = {
                        onEvent(GroupsEvent.DeleteGroup(activeGroup!!.id))
                        isContextMenuVisible = false
                    }
                )
            )
        }
        items
    }

    ContextMenuOverlay(
        visible = isContextMenuVisible,
        position = contextMenuPosition,
        size = contextMenuSize,
        onDismiss = { isContextMenuVisible = false },
        menuItems = contextMenuItems,
        content = {
            if (activeReminder != null) {
                // Show Reminder Card in overlay
                ReminderCard(
                    reminder = activeReminder!!,
                    onCheckedChange = {},
                    onSubtaskChecked = { _, _ -> },
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (activeGroup != null) {
                // Show Group Item (collapsed) in overlay
                GroupItem(
                    group = activeGroup!!,
                    isExpanded = false,
                    reminders = emptyList(), // Don't show reminders in context menu preview
                    onToggleExpand = {},
                    onEditGroup = {},
                    onReminderClick = {},
                    onReminderLongClick = { _, _, _ -> },
                    onGroupLongClick = { _, _ -> },
                    onReminderChecked = { _, _ -> },
                    onSubtaskChecked = { _, _, _ -> }
                )
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section
            HeaderSection(
                title = "Groups",
                syncDeviceCount = 0, // Placeholder, Groups screen might not show sync status or get it from common state
                searchQuery = state.searchQuery,
                onSearchQueryChanged = { onEvent(GroupsEvent.OnSearchQueryChanged(it)) },
                onNavigateToSettings = onNavigateToSettings,
                searchPlaceholder = "Search groups..."
            )

            if (state.groupsWithReminders.isEmpty() && state.ungroupedReminders.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ungrouped reminders section (Top)
                    if (state.ungroupedReminders.isNotEmpty()) {
                        item(key = "ungrouped_section") {
                            UngroupedSection(
                                reminders = state.ungroupedReminders,
                                isExpanded = "ungrouped" in state.expandedGroupIds,
                                onToggleExpand = {
                                    onEvent(GroupsEvent.ToggleGroupExpanded("ungrouped"))
                                },
                                onReminderClick = onNavigateToReminder,
                                onReminderChecked = { reminder, isCompleted ->
                                    onEvent(GroupsEvent.UpdateReminderStatus(reminder.id, isCompleted))
                                },
                                onSubtaskChecked = { reminder, subTask, isCompleted ->
                                    onEvent(GroupsEvent.UpdateSubtaskStatus(reminder, subTask, isCompleted))
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    // User's groups
                    items(state.groupsWithReminders, key = { it.group.id }) { item ->
                        val group = item.group
                        val groupReminders = item.reminders

                        GroupItem(
                            group = group,
                            isExpanded = group.id in state.expandedGroupIds,
                            reminders = groupReminders,
                            onToggleExpand = {
                                onEvent(GroupsEvent.ToggleGroupExpanded(group.id))
                            },
                            onEditGroup = { onEvent(GroupsEvent.ShowEditDialog(group)) },
                            onReminderClick = onNavigateToReminder,
                            onGroupLongClick = { pos, size ->
                                activeGroup = group
                                activeReminder = null
                                contextMenuPosition = pos
                                contextMenuSize = size
                                isContextMenuVisible = true
                            },
                            onReminderLongClick = { reminder, pos, size ->
                                activeReminder = reminder
                                activeGroup = null
                                contextMenuPosition = pos
                                contextMenuSize = size
                                isContextMenuVisible = true
                            },
                            onReminderChecked = { reminder, isCompleted ->
                                onEvent(GroupsEvent.UpdateReminderStatus(reminder.id, isCompleted))
                            },
                            onSubtaskChecked = { reminder, subTask, isCompleted ->
                                onEvent(GroupsEvent.UpdateSubtaskStatus(reminder, subTask, isCompleted))
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    // Bottom padding for FAB
                    item {
                        Spacer(Modifier.height(80.dp))
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
    onReminderClick: (String) -> Unit,
    onReminderChecked: (Reminder, Boolean) -> Unit,
    onSubtaskChecked: (Reminder, SubTask, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ungrouped",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${reminders.size} reminder${if (reminders.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                visible = isExpanded && reminders.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminders.forEach { reminder ->
                        ReminderCard(
                             reminder = reminder,
                             onCheckedChange = { onReminderChecked(reminder, reminder.status != ReminderStatus.COMPLETED) },
                             onSubtaskChecked = { subtask, isChecked -> onSubtaskChecked(reminder, subtask, isChecked) },
                             onClick = { onReminderClick(reminder.id) },
                             modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
