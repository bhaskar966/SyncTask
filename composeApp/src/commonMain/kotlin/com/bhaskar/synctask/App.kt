package com.bhaskar.synctask

import ReminderDetailScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.platform.NotificationScheduler
import com.bhaskar.synctask.presentation.auth.LoginScreen
import com.bhaskar.synctask.presentation.list.ReminderListScreen
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.create.CreateReminderScreen
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.detail.ReminderDetailViewModel
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.settings.SettingsScreen
import com.bhaskar.synctask.presentation.utils.MainRoutes
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    val authManager: AuthManager = koinInject()
    val authState by authManager.authState.collectAsState()

    val notificationScheduler: NotificationScheduler = koinInject()

    LaunchedEffect(Unit) {
        notificationScheduler.scheduleNext()
    }

    SyncTaskTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (authState) {
                is AuthState.Loading -> {
                    // Show loading indicator while checking auth
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AuthState.Unauthenticated -> {
                    // Show login screen if not authenticated
                    LoginScreen(
                        onLoginSuccess = {
                            // Auth state will change automatically
                        }
                    )
                }
                is AuthState.Authenticated -> {
                    // Show main app content when authenticated
                    MainAppContent(notificationScheduler)
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(notificationScheduler: NotificationScheduler) {
    val navController = rememberNavController()
    val createReminderViewModel: CreateReminderViewModel = koinViewModel()
    val createReminderState by createReminderViewModel.state.collectAsState()
    val reminderDetailViewModel: ReminderDetailViewModel = koinViewModel()
    val reminderDetailState by reminderDetailViewModel.state.collectAsState()
    val reminderListViewModel: ReminderListViewModel = koinViewModel()
    val reminderListState by reminderListViewModel.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = MainRoutes.ReminderListScreen,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        composable<MainRoutes.ReminderListScreen> {
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

        composable<MainRoutes.ReminderDetailScreen> { navBackStackEntry ->
            val reminderId = navBackStackEntry.toRoute<MainRoutes.ReminderDetailScreen>().id

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

        composable<MainRoutes.SettingsScreen> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
