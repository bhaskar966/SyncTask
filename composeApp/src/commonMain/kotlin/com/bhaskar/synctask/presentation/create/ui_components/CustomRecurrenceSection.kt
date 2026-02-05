package com.bhaskar.synctask.presentation.create.ui_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.presentation.theme.Indigo500
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import com.bhaskar.synctask.presentation.create.components.CreateReminderEvent
import com.bhaskar.synctask.presentation.create.components.CreateReminderState
import com.bhaskar.synctask.presentation.create.components.RecurrenceEndMode
import com.bhaskar.synctask.presentation.create.components.RecurrenceFrequency
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant


@Composable
fun CustomRecurrenceSection(
    state: CreateReminderState,
    onEvent: (CreateReminderEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.customRecurrenceMode,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Header with Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Custom Recurrence",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { onEvent(CreateReminderEvent.OnCustomRecurrenceToggled) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Section
            Text(
                "Repeat every",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Interval Stepper
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            onEvent(CreateReminderEvent.OnRecurrenceIntervalChanged(state.recurrenceInterval - 1))
                        },
                        enabled = state.recurrenceInterval > 1
                    ) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(state.recurrenceInterval.toString(), fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            onEvent(CreateReminderEvent.OnRecurrenceIntervalChanged(state.recurrenceInterval + 1))
                        }
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                // Frequency Dropdown
                var expandedFrequency by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { expandedFrequency = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val label = when (state.recurrenceFrequency) {
                            RecurrenceFrequency.DAILY -> "Day"
                            RecurrenceFrequency.WEEKLY -> "Week"
                            RecurrenceFrequency.MONTHLY -> "Month"
                            RecurrenceFrequency.YEARLY -> "Year"
                        }
                        Text(
                            label + if (state.recurrenceInterval > 1) "s" else "",
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.ExpandMore, null)
                    }

                    DropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        RecurrenceFrequency.entries.forEach { frequency ->
                            val label = when (frequency) {
                                RecurrenceFrequency.DAILY -> "Day"
                                RecurrenceFrequency.WEEKLY -> "Week"
                                RecurrenceFrequency.MONTHLY -> "Month"
                                RecurrenceFrequency.YEARLY -> "Year"
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onEvent(CreateReminderEvent.OnRecurrenceFrequencyChanged(frequency))
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Days Section (Only if Weekly)
            if (state.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                Text(
                    "Repeats On",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    (1..7).forEach { day ->
                        val isSelected = state.recurrenceSelectedDays.contains(day)
                        DayButton(
                            label = days[day - 1],
                            isSelected = isSelected,
                            onClick = { onEvent(CreateReminderEvent.OnRecurrenceDayToggled(day)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Repeat after completion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(CreateReminderEvent.OnRecurrenceFromCompletionToggled(!state.recurrenceFromCompletion))
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.recurrenceFromCompletion,
                    onCheckedChange = {
                        onEvent(CreateReminderEvent.OnRecurrenceFromCompletionToggled(it))
                    },
                    colors = CheckboxDefaults.colors(checkedColor = Indigo500)
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        "Repeat after completion",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        if (state.recurrenceFromCompletion)
                            "Next reminder created only when you mark this done"
                        else
                            "Next reminder auto-created after due time passes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ends Section
            Text(
                "Ends",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Never
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.NEVER))
                    }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = state.recurrenceEndMode == RecurrenceEndMode.NEVER,
                    onClick = {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.NEVER))
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("Never", style = MaterialTheme.typography.bodyLarge)
            }

            // On Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.DATE))
                    }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = state.recurrenceEndMode == RecurrenceEndMode.DATE,
                    onClick = {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.DATE))
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("On date", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                if (state.recurrenceEndMode == RecurrenceEndMode.DATE) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { onEvent(CreateReminderEvent.OnToggleRecurrenceEndDatePicker) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val dateText = state.recurrenceEndDate?.let {
                            val date = Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"
                        } ?: "Select date"
                        Text(dateText, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // After occurrences
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.COUNT))
                    }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = state.recurrenceEndMode == RecurrenceEndMode.COUNT,
                    onClick = {
                        onEvent(CreateReminderEvent.OnRecurrenceEndModeChanged(RecurrenceEndMode.COUNT))
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("After", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                if (state.recurrenceEndMode == RecurrenceEndMode.COUNT) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onEvent(CreateReminderEvent.OnRecurrenceOccurrenceCountChanged(
                                    (state.recurrenceOccurrenceCount ?: 1) - 1
                                ))
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = (state.recurrenceOccurrenceCount ?: 1) > 1
                        ) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            (state.recurrenceOccurrenceCount ?: 1).toString(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                onEvent(CreateReminderEvent.OnRecurrenceOccurrenceCountChanged(
                                    (state.recurrenceOccurrenceCount ?: 1) + 1
                                ))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("occurrences", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun DayButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) Indigo500 else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) Indigo500 else MaterialTheme.colorScheme.outlineVariant,
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}