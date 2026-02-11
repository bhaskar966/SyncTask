package com.bhaskar.synctask.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import com.bhaskar.synctask.presentation.create.CreateReminderDialog
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.paywall.PaywallWrapper
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditReminderWrapper(
    reminderId: String,
    onDismiss: () -> Unit
) {
    // Get ViewModel
    val viewModel: CreateReminderViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Get groups and tags from ViewModel
    val groups by viewModel.groups.collectAsState()
    val tags by viewModel.tags.collectAsState()
    
    var showPaywall by remember { mutableStateOf(false) }

    // Load the reminder
    LaunchedEffect(reminderId) {
        viewModel.loadReminder(reminderId)
    }

    if (showPaywall) {
        PaywallWrapper(
            onDismiss = { showPaywall = false }
        )
    } else {
        CreateReminderDialog(
            state = state,
            onEvent = viewModel::onEvent,
            groups = groups,
            tags = tags,
            onNavigateToSubscription = { showPaywall = true },
            onDismiss = onDismiss
        )
    }
}