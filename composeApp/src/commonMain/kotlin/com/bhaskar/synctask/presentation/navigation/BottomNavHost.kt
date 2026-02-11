package com.bhaskar.synctask.presentation.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bhaskar.synctask.presentation.create.CreateReminderDialog
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.groups.GroupsScreen
import com.bhaskar.synctask.presentation.groups.GroupsViewModel
import com.bhaskar.synctask.presentation.groups.components.GroupsEvent
import com.bhaskar.synctask.presentation.history.HistoryScreen
import com.bhaskar.synctask.presentation.history.HistoryViewModel
import com.bhaskar.synctask.presentation.list.ReminderListScreen
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.navigation.components.BottomNavigationBar
import com.bhaskar.synctask.presentation.utils.BottomNavRoutes
import com.bhaskar.synctask.presentation.utils.MainRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BottomNavHost(
    mainNavController: NavHostController,
    reminderListViewModel: ReminderListViewModel = koinViewModel(),
    groupsViewModel: GroupsViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    createReminderViewModel: CreateReminderViewModel = koinViewModel()
) {
    val bottomNavController = rememberNavController()
    val reminderListState by reminderListViewModel.state.collectAsState()
    
    // Create Reminder Sheet State
    val createReminderState by createReminderViewModel.state.collectAsState()
    val groups by createReminderViewModel.groups.collectAsState()
    val tags by createReminderViewModel.tags.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    val openSheet: (String?) -> Unit = { reminderId ->
        if (reminderId != null) {
            createReminderViewModel.loadReminder(reminderId)
        } else {
            createReminderViewModel.resetState()
        }
        showSheet = true
    }
    
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val isReminders = currentDestination?.hierarchy?.any { it.hasRoute<BottomNavRoutes.RemindersScreen>() } == true
    val isGroups = currentDestination?.hierarchy?.any { it.hasRoute<BottomNavRoutes.GroupsScreen>() } == true
    
    val routeOrder = listOf(
        BottomNavRoutes.RemindersScreen,
        BottomNavRoutes.GroupsScreen,
        BottomNavRoutes.HistoryScreen
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = bottomNavController,
                showAddButton = isReminders || isGroups,
                onAddClick = {
                    if (isReminders) {
                        openSheet(null) // Open new reminder sheet
                    } else if (isGroups) {
                        groupsViewModel.onEvent(GroupsEvent.ShowCreateDialog)
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavRoutes.RemindersScreen,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                val initialIndex = routeOrder.indexOfFirst { initialState.destination.hasRoute(it::class) }
                val targetIndex = routeOrder.indexOfFirst { targetState.destination.hasRoute(it::class) }
                if (initialIndex < targetIndex) {
                    slideInHorizontally(initialOffsetX = { it })
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                }
            },
            exitTransition = {
                val initialIndex = routeOrder.indexOfFirst { initialState.destination.hasRoute(it::class) }
                val targetIndex = routeOrder.indexOfFirst { targetState.destination.hasRoute(it::class) }
                if (initialIndex < targetIndex) {
                    slideOutHorizontally(targetOffsetX = { -it })
                } else {
                    slideOutHorizontally(targetOffsetX = { it })
                }
            },
            popEnterTransition = {
                val initialIndex = routeOrder.indexOfFirst { initialState.destination.hasRoute(it::class) }
                val targetIndex = routeOrder.indexOfFirst { targetState.destination.hasRoute(it::class) }
                if (initialIndex < targetIndex) {
                    slideInHorizontally(initialOffsetX = { it })
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                }
            },
            popExitTransition = {
                val initialIndex = routeOrder.indexOfFirst { initialState.destination.hasRoute(it::class) }
                val targetIndex = routeOrder.indexOfFirst { targetState.destination.hasRoute(it::class) }
                if (initialIndex < targetIndex) {
                    slideOutHorizontally(targetOffsetX = { -it })
                } else {
                    slideOutHorizontally(targetOffsetX = { it })
                }
            }
        ) {
            composable<BottomNavRoutes.RemindersScreen> {
                ReminderListScreen(
                    reminderListState = reminderListState,
                    onReminderScreenEvent = reminderListViewModel::onEvent,
                    onNavigateToCreate = {
                        openSheet(null)
                    },
                    onNavigateToDetail = { reminderId ->
                        openSheet(reminderId)
                    },
                    onNavigateToSettings = {
                        mainNavController.navigate(MainRoutes.SettingsScreen)
                    }
                )
            }

            composable<BottomNavRoutes.GroupsScreen> {
                // Shared viewmodel instance passed from above
                val groupsState by groupsViewModel.state.collectAsState()

                GroupsScreen(
                    state = groupsState,
                    onEvent = groupsViewModel::onEvent,
                    onNavigateToReminder = { reminderId ->
                         openSheet(reminderId)
                    },
                    onNavigateToSubscription = {
                        mainNavController.navigate(MainRoutes.PaywallScreen)
                    },
                    onNavigateToSettings = {
                        mainNavController.navigate(MainRoutes.SettingsScreen)
                    }
                )
            }

            composable<BottomNavRoutes.HistoryScreen> {
                val historyState by historyViewModel.state.collectAsState()
                
                HistoryScreen(
                    state = historyState,
                    onEvent = historyViewModel::onEvent,
                    onNavigateToSettings = {
                         mainNavController.navigate(MainRoutes.SettingsScreen)
                    },
                    // Added param, need to update HistoryScreen next
                    onNavigateToReminder = { reminderId ->
                        openSheet(reminderId)
                    }
                )
            }
        }
    }
    
    // Global Create/Edit Reminder Sheet
    if (showSheet) {
        CreateReminderDialog(
            state = createReminderState,
            onEvent = createReminderViewModel::onEvent,
            groups = groups,
            tags = tags,
            onNavigateToSubscription = { mainNavController.navigate(MainRoutes.PaywallScreen) },
            onDismiss = { showSheet = false }
        )
    }
}
