package com.bhaskar.synctask.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SnoozeScreen(
    reminderId: String,
    title: String,
    onDismiss: () -> Unit = {}
) {
    val repository: ReminderRepository = koinInject()
    val scope = rememberCoroutineScope()

    var sliderValue by remember { mutableStateOf(5f) }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }

    BackHandler(onBack = onDismiss)

    SyncTaskTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snooze Reminder",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

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

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = selectedUnit.range,
                        steps = selectedUnit.steps,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SNOOZE", modifier = Modifier.padding(8.dp))
                    }
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
