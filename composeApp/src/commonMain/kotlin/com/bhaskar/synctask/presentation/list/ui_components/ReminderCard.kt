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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.domain.model.Priority
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.presentation.theme.Amber500
import com.bhaskar.synctask.presentation.theme.Indigo500
import kotlinx.datetime.TimeZone
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlin.time.Instant

@Composable
fun ReminderCard(
    reminder: Reminder,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgent = reminder.priority == Priority.HIGH
    val isSnoozed = reminder.status == ReminderStatus.SNOOZED
    
    val borderColor = when {
        isUrgent -> Color(0xFFEF4444) // Red
        isSnoozed -> Amber500
        else -> Indigo500
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        
        Row(modifier = Modifier.height(80.dp)) {
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { onCheckedChange() }
                )
                
                Column(modifier = Modifier.weight(1f)) {
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (isUrgent) {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HIGH",
                                    color = Color(0xFFEF4444), // Text Urgent
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
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Due",
                            modifier = Modifier.size(14.dp),
                            tint = if (isUrgent) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(reminder.dueTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    // Simple format for MVP
    return "${datetime.hour}:${datetime.minute.toString().padStart(2, '0')}"
}
