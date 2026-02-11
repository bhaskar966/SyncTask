package com.bhaskar.synctask.notificationDialogsActivities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.create.components.ReminderTimeMode
import com.bhaskar.synctask.presentation.create.ui_components.CompactSelectableRow
import com.bhaskar.synctask.presentation.create.ui_components.DatePickerModal
import com.bhaskar.synctask.presentation.create.ui_components.RemindMeSelectionDialog
import com.bhaskar.synctask.presentation.create.ui_components.TimePickerModal
import com.bhaskar.synctask.presentation.create.utils.ReminderUtils
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import org.koin.android.ext.android.inject
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

class RescheduleDialogActivity : ComponentActivity() {

    private val repository: ReminderRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: ""
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"

        if (reminderId.isEmpty()) {
            finish()
            return
        }

        setContent {
            SyncTaskTheme {
                var initialDate by remember { mutableStateOf<LocalDate?>(null) }
                var initialTime by remember { mutableStateOf<LocalTime?>(null) }
                // Use nullable Long for reminderTime to indicate if it exists
                var initialReminderTime by remember { mutableStateOf<Long?>(null) }

                LaunchedEffect(reminderId) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val reminder = repository.getReminderById(reminderId).firstOrNull()
                        if (reminder != null) {
                            val dueDateTime = Instant.fromEpochMilliseconds(reminder.dueTime)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                            initialDate = dueDateTime.date
                            initialTime = dueDateTime.time
                            initialReminderTime = reminder.reminderTime
                        }
                    }
                }

