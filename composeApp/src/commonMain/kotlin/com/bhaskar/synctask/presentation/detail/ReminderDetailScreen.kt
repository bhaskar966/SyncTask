import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.theme.Indigo700
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.bhaskar.synctask.domain.RecurrenceUtils
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailEvent
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailState
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun ReminderDetailScreen(
    onNavigateBack: () -> Unit,
    reminderId: String,
    reminderDetailState: ReminderDetailState,
    onReminderDetailEvent: (ReminderDetailEvent) -> Unit,
    onNavigateToEdit: (String) -> Unit
) {

    val reminder = reminderDetailState.allReminders.find { it.id == reminderId }

    LaunchedEffect(true) {
        println("is reminder there(ReminderScreen): ${reminderDetailState.allReminders.any { it.id == reminderId }}")
    }



    Scaffold(
        bottomBar = {
            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Simplified footer
                // Simplified footer
                 Button(
                    onClick = { 
                        onReminderDetailEvent(ReminderDetailEvent.OnDelete(reminderId = reminderId))
                        println("Delete button clicked in UI")
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)), // Red-50, Red-600
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Reminder")
                }
            }
        }
    ) { paddingValues ->
        if (reminder == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Indigo500, Indigo700)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onNavigateBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        IconButton(
                            onClick = { onNavigateToEdit(reminderId) }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                }

                // Content overlapping
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-60).dp)
                ) {
                     // Main Card
                     Card(
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                         elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Column(Modifier.padding(24.dp)) {
                             Text(reminder.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                             
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             // Priority
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, null, tint = Indigo500, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(reminder.priority.name, style = MaterialTheme.typography.labelMedium, color = Indigo500)
                             }

                             Spacer(modifier = Modifier.height(24.dp))
                             
                             // Description
                             if (!reminder.description.isNullOrBlank()) {
                                 Row(verticalAlignment = Alignment.Top) {
                                     Icon(Icons.AutoMirrored.Filled.Notes, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                     Spacer(modifier = Modifier.width(16.dp))
                                     Text(reminder.description, style = MaterialTheme.typography.bodyLarge)
                                 }
                                 Spacer(modifier = Modifier.height(24.dp))
                             }

                             HorizontalDivider()
                             Spacer(modifier = Modifier.height(16.dp))

                             // Due Date / Recurrence
                             val dueDateTime = Instant.fromEpochMilliseconds(reminder.dueTime).toLocalDateTime(TimeZone.currentSystemDefault())
                             DetailRow(
                                 icon = Icons.Default.Event,
                                 label = "Due: ${formatDateTime(dueDateTime)}",
                                 subLabel = if (reminder.recurrence != null) RecurrenceUtils.formatRecurrenceRule(reminder.recurrence) else null
                             )
                             
                             if (reminder.targetRemindCount != null) {
                                 Spacer(modifier = Modifier.height(16.dp))
                                 DetailRow(
                                     icon = Icons.Default.Repeat,
                                     label = "Occurrence",
                                     subLabel = "${reminder.currentReminderCount ?: 1} of ${reminder.targetRemindCount}"
                                 )
                             }

                             Spacer(modifier = Modifier.height(16.dp))
                             
                             // Deadline
                             if (reminder.deadline != null) {
                                  val deadlineDate = Instant.fromEpochMilliseconds(reminder.deadline).toLocalDateTime(TimeZone.currentSystemDefault())
                                  DetailRow(
                                     icon = Icons.Default.Event,
                                     label = "Deadline",
                                     subLabel = formatDateTime(deadlineDate)
                                  )
                                  Spacer(modifier = Modifier.height(16.dp))
                             }

                             // Reminder Time
                             if (reminder.reminderTime != null) {
                                  val remindDate = Instant.fromEpochMilliseconds(reminder.reminderTime).toLocalDateTime(TimeZone.currentSystemDefault())
                                  DetailRow(
                                     icon = Icons.Default.Notifications,
                                     label = "Remind me",
                                     subLabel = formatDateTime(remindDate)
                                  )
                             }
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    subLabel: String? = null
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Indigo500, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subLabel != null) {
                 Text(subLabel, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

private fun formatDateTime(dt: LocalDateTime): String {
    val month = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val minute = dt.minute.toString().padStart(2, '0')
    return "$month ${dt.day} • ${dt.hour}:${minute}"
}


