package com.bhaskar.synctask.presentation.recurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.theme.Indigo500
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceModal(
    startDate: LocalDate,
    onDismissRequest: () -> Unit,
    onRecurrenceSelected: (RecurrenceRule?) -> Unit,
    onCustomSelected: () -> Unit,
    currentRule: RecurrenceRule?
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Repeat",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            // Never
            RecurrenceOption(
                icon = Icons.Filled.DoNotDisturbOn,
                label = "Never",
                isSelected = currentRule == null,
                onClick = { onRecurrenceSelected(null) }
            )

            // Daily
            RecurrenceOption(
                icon = Icons.Filled.WbSunny,
                label = "Daily",
                isSelected = currentRule is RecurrenceRule.Daily && currentRule.interval == 1,
                onClick = { onRecurrenceSelected(RecurrenceRule.Daily(1)) }
            )

            // Weekly
            val dayOfWeekName = startDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            RecurrenceOption(
                icon = Icons.Filled.CalendarViewWeek,
                label = "Weekly on $dayOfWeekName",
                isSelected = currentRule is RecurrenceRule.Weekly && currentRule.interval == 1,
                onClick = { 
                    onRecurrenceSelected(RecurrenceRule.Weekly(1, listOf(startDate.dayOfWeek.isoDayNumber))) 
                }
            )

            // Monthly
            RecurrenceOption(
                icon = Icons.Filled.CalendarMonth,
                label = "Monthly on day ${startDate.day}",
                isSelected = currentRule is RecurrenceRule.Monthly && currentRule.interval == 1,
                onClick = { 
                     onRecurrenceSelected(RecurrenceRule.Monthly(1, startDate.day))
                }
            )

            // Weekdays (CustomDays effectively or Weekly with multiple days)
            // RecurrenceRule.Weekly(interval=1, days=[1,2,3,4,5]) ?
             RecurrenceOption(
                icon = Icons.Filled.Work,
                label = "Every weekday",
                subLabel = "Mon-Fri",
                isSelected = false, // Simplified check
                onClick = { 
                     onRecurrenceSelected(RecurrenceRule.Weekly(1, listOf(1,2,3,4,5)))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Custom
            RecurrenceOption(
                icon = Icons.Filled.Tune,
                label = "Custom...",
                isSelected = false,
                onClick = onCustomSelected
            )
        }
    }
}

@Composable
fun RecurrenceOption(
    icon: ImageVector,
    label: String,
    subLabel: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Indigo500 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if(isSelected) Indigo500 else MaterialTheme.colorScheme.onSurface
                )
            )
            if (subLabel != null) {
                Text(subLabel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Indigo500)
        }
    }
}
