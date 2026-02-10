package com.bhaskar.synctask

import BottomNavHost
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
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.settings.SettingsScreen
import com.bhaskar.synctask.presentation.settings.SettingsViewModel
import com.bhaskar.synctask.presentation.utils.MainRoutes
import com.bhaskar.synctask.presentation.theme.SystemAppearance
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.bhaskar.synctask.domain.repository.ThemeRepository
import com.bhaskar.synctask.domain.model.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme


@Composable
fun App() {
    val authManager: AuthManager = koinInject()
    val authState by authManager.authState.collectAsState()
    val notificationScheduler: NotificationScheduler = koinInject()
    
    val themeRepository: ThemeRepository = koinInject()
    val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    
    val isDarkTheme = when(themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    SystemAppearance(isDark = isDarkTheme)

    LaunchedEffect(Unit) {
        notificationScheduler.scheduleNext()
    }

    SyncTaskTheme(darkTheme = isDarkTheme) {
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

        // Settings Screen
        composable<MainRoutes.SettingsScreen> {
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