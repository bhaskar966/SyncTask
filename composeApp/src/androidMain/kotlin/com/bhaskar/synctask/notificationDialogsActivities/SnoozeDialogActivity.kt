package com.bhaskar.synctask.notificationDialogsActivities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.data.NotificationActionHandler
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SnoozeDialogActivity : ComponentActivity() {

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
                SnoozeDialog(
                    title = title,
                    onSnooze = { minutes ->
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.snoozeReminder(reminderId, minutes)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    this@SnoozeDialogActivity,
                                    "⏰ Snoozed for $minutes minutes",
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

    private fun cancelNotification(reminderId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.hashCode())
    }
}

@Preview()
@Composable
fun SnoozeDialog(
    title: String = "",
    onSnooze: (Int) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var sliderValue by remember { mutableFloatStateOf(5f) }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }

    // ✅ Remove padding, fill entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Snooze Reminder",
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

                // Display snooze time
                Text(
                    text = "remind me in",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${sliderValue.toInt()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedUnit.displayName,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Slider
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = selectedUnit.range,
                    steps = selectedUnit.steps,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time unit selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = {
                                selectedUnit = unit
                                sliderValue = when (unit) {
                                    TimeUnit.MINUTES -> 5f
                                    TimeUnit.HOURS -> 1f
                                    TimeUnit.DAYS -> 1f
                                }
                            },
                            label = { Text(unit.displayName.uppercase()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Snooze button
                Button(
                    onClick = {
                        val totalMinutes = when (selectedUnit) {
                            TimeUnit.MINUTES -> sliderValue.toInt()
                            TimeUnit.HOURS -> sliderValue.toInt() * 60
                            TimeUnit.DAYS -> sliderValue.toInt() * 60 * 24
                        }
                        onSnooze(totalMinutes)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SNOOZE", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

enum class TimeUnit(
    val displayName: String,
    val range: ClosedFloatingPointRange<Float>,
    val steps: Int
) {
    MINUTES("minutes", 1f..60f, 58),
    HOURS("hours", 1f..24f, 22),
    DAYS("days", 1f..30f, 28)
}
