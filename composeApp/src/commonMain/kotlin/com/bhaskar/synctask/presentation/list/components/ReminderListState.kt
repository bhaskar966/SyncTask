package com.bhaskar.synctask.presentation.list.components

import com.bhaskar.synctask.domain.model.Reminder

data class ReminderListState(
    val pinnedReminders: List<Reminder> = emptyList(),
    val overdueReminders: List<Reminder> = emptyList(),
    val todayReminders: List<Reminder> = emptyList(),
    val tomorrowReminders: List<Reminder> = emptyList(),
    val laterReminders: List<Reminder> = emptyList(),
    val snoozedReminders: List<Reminder> = emptyList(),
    val missedReminders: List<Reminder> = emptyList(),
    val completedReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val syncDeviceCount: Int = 0,
    val selectedFilter: ReminderFilter = ReminderFilter.ACTIVE,
    val expandedSections: Set<String> = setOf("pinned", "overdue", "today"),
    val showPremiumDialog: Boolean = false,
    val premiumDialogMessage: String = "",
    val isMaxLimitReached: Boolean = false,
    val is24HourFormat: Boolean = false
)