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
import com.bhaskar.synctask.presentation.paywall.PaywallWrapper
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.create.CreateReminderScreen
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.detail.ReminderDetailViewModel
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.navigation.BottomNavHost
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AuthState.Unauthenticated -> {
                    LoginScreen(
                        onLoginSuccess = {
                            // Auth state will change automatically
                        }
                    )
                }
                is AuthState.Authenticated -> {
                    MainAppContent(notificationScheduler)
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(notificationScheduler: NotificationScheduler) {
    val navController = rememberNavController()

    // ViewModels initialized once at top level
    val createReminderViewModel: CreateReminderViewModel = koinViewModel()
    val createReminderState by createReminderViewModel.state.collectAsState()
    val groups by createReminderViewModel.groups.collectAsState()
    val tags by createReminderViewModel.tags.collectAsState()

    val reminderDetailViewModel: ReminderDetailViewModel = koinViewModel()
    val reminderDetailState by reminderDetailViewModel.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = MainRoutes.BottomNavHost,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Bottom Navigation Host (contains the 3 tabs)
        composable<MainRoutes.BottomNavHost> {
            BottomNavHost(
                mainNavController = navController
            )
        }

        // Create/Edit Reminder Screen
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
                state = createReminderState,
                onEvent = createReminderViewModel::onEvent,
                groups = groups,
                tags = tags,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCustomRecurrence = {
                    navController.navigate(MainRoutes.CustomRecurrenceScreen)
                },
                onNavigateToPaywall = {
                    navController.navigate(MainRoutes.PaywallScreen)
                },
                navController = navController
            )
        }

        // Reminder Detail Screen
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

        // Settings Screen
        composable<MainRoutes.SettingsScreen> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate(MainRoutes.PaywallScreen)
                }
            )
        }
        // Paywall Screen (RevenueCat UI)
        composable<MainRoutes.PaywallScreen> {
            PaywallWrapper(
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}