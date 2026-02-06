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
import kotlinx.coroutines.launch
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
import com.bhaskar.synctask.presentation.settings.SettingsViewModel
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
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.state.collectAsState()
            
            // Create permission controller at NavHost level for Settings scope
            val factory = dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory()
            val permissionsController = remember(factory) { factory.createPermissionsController() }
            
            // Note: We need a way to pass this controller to the ViewModel if it expects it in constructor
            // However, Koin injects it. If Koin injects it, we don't pass it manually to constructor here.
            // BUT: PermissionsController usually needs to be created in Composable scope.
            // The user's previous code injected it into VM via Koin? No, it passed it manually:
            // SettingsViewModel(permissionsController = permissionsController...)
            // So we need to ensure Koin module provides it OR we pass it. 
            // The current VM definition in Modules.kt uses `viewModelOf(::SettingsViewModel)`. 
            // Koin can't auto-inject a Composable-scoped PermissionsController. 
            // LIMITATION: We must effectively "inject" the controller into the VM after creation or refactor VM.
            // Refactoring VM to NOT take controller in constructor but as method arg is safer for KMP.
            // OR: We define a generic "PermissionManager" that is a singleton, but Moko requires UI binding.
            
            // Let's look at `SettingsViewModel` constructor again. It takes `permissionsController`.
            // If we use strict `koinViewModel()`, Koin needs to know how to build `PermissionsController`.
            // It's not in Koin modules. So `koinViewModel()` will fail if we don't provide parameters.
            
            // Fix: No parameters needed now. ViewModel is clean.
            val viewModel: SettingsViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()

            SettingsScreen(
                state = state,
                onEvent = viewModel::onEvent,
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