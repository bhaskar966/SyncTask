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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.bhaskar.synctask.presentation.theme.Indigo500
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecurrenceScreen(
    onNavigateBack: () -> Unit,
    onRuleConfirmed: (RecurrenceRule) -> Unit,
    viewModel: CustomRecurrenceViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var expandedFrequency by remember { mutableStateOf(false) }

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
                        val rule = viewModel.getRecurrenceRule()
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
                    IconButton(onClick = { viewModel.onEvent(CustomRecurrenceEvent.OnIntervalChanged(state.interval - 1)) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(state.interval.toString(), fontWeight = FontWeight.Bold)
                     IconButton(onClick = { viewModel.onEvent(CustomRecurrenceEvent.OnIntervalChanged(state.interval + 1)) }) {
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
                         Text(
                             state.frequency.name.lowercase().replaceFirstChar { it.uppercase() } + if(state.interval > 1) "s" else "",
                             fontWeight = FontWeight.Medium
                         )
                         Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        Frequency.entries.forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.onEvent(CustomRecurrenceEvent.OnFrequencyChanged(frequency))
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Days Section (Only if Weekly)
            if (state.frequency == Frequency.WEEKLY) {
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
                        val isSelected = state.selectedDays.contains(day)
                        DayButton(
                            label = days[day-1],
                            isSelected = isSelected,
                            onClick = { viewModel.onEvent(CustomRecurrenceEvent.OnDayToggled(day)) }
                        )
                    }
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
