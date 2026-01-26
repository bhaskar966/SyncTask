package com.bhaskar.synctask.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.theme.Indigo700
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailEvent
import com.bhaskar.synctask.presentation.detail.component.ReminderDetailState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReminderDetailScreen(
    onNavigateBack: () -> Unit,
    reminderId: String,
    reminderDetailState: ReminderDetailState,
    onReminderDetailEvent: (ReminderDetailEvent) -> Unit,
) {

    LaunchedEffect(Unit) {
        onReminderDetailEvent(ReminderDetailEvent.OnLoadReminder(reminderId))
    }

    val reminder = remember(reminderDetailState.reminder) { reminderDetailState.reminder }

    Scaffold(
        bottomBar = {
            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Simplified footer
                 Button(
                    onClick = { onReminderDetailEvent(ReminderDetailEvent.OnDelete); onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)), // Red-50, Red-600
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Reminder")
                }
            }
        }
    ) { paddingValues ->
        if (reminderDetailState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (reminder != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Indigo500, Indigo700)
                            )
                        )
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(top = 48.dp, start = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                
                // Content overlapping
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-60).dp)
                ) {
                     // Main Card
                     androidx.compose.material3.Card(
                         colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                         elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Column(Modifier.padding(24.dp)) {
                             Text(reminder.title, style = MaterialTheme.typography.headlineSmall)
                             // Render other details...
                         }
                     }
                }
            }
        } else {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Reminder not found")
            }
        }
    }
}
