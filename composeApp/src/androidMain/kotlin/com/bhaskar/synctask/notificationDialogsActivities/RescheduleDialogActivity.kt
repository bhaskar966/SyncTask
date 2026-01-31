package com.bhaskar.synctask.notificationDialogsActivities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.koin.android.ext.android.inject
import kotlin.time.Instant

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
                                    // ✅ Clear notification
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
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.hashCode())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // ✅ Pre-reminder settings
    var hasPreReminder by remember { mutableStateOf(initialReminderTime != null) }
    var preReminderTime by remember {
        mutableStateOf(
            if (initialReminderTime != null) {
                Instant.fromEpochMilliseconds(initialReminderTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).time
            } else {
                LocalTime(9, 0)
            }
        )
    }
    var showPreReminderTimePicker by remember { mutableStateOf(false) }

    // ✅ Remove padding from outer Box, fill entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 24.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Reschedule Reminder",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Due Date selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: $selectedDate")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Due Time selector
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Time: ${String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Pre-reminder toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasPreReminder,
                        onCheckedChange = { hasPreReminder = it }
                    )
                    Text("Set pre-reminder", fontSize = 14.sp)
                }

                // ✅ Pre-reminder time picker (only if enabled)
                if (hasPreReminder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showPreReminderTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pre-reminder at: ${String.format("%02d:%02d", preReminderTime.hour, preReminderTime.minute)}")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reschedule button
                Button(
                    onClick = {
                        val newDueTime = selectedDate
                            .atTime(selectedTime)
                            .toInstant(TimeZone.currentSystemDefault())
                            .toEpochMilliseconds()

                        val newReminderTime = if (hasPreReminder) {
                            selectedDate
                                .atTime(preReminderTime)
                                .toInstant(TimeZone.currentSystemDefault())
                                .toEpochMilliseconds()
                        } else {
                            null
                        }

                        onReschedule(newDueTime, newReminderTime)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RESCHEDULE", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }

    // ✅ Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
                    .atStartOfDayIn(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds()
            )
            DatePicker(state = datePickerState)

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                }
            }
        }
    }

    // ✅ Due Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    // ✅ Pre-reminder Time Picker Dialog
    if (showPreReminderTimePicker) {
        val preTimePickerState = rememberTimePickerState(
            initialHour = preReminderTime.hour,
            initialMinute = preReminderTime.minute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showPreReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    preReminderTime = LocalTime(preTimePickerState.hour, preTimePickerState.minute)
                    showPreReminderTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreReminderTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = preTimePickerState)
            }
        )
    }
}