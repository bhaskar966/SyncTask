package com.bhaskar.synctask.presentation.list.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.theme.Amber500
import com.bhaskar.synctask.presentation.theme.Indigo500
import kotlinx.datetime.TimeZone
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        Text(
            text = "$title ($count)".uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = color
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    onSnooze: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val isUrgent = reminder.priority == Priority.HIGH
    val isSnoozed = reminder.status == ReminderStatus.SNOOZED
    val isMissed = reminder.status == ReminderStatus.MISSED

    val borderColor = when {
        isMissed -> Color(0xFFDC2626)
        isUrgent -> Color(0xFFEF4444)
        isSnoozed -> Amber500
        else -> Indigo500
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.heightIn(min = 80.dp)) {
            // Colored strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { onCheckedChange() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isMissed) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFDC2626)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Title Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isUrgent) {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HIGH",
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time and recurrence info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Due",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDateTime(reminder.dueTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Recurrence indicator
                        if (reminder.recurrence != null) {
                            Surface(
                                color = Indigo500.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatRecurrence(reminder.recurrence),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp
                                    ),
                                    color = Indigo500,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Snooze button
                if (onSnooze != null && !isMissed) {
                    Box {
                        IconButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Snooze",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("15 minutes") },
                                onClick = {
                                    onSnooze(15)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("30 minutes") },
                                onClick = {
                                    onSnooze(30)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("1 hour") },
                                onClick = {
                                    onSnooze(60)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("2 hours") },
                                onClick = {
                                    onSnooze(120)
                                    showSnoozeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Completed",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF10B981)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                reminder.completedAt?.let { completedTime ->
                    Text(
                        text = "Completed ${formatDateTime(completedTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Helper functions
fun formatDateTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val timeStr = "${datetime.hour.toString().padStart(2, '0')}:${datetime.minute.toString().padStart(2, '0')}"

    return when {
        datetime.date == now.date -> "Today $timeStr"
        datetime.date == now.date.plus(DatePeriod(days = 1)) -> "Tomorrow $timeStr"
        datetime.date == now.date.minus(DatePeriod(days = 1)) -> "Yesterday $timeStr"
        else -> "${datetime.date.month.name.lowercase().capitalize()} ${datetime.date.day}, $timeStr"
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
