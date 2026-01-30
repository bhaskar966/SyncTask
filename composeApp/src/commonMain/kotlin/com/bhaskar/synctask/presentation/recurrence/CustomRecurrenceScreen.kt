package com.bhaskar.synctask.presentation.recurrence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceEvent
import com.bhaskar.synctask.presentation.recurrence.components.CustomRecurrenceState
import com.bhaskar.synctask.presentation.recurrence.components.EndMode
import com.bhaskar.synctask.presentation.recurrence.components.Frequency
import com.bhaskar.synctask.presentation.recurrence.components.toRecurrenceRule
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.create.components.ui_components.DatePickerModal
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecurrenceScreen(
    onNavigateBack: () -> Unit,
    onRuleConfirmed: (RecurrenceRule) -> Unit,
    customRecurrenceState: CustomRecurrenceState,
    onCustomRecurrenceEvent: (CustomRecurrenceEvent) -> Unit,
) {
    var expandedFrequency by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        DatePickerModal(
            selectedDate = customRecurrenceState.endDate?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date } ?: today,
            onDateSelected = { 
                onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndDateChanged(it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Recurrence", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Box(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val rule = customRecurrenceState.toRecurrenceRule()
                        onRuleConfirmed(rule)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Frequency Section
            Text(
                "Repeat every",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Stepper
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnIntervalChanged(customRecurrenceState.interval - 1)) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(customRecurrenceState.interval.toString(), fontWeight = FontWeight.Bold)
                     IconButton(onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnIntervalChanged(customRecurrenceState.interval + 1)) }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
                
                // Frequency Dropdown
                 Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
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
                          val label = when (customRecurrenceState.frequency) {
                              Frequency.DAILY -> "Day"
                              Frequency.WEEKLY -> "Week"
                              Frequency.MONTHLY -> "Month"
                              Frequency.YEARLY -> "Year"
                          }
                          Text(
                              label + if(customRecurrenceState.interval > 1) "s" else "",
                              fontWeight = FontWeight.Medium
                          )
                          Icon(Icons.Default.ExpandMore, null)
                     }
                     DropdownMenu(
                         expanded = expandedFrequency,
                         onDismissRequest = { expandedFrequency = false }
                     ) {
                         Frequency.entries.forEach { frequency ->
                             val label = when (frequency) {
                                  Frequency.DAILY -> "Day"
                                  Frequency.WEEKLY -> "Week"
                                  Frequency.MONTHLY -> "Month"
                                  Frequency.YEARLY -> "Year"
                             }
                             DropdownMenuItem(
                                 text = { Text(label) },
                                 onClick = {
                                     onCustomRecurrenceEvent(CustomRecurrenceEvent.OnFrequencyChanged(frequency))
                                     expandedFrequency = false
                                 }
                             )
                         }
                     }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Days Section (Only if Weekly)
            if (customRecurrenceState.frequency == Frequency.WEEKLY) {
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
                    // 1 = Monday, 7 = Sunday
                    (1..7).forEach { day ->
                        val isSelected = customRecurrenceState.selectedDays.contains(day)
                        DayButton(
                            label = days[day-1],
                            isSelected = isSelected,
                            onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnDayToggled(day)) }
                        )
                    }
                }
                 Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Repeat after completion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnFromCompletionToggled(!customRecurrenceState.fromCompletion)) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = customRecurrenceState.fromCompletion,
                    onCheckedChange = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnFromCompletionToggled(it)) },
                    colors = CheckboxDefaults.colors(checkedColor = Indigo500)
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        "Repeat after completion",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        if (customRecurrenceState.fromCompletion)
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
            
            // Never
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.NEVER)) }
            ) {
                RadioButton(
                    selected = customRecurrenceState.endMode == EndMode.NEVER,
                    onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.NEVER)) },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("Never", style = MaterialTheme.typography.bodyLarge)
            }
            
            // On Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.DATE)) }
            ) {
                RadioButton(
                    selected = customRecurrenceState.endMode == EndMode.DATE,
                    onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.DATE)) },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("On date", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))

                
                if (customRecurrenceState.endMode == EndMode.DATE) {
                     Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val dateText = customRecurrenceState.endDate?.let { 
                             val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                             "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"
                        } ?: "Select date"
                        Text(dateText, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
             // After occurrences
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.COUNT)) }
            ) {
                RadioButton(
                    selected = customRecurrenceState.endMode == EndMode.COUNT,
                    onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnEndModeChanged(EndMode.COUNT)) },
                    colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                )
                Text("After", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                
                if (customRecurrenceState.endMode == EndMode.COUNT) {
                     Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnOccurrenceCountChanged((customRecurrenceState.occurrenceCount ?: 1) - 1)) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                        }
                        Text((customRecurrenceState.occurrenceCount ?: 1).toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { onCustomRecurrenceEvent(CustomRecurrenceEvent.OnOccurrenceCountChanged((customRecurrenceState.occurrenceCount ?: 1) + 1)) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("occurrences", style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
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
            .background(if(isSelected) Indigo500 else Color.Transparent)
            .border(1.dp, if(isSelected) Indigo500 else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if(isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
