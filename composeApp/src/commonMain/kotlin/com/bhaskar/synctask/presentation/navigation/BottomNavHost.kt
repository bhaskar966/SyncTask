package com.bhaskar.synctask.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhaskar.synctask.presentation.groups.GroupsScreen
import com.bhaskar.synctask.presentation.groups.GroupsViewModel
import com.bhaskar.synctask.presentation.history.HistoryScreen
import com.bhaskar.synctask.presentation.list.ReminderListScreen
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.utils.BottomNavRoutes
import com.bhaskar.synctask.presentation.utils.MainRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BottomNavHost(
    mainNavController: NavHostController,
    reminderListViewModel: ReminderListViewModel = koinViewModel()
) {
    val bottomNavController = rememberNavController()
    val reminderListState by reminderListViewModel.state.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = bottomNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavRoutes.RemindersScreen,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<BottomNavRoutes.RemindersScreen> {
                ReminderListScreen(
                    reminderListState = reminderListState,
                    onReminderScreenEvent = reminderListViewModel::onEvent,
                    onNavigateToCreate = {
                        mainNavController.navigate(com.bhaskar.synctask.presentation.utils.MainRoutes.CreateReminderScreen())
                    },
                    onNavigateToDetail = { reminderId ->
                        mainNavController.navigate(com.bhaskar.synctask.presentation.utils.MainRoutes.ReminderDetailScreen(reminderId))
                    },
                    onNavigateToSettings = {
                        mainNavController.navigate(com.bhaskar.synctask.presentation.utils.MainRoutes.SettingsScreen)
                    }
                )
            }

            composable<BottomNavRoutes.GroupsScreen> {

                val groupsViewModel: GroupsViewModel = koinViewModel()

                GroupsScreen(
                    viewModel = groupsViewModel,
                    onNavigateToReminder = { reminderId ->
                        mainNavController.navigate(MainRoutes.ReminderDetailScreen(reminderId))
                    },
                    onNavigateToSubscription = {
                        // TODO: Navigate to subscription screen when you create it
                        // mainNavController.navigate(MainRoutes.SubscriptionScreen)
                    }
                )
            }

            composable<BottomNavRoutes.HistoryScreen> {
                HistoryScreen(
                    reminderListState = reminderListState,
                    onReminderScreenEvent = reminderListViewModel::onEvent,
                    onNavigateToDetail = { reminderId ->
                        mainNavController.navigate(com.bhaskar.synctask.presentation.utils.MainRoutes.ReminderDetailScreen(reminderId))
                    }
                )
            }
        }
    }
}