                if (initialDate != null && initialTime != null) {
                    RescheduleDialog(
                        title = title,
                        initialDate = initialDate!!,
                        initialTime = initialTime!!,
                        initialReminderTime = initialReminderTime,
                        onReschedule = { newDueTime, newReminderTime ->
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.rescheduleReminder(reminderId, newDueTime, newReminderTime)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        this@RescheduleDialogActivity,
                                        "📅 Rescheduled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    //  Clear notification
                                    cancelNotification(reminderId)
                                    finish()
                                }
                            }
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    private fun cancelNotification(reminderId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.hashCode())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RescheduleDialog(
    title: String,
    initialDate: LocalDate,
    initialTime: LocalTime,
    initialReminderTime: Long?,
    onReschedule: (Long, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime) }
    
    // Calculate initial reminder mode
    var reminderTimeMode by remember(initialReminderTime, initialDate, initialTime) {
        mutableStateOf(
            if (initialReminderTime != null) {
                val dueInstant = isDateAtTime(initialDate, initialTime)
                if (initialReminderTime == dueInstant) ReminderTimeMode.AT_DUE_TIME
                else if (initialReminderTime < dueInstant) {
                    val offset = dueInstant - initialReminderTime
                    if (ReminderUtils.COMMON_OFFSETS.contains(offset)) ReminderTimeMode.BEFORE_DUE_TIME else ReminderTimeMode.CUSTOM_TIME
                }
                else ReminderTimeMode.CUSTOM_TIME
            } else {
                ReminderTimeMode.AT_DUE_TIME 
            }
        )
    }

    var beforeDueOffset by remember { mutableLongStateOf(1800000L) } // Default 30 min
    
    // Initialize offset if mode is BEFORE_DUE_TIME
    LaunchedEffect(initialReminderTime, initialDate, initialTime) {
        if (initialReminderTime != null) {
             val dueInstant = isDateAtTime(initialDate, initialTime)
             if (initialReminderTime < dueInstant) {
                 val offset = dueInstant - initialReminderTime
                 if (ReminderUtils.COMMON_OFFSETS.contains(offset)) beforeDueOffset = offset
             }
        }
    }

    // Dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRemindMeDialog by remember { mutableStateOf(false) }

    // Custom reminder time/date
    var customReminderDate by remember(initialReminderTime) { 
        mutableStateOf(
            if (initialReminderTime != null) 
                Instant.fromEpochMilliseconds(initialReminderTime).toLocalDateTime(TimeZone.currentSystemDefault()).date
            else null
        ) 
    }
    var customReminderTime by remember(initialReminderTime) { 
        mutableStateOf(
            if (initialReminderTime != null) 
                Instant.fromEpochMilliseconds(initialReminderTime).toLocalDateTime(TimeZone.currentSystemDefault()).time
            else null
        ) 
    }
    var showCustomReminderDatePicker by remember { mutableStateOf(false) }
    var showCustomReminderTimePicker by remember { mutableStateOf(false) }


    // Dialog Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Dark/Surface color
             border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reschedule",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                 Text(
                    text = "Due date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                 )
                 Spacer(modifier = Modifier.height(8.dp))

                // Date & Time Selection Row
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateTimePickerBox(
                        text = formatDate(selectedDate),
                        icon = Icons.Default.CalendarMonth,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    DateTimePickerBox(
                        text = formatTime(selectedTime),
                        icon = Icons.Default.AccessTime,
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Remind Me Section
                CompactSelectableRow(
                    title = "Remind me",
                    selectedOption = when (reminderTimeMode) {
                        ReminderTimeMode.AT_DUE_TIME -> "At time"
                        ReminderTimeMode.BEFORE_DUE_TIME -> "Before event"
                        ReminderTimeMode.CUSTOM_TIME -> "Custom"
                    },
                    onClick = { showRemindMeDialog = true },
                   // icon = Icons.Default.Notifications // Icon removed to match cleaner look or screenshot? Screenshot had no icon on left of "Remind me"
                )
                
                // Dynamic content for Remind Me
                 if (reminderTimeMode == ReminderTimeMode.BEFORE_DUE_TIME) {
                    Spacer(modifier = Modifier.height(8.dp))
                     FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = ReminderUtils.REMINDER_OFFSETS
                        options.forEach { (value, label) ->
                             FilterChip(
                                selected = value == beforeDueOffset,
                                onClick = { beforeDueOffset = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
                 if (reminderTimeMode == ReminderTimeMode.CUSTOM_TIME) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateTimePickerBox(
                            text = customReminderDate?.let { formatDate(it) } ?: "Set date",
                            icon = Icons.Default.CalendarMonth,
                            onClick = { showCustomReminderDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        DateTimePickerBox(
                            text = customReminderTime?.let { formatTime(it) } ?: "Set time",
                            icon = Icons.Default.AccessTime,
                            onClick = { showCustomReminderTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                 }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                     modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "Cancel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                 VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 12.dp)
                 )

                TextButton(
                    onClick = {
                         val dueInstant = selectedDate.atTime(selectedTime)
                            .toInstant(TimeZone.currentSystemDefault())
                            .toEpochMilliseconds()
                            
                        val newReminderTime = when (reminderTimeMode) {
                            ReminderTimeMode.AT_DUE_TIME -> dueInstant
                            ReminderTimeMode.BEFORE_DUE_TIME -> dueInstant - beforeDueOffset
                             ReminderTimeMode.CUSTOM_TIME -> {
                                 if (customReminderDate != null && customReminderTime != null) {
                                     customReminderDate!!.atTime(customReminderTime!!)
                                        .toInstant(TimeZone.currentSystemDefault())
                                        .toEpochMilliseconds()
                                 } else {
                                     null // Fallback or assume unchanged? Or just don't set if missing
                                 }
                             }
                        }
                        onReschedule(dueInstant, newReminderTime)
                    },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "Reschedule",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }

    // Pickers
    if (showDatePicker) {
        DatePickerModal(
            selectedDate = selectedDate,
            onDateSelected = { 
                selectedDate = it 
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showTimePicker) {
         TimePickerModal(
            selectedTime = selectedTime,
            onTimeSelected = { 
                selectedTime = it 
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
    if (showRemindMeDialog) {
         RemindMeSelectionDialog(
            selectedMode = reminderTimeMode,
            onModeSelected = { reminderTimeMode = it },
            onDismissRequest = { showRemindMeDialog = false }
        )
    }
    // Custom Reminder Pickers
    if (showCustomReminderDatePicker) {
         DatePickerModal(
            selectedDate = customReminderDate ?: selectedDate,
            onDateSelected = { 
                customReminderDate = it 
                showCustomReminderDatePicker = false
            },
            onDismiss = { showCustomReminderDatePicker = false }
        )
    }
    if (showCustomReminderTimePicker) {
         TimePickerModal(
            selectedTime = customReminderTime ?: selectedTime,
            onTimeSelected = { 
                customReminderTime = it 
                showCustomReminderTimePicker = false
            },
            onDismiss = { showCustomReminderTimePicker = false }
        )
    }
}

// Helper to check epoch
private fun isDateAtTime(date: LocalDate, time: LocalTime): Long {
    return date.atTime(time).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

@Composable
fun DateTimePickerBox(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center // Centered content as per screenshot
    ) {
       // Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
       // Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Private helper functions
private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val period = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $period"
}

private fun formatDate(date: LocalDate): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        else -> "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"
    }
}