package com.bhaskar.synctask

import ReminderDetailScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.platform.NotificationScheduler
import com.bhaskar.synctask.presentation.list.ReminderListScreen
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.create.CreateReminderScreen
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.detail.ReminderDetailViewModel
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.recurrence.CustomRecurrenceScreen
import com.bhaskar.synctask.presentation.recurrence.CustomRecurrenceViewModel
import com.bhaskar.synctask.presentation.settings.SettingsScreen
import com.bhaskar.synctask.presentation.utils.MainRoutes
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

@Composable
fun App() {

    val notificationScheduler: NotificationScheduler = koinInject()

    LaunchedEffect(Unit) {
        notificationScheduler.scheduleNext()
    }

    SyncTaskTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            val createReminderViewModel: CreateReminderViewModel = koinViewModel()
            val createReminderState by createReminderViewModel.state.collectAsState()

            val reminderDetailViewModel: ReminderDetailViewModel = koinViewModel()
            val reminderDetailState by reminderDetailViewModel.state.collectAsState()

            val reminderListViewModel: ReminderListViewModel = koinViewModel()
            val reminderListState by reminderListViewModel.state.collectAsState()

            val customRecurrenceViewModel: CustomRecurrenceViewModel = koinViewModel()
            val customRecurrenceState by customRecurrenceViewModel.state.collectAsState()


            NavHost(
                navController = navController,
                startDestination = MainRoutes.ReminderListScreen,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                composable<MainRoutes.ReminderListScreen>() {
                    ReminderListScreen(
                        reminderListState = reminderListState,
                        onReminderScreenEvent = reminderListViewModel::onEvent,
                        onNavigateToCreate = {
                            navController.navigate(MainRoutes.CreateReminderScreen())
                        },
                        onNavigateToDetail = { reminderId ->
                            navController.navigate(MainRoutes.ReminderDetailScreen(reminderId))
                        },
                        onNavigateToSettings = {
                            navController.navigate(MainRoutes.SettingsScreen)
                        }
                    )
                }
                composable<MainRoutes.CreateReminderScreen> { backStackEntry ->
                    val args = backStackEntry.toRoute<MainRoutes.CreateReminderScreen>()
                    val reminderId = args.id
                    
                    LaunchedEffect(reminderId) {
                        if (reminderId != null) {
                            createReminderViewModel.loadReminder(reminderId)
                        } else {
                            createReminderViewModel.resetState()
                        }
                    }

                    CreateReminderScreen(
                        createReminderState = createReminderState,
                        onCreateReminderEvent = createReminderViewModel::onEvent,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCustomRecurrence = {
                            navController.navigate(MainRoutes.CustomRecurrenceScreen)
                        },
                        navController = navController
                    )
                }
                composable<MainRoutes.ReminderDetailScreen>() { navBackSTackEntry ->
                    val reminderId = navBackSTackEntry.toRoute<MainRoutes.ReminderDetailScreen>().id
                    LaunchedEffect(true) {
                        println("Loading reminder with ID: $reminderId")
                        println("is reminder there(navigation): ${reminderDetailState.allReminders.any { it.id == reminderId }}")
                    }
                    ReminderDetailScreen(
                        reminderDetailState = reminderDetailState,
                        onReminderDetailEvent = reminderDetailViewModel::onEvent,
                        reminderId = reminderId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToEdit = { id ->
                            navController.navigate(MainRoutes.CreateReminderScreen(id = id))
                        }
                    )
                }
                composable<MainRoutes.CustomRecurrenceScreen>() {
                    CustomRecurrenceScreen(
                        customRecurrenceState = customRecurrenceState,
                        onCustomRecurrenceEvent = customRecurrenceViewModel::onEvent,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRuleConfirmed = { rule ->
                            // Pass result back serializing to String
                            val json = Json.encodeToString(RecurrenceRule.serializer(), rule)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("recurrence_rule", json)
                            navController.popBackStack()
                        }
                    )
                }
                composable<MainRoutes.SettingsScreen>() {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}