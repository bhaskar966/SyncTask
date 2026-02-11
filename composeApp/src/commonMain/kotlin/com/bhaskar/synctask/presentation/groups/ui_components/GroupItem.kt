package com.bhaskar.synctask.presentation.groups.ui_components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.model.SubTask
import com.bhaskar.synctask.presentation.list.ui_components.ReminderCard
import com.bhaskar.synctask.presentation.utils.parseHexColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItem(
    group: ReminderGroup,
    isExpanded: Boolean,
    reminders: List<Reminder>,
    is24HourFormat: Boolean,
    onToggleExpand: () -> Unit,
    onEditGroup: () -> Unit,
    onReminderClick: (String) -> Unit,
    onReminderLongClick: (Reminder, Offset, IntSize) -> Unit,
    onGroupLongClick: (Offset, IntSize) -> Unit,
    onReminderChecked: (Reminder, Boolean) -> Unit,
    onSubtaskChecked: (Reminder, SubTask, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupColor = parseHexColor(group.colorHex)
    var groupPosition by remember { mutableStateOf(Offset.Zero) }
    var groupSize by remember { mutableStateOf(IntSize.Zero) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                groupPosition = coordinates.positionInRoot()
                groupSize = coordinates.size
            }
            .combinedClickable(
                onClick = onToggleExpand, 
                onLongClick = { onGroupLongClick(groupPosition, groupSize) }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, groupColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Group Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emoji Icon with background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(groupColor.copy(alpha = 0.2f))
                        .border(1.dp, groupColor, CircleShape)
                        .clickable(onClick = onEditGroup),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Title and Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${reminders.size} reminders",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand Arrow
                if (reminders.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Reminders List
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
                            is24HourFormat = is24HourFormat,
                            onCheckedChange = { onReminderChecked(reminder, reminder.status != ReminderStatus.COMPLETED) },
                            onSubtaskChecked = { subtask, isChecked ->
                                onSubtaskChecked(reminder, subtask, isChecked)
                            },
                            onClick = { onReminderClick(reminder.id) },
                            onLongClick = { pos, size ->
                                onReminderLongClick(reminder, pos, size)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
