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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.bhaskar.synctask.presentation.onboarding.OnboardingViewModel
import com.bhaskar.synctask.presentation.onboarding.OnboardingEvent
import com.bhaskar.synctask.presentation.onboarding.WelcomeScreen
import com.bhaskar.synctask.presentation.onboarding.OnboardingScreen
import com.bhaskar.synctask.presentation.onboarding.NotificationPermissionScreen
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

@Composable
fun App() {
    val authManager: AuthManager = koinInject()
    val authState by authManager.authState.collectAsState()
    val notificationScheduler: NotificationScheduler = koinInject()
    
    val themeRepository: ThemeRepository = koinInject()
    val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    
    val onboardingViewModel: OnboardingViewModel = koinViewModel()
    val onboardingState by onboardingViewModel.state.collectAsState()

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
            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    when (initialState) {
                        is AuthState.Unauthenticated if targetState is AuthState.Authenticated -> {
                            // Login -> Main: Login slides down (out), Main fades in (or stays)
                            fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                                    slideOutVertically(
                                        targetOffsetY = { fullHeight -> fullHeight },
                                        animationSpec = tween(durationMillis = 500)
                                    )
                        }

                        is AuthState.Authenticated if targetState is AuthState.Unauthenticated -> {
                            // Logout: Main fades out, Login slides up (in)
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 500)
                            ) togetherWith fadeOut(animationSpec = tween(durationMillis = 300))
                        }

                        else -> {
                            // Default fade
                            fadeIn() togetherWith fadeOut()
                        }
                    }
                }
            ) { targetState ->
                when (targetState) {
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        if (onboardingState.isCompleted == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            UnauthenticatedNavHost(
                                startDestination = if (onboardingState.isCompleted == true) MainRoutes.LoginScreen else MainRoutes.WelcomeScreen,
                                viewModel = onboardingViewModel
                            )
                        }
                    }
                    is AuthState.Authenticated -> {
                        MainAppContent(notificationScheduler)
                    }
                }
            }
        }
    }
}

@Composable
fun UnauthenticatedNavHost(
    startDestination: MainRoutes,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    val scope = rememberCoroutineScope()

    BindEffect(permissionsController)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        composable<MainRoutes.WelcomeScreen> {
            WelcomeScreen(
                onNavigateNext = {
                    navController.navigate(MainRoutes.OnboardingScreen)
                }
            )
        }
        composable<MainRoutes.OnboardingScreen> {
            OnboardingScreen(
                onNavigateNext = {
                    scope.launch {
                        val isGranted = try {
                            permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                        } catch (e: Exception) {
                            false
                        }

                        if (isGranted) {
                            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                            navController.navigate(MainRoutes.LoginScreen) {
                                popUpTo(MainRoutes.WelcomeScreen) { inclusive = true }
                            }
                        } else {
                            navController.navigate(MainRoutes.NotificationPermissionScreen)
                        }
                    }
                }
            )
        }
        composable<MainRoutes.NotificationPermissionScreen> {
            NotificationPermissionScreen(
                onPermissionResult = {
                    viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                    navController.navigate(MainRoutes.LoginScreen) {
                        popUpTo(MainRoutes.WelcomeScreen) { inclusive = true }
                    }
                }
            )
        }
        composable<MainRoutes.LoginScreen> {
            LoginScreen(
                onLoginSuccess = {
                    // AuthState change handles navigation
                }
            )
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
        composable<MainRoutes.SettingsScreen>(
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) },
            popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }) }
        ) {
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