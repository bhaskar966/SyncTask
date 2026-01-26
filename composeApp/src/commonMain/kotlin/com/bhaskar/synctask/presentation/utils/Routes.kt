package com.bhaskar.synctask.presentation.utils

import kotlinx.serialization.Serializable

sealed class MainRoutes {

    @Serializable
    data object ReminderListScreen: MainRoutes()

    @Serializable
    data object CreateReminderScreen: MainRoutes()

    @Serializable
    data class ReminderDetailScreen(val id: String): MainRoutes()

    @Serializable
    data object CustomRecurrenceScreen: MainRoutes()

    @Serializable
    data object SettingsScreen: MainRoutes()
}