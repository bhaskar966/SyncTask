package com.bhaskar.synctask.presentation.list.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.model.SubTask
import com.bhaskar.synctask.presentation.theme.ReminderColors
import com.bhaskar.synctask.presentation.list.ui_components.CircularCheckbox
import com.bhaskar.synctask.presentation.list.ui_components.SubtaskItem
import com.bhaskar.synctask.presentation.theme.PredefinedIcons
import kotlinx.datetime.TimeZone
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.Instant
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onCheckedChange: () -> Unit,
    onSubtaskChecked: (SubTask, Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: ((Offset, IntSize) -> Unit)? = null,
    onSnooze: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    val isUrgent = reminder.priority == Priority.HIGH
    val isSnoozed = reminder.status == ReminderStatus.SNOOZED
    val isMissed = reminder.status == ReminderStatus.MISSED

    // Color Logic
    val reminderColor = reminder.colorHex?.let { ReminderColors.getColorByHex(it) }
        ?: MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInRoot()
                itemSize = coordinates.size
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick?.invoke(itemPosition, itemSize)
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Icon (Emoji in Circle)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(2.dp, reminderColor, CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reminder.icon ?: PredefinedIcons.getDefaultIcon(), // Default icon
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 14.sp
                    )
                }

                // Center Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Priority Indicator
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (reminder.priority == Priority.HIGH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = reminder.priority.name.lowercase().replaceFirstChar { it.titlecase() }, // "High", "Medium", "Low"
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reminder.priority == Priority.HIGH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Description
                    if (!reminder.description.isNullOrBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }

                    // Date & Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayText = if (isSnoozed && reminder.snoozeUntil != null) {
                            val duration = reminder.snoozeUntil - reminder.dueTime
                            val durationStr = formatDuration(duration)
                            "${formatDateTime(reminder.snoozeUntil)} (snoozed for $durationStr)"
                        } else {
                            formatDateTime(reminder.dueTime, reminder.deadline)
                        }

                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // Right Checkbox
                CircularCheckbox(
                    checked = false, // Main reminder not checked yet
                    onCheckedChange = onCheckedChange,
                    color = reminderColor,
                    size = 28.dp,
                    checkmarkSize = 18.dp
                )
            }
        }

        // Subtasks Section (if any)
        if (reminder.subtasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(start = 20.dp) // Indent
            ) {
                reminder.subtasks.forEach { subtask ->
                    SubtaskItem(
                        subtask = subtask,
                        onCheckedChange = { onSubtaskChecked(subtask, !subtask.isCompleted) },
                        accentColor = reminderColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Helper functions
fun formatDateTime(timestamp: Long, deadline: Long? = null): String {
    val timeZone = TimeZone.currentSystemDefault()
    val startInstant = Instant.fromEpochMilliseconds(timestamp)
    val startDateTime = startInstant.toLocalDateTime(timeZone)
    val now = Clock.System.now().toLocalDateTime(timeZone)

    val startTimeStr = "${startDateTime.hour.toString().padStart(2, '0')}:${
        startDateTime.minute.toString().padStart(2, '0')
    }"

    val datePart = when (startDateTime.date) {
        now.date -> "Today"
        now.date.plus(DatePeriod(days = 1)) -> "Tomorrow"
        now.date.minus(DatePeriod(days = 1)) -> "Yesterday"
        else -> "${
            startDateTime.date.month.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } ${startDateTime.date.day}"
    }

    if (deadline != null) {
        val endInstant = Instant.fromEpochMilliseconds(deadline)
        val endDateTime = endInstant.toLocalDateTime(timeZone)
        val endTimeStr = "${endDateTime.hour.toString().padStart(2, '0')}:${
            endDateTime.minute.toString().padStart(2, '0')
        }"

        return "$datePart $startTimeStr - $endTimeStr"
    }

    return "$datePart $startTimeStr"
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""}"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        else -> "less than a minute"
    }
}

fun formatRecurrence(recurrence: RecurrenceRule): String {
    return when (recurrence) {
        is RecurrenceRule.Daily -> {
            if (recurrence.interval == 1) "Daily"
            else "Every ${recurrence.interval} days"
        }

        is RecurrenceRule.Weekly -> {
            if (recurrence.interval == 1) "Weekly"
            else "Every ${recurrence.interval} weeks"
        }

        is RecurrenceRule.Monthly -> {
            if (recurrence.interval == 1) "Monthly"
            else "Every ${recurrence.interval} months"
        }

        is RecurrenceRule.Yearly -> {
            if (recurrence.interval == 1) "Yearly"
            else "Every ${recurrence.interval} years"
        }

        is RecurrenceRule.CustomDays -> {
            "Every ${recurrence.interval} days"
        }
    }
}
