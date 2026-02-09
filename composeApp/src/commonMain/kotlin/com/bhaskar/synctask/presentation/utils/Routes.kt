package com.bhaskar.synctask.presentation.utils

import kotlinx.serialization.Serializable

// Main app routes (full screen navigation)
sealed class MainRoutes {
    @Serializable
    data object BottomNavHost : MainRoutes()

    @Serializable
    data object ReminderListScreen: MainRoutes()

    @Serializable
    data class CreateReminderScreen(val id: String? = null): MainRoutes()

    @Serializable
    data object CustomRecurrenceScreen: MainRoutes()

    @Serializable
    data object SettingsScreen: MainRoutes()
    
    @Serializable
    data object PaywallScreen: MainRoutes()
}

// Bottom navigation routes (tabs)
@Serializable
sealed class BottomNavRoutes {
    @Serializable
    data object RemindersScreen : BottomNavRoutes()

    @Serializable
    data object GroupsScreen : BottomNavRoutes()

    @Serializable
    data object HistoryScreen : BottomNavRoutes()
}