package com.bhaskar.synctask.notificationDialogsActivities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.android.inject
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

class SnoozeDialogActivity : ComponentActivity() {

    private val repository: ReminderRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: ""
        // Title is no longer used in the new design, but kept if needed
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"

        if (reminderId.isEmpty()) {
            finish()
            return
        }

        setContent {
            SyncTaskTheme {
                SnoozeDialog(
                    onSnooze = { totalMinutes ->
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.snoozeReminder(reminderId, totalMinutes)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    this@SnoozeDialogActivity,
                                    "Snoozed for $totalMinutes minutes",
                                    Toast.LENGTH_SHORT
                                ).show()
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

@Preview
@Composable
fun SnoozeDialog(
    onSnooze: (Int) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var sliderValue by remember { mutableFloatStateOf(5f) }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }

    // Calculate future time
    val futureTime = remember(sliderValue, selectedUnit) {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val timeZone = TimeZone.currentSystemDefault()
        val nowInstant = Instant.fromEpochMilliseconds(nowMillis)
            
        val amount = sliderValue.toInt()
        val futureInstant = when (selectedUnit) {
            TimeUnit.MINUTES -> nowInstant.plus(amount, DateTimeUnit.MINUTE, timeZone)
            TimeUnit.HOURS -> nowInstant.plus(amount, DateTimeUnit.HOUR, timeZone)
            TimeUnit.DAYS -> nowInstant.plus(amount, DateTimeUnit.DAY, timeZone)
        }
        futureInstant.toLocalDateTime(timeZone)
    }

    // Format: "Mon, 9 Feb 2026, 1:13 pm"
    // Using Java Time for easier formatting if needed, or manual kotlinx
    val formattedTime = remember(futureTime) {
        val dayOfWeek = futureTime.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val month = futureTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val day = futureTime.day
        val year = futureTime.year
        
        val hour24 = futureTime.hour

        val amPm = if (hour24 >= 12) "pm" else "am"
        val hour12 = if (hour24 > 12) hour24 - 12 else if (hour24 == 0) 12 else hour24
        val minute = futureTime.minute.toString().padStart(2, '0')
        
        "$dayOfWeek, $day $month $year, $hour12:$minute $amPm"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface), // Dark card as requested
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Snooze for",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "${sliderValue.toInt()}",
                    fontSize = 96.sp, // Big font
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = selectedUnit.displayName,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // Slider
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = selectedUnit.range,
                    steps = selectedUnit.steps,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.3f
                        ),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = formattedTime,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // Unit Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeUnit.entries.forEach { unit ->
                        val isSelected = selectedUnit == unit
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        
                        // Using Box with border to simulate outlined button with custom shape/style if needed
                        // Or just OutlinedButton
                        OutlinedButton(
                            onClick = { 
                                selectedUnit = unit 
                                // Reset or adjust slider value if needed, or keep relative scale? 
                                // User code earlier reset it, assume desirable.
                                sliderValue = when (unit) {
                                    TimeUnit.MINUTES -> 5f
                                    TimeUnit.HOURS -> 1f
                                    TimeUnit.DAYS -> 1f
                                }
                            },
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = unit.displayName.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            
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
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                VerticalDivider(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                
                TextButton(
                    onClick = {
                        val totalMinutes = when (selectedUnit) {
                            TimeUnit.MINUTES -> sliderValue.toInt()
                            TimeUnit.HOURS -> sliderValue.toInt() * 60
                            TimeUnit.DAYS -> sliderValue.toInt() * 60 * 24
                        }
                        onSnooze(totalMinutes)
                    },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                ) {

                    Text(
                        "Snooze",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
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
    MINUTES("minutes", 1f..60f, 58), // 1 to 60
    HOURS("hours", 1f..24f, 22),     // 1 to 24
    DAYS("days", 1f..31f, 29)        // 1 to 31
}
