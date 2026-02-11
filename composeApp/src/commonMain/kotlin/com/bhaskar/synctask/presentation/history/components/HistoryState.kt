package com.bhaskar.synctask.presentation.history.components

import com.bhaskar.synctask.domain.model.Reminder

data class HistoryState(
    val selectedTab: HistoryTab = HistoryTab.Completed,
    val searchQuery: String = "",
    val completedReminders: List<Reminder> = emptyList(),
    val missedReminders: List<Reminder> = emptyList(),
    val dismissedReminders: List<Reminder> = emptyList(),
    val searchResults: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val is24HourFormat: Boolean = false
)

enum class HistoryTab(val index: Int, val title: String) {
    Completed(0, "Completed"),
    Missed(1, "Missed"),
    Dismissed(2, "Dismissed");

    companion object {
        fun getByIndex(index: Int): HistoryTab = entries.getOrElse(index) { Completed }
    }
}
