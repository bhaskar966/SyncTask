package com.bhaskar.synctask.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock

@Composable
fun SnoozeScreen(
    reminderId: String,
    title: String,
    onDismiss: () -> Unit = {}
) {
    val repository: ReminderRepository = koinInject()
    val scope = rememberCoroutineScope()

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

    SyncTaskTheme {
        // Use Surface to provide background color, simpler than Scaffold for a sheet
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle (Visual indicator for sheet)
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 24.dp)
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}

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
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                        
                        OutlinedButton(
                            onClick = { 
                                selectedUnit = unit 
                                sliderValue = when (unit) {
                                    TimeUnit.MINUTES -> 5f
                                    TimeUnit.HOURS -> 1f
                                    TimeUnit.DAYS -> 1f
                                }
                            },
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = unit.displayName.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Action Buttons
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Cancel", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Snooze Button
                    Button(
                        onClick = {
                            val totalMinutes = when (selectedUnit) {
                                TimeUnit.MINUTES -> sliderValue.toInt()
                                TimeUnit.HOURS -> sliderValue.toInt() * 60
                                TimeUnit.DAYS -> sliderValue.toInt() * 60 * 24
                            }

                            scope.launch {
                                repository.snoozeReminder(reminderId, totalMinutes)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Text(
                            "Snooze",
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
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
