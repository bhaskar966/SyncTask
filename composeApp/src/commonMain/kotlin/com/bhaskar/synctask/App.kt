package com.bhaskar.synctask

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.presentation.list.ReminderListScreen
import com.bhaskar.synctask.presentation.theme.SyncTaskTheme
import com.bhaskar.synctask.presentation.create.CreateReminderScreen
import com.bhaskar.synctask.presentation.detail.ReminderDetailScreen
import com.bhaskar.synctask.presentation.recurrence.CustomRecurrenceScreen
import com.bhaskar.synctask.presentation.settings.SettingsScreen
import kotlinx.serialization.json.Json

@Composable
fun App() {
    SyncTaskTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "list",
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                composable("list") {
                    ReminderListScreen(
                        onNavigateToCreate = {
                            navController.navigate("create")
                        },
                        onNavigateToDetail = { reminderId ->
                            navController.navigate("detail/$reminderId")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        }
                    )
                }
                composable("create") {
                    CreateReminderScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCustomRecurrence = {
                            navController.navigate("custom_recurrence")
                        },
                        navController = navController
                    )
                }
                composable("detail/{reminderId}") {
                    ReminderDetailScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("custom_recurrence") {
                    CustomRecurrenceScreen(
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
                composable("settings") {
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