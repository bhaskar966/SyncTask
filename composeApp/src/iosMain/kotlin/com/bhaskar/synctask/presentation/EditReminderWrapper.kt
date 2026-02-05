package com.bhaskar.synctask.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhaskar.synctask.presentation.create.CreateReminderScreen
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditReminderWrapper(
    reminderId: String,
    onDismiss: () -> Unit
) {
    // Create a simple NavController for this modal
    val navController = rememberNavController()

    // Get ViewModel
    val viewModel: CreateReminderViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Get groups and tags from ViewModel
    val groups by viewModel.groups.collectAsState()
    val tags by viewModel.tags.collectAsState()

    BackHandler(onBack = onDismiss)

    // Load the reminder
    LaunchedEffect(reminderId) {
        viewModel.loadReminder(reminderId)
    }

    // Simple NavHost with just the edit screen
    NavHost(
        navController = navController,
        startDestination = "edit",
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        composable("edit") {
            CreateReminderScreen(
                state = state,
                onEvent = viewModel::onEvent,
                groups = groups,
                tags = tags,
                onNavigateBack = onDismiss,
                onNavigateToCustomRecurrence = {
                    // Custom recurrence is handled inline
                },
                navController = navController
            )
        }
    }
}