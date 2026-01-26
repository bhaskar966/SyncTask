package com.bhaskar.synctask.presentation.list.components

sealed class ReminderListEvent {
    data class OnSearchQueryChanged(val query: String) : ReminderListEvent()
    data class OnFilterChanged(val filter: ReminderFilter) : ReminderListEvent()
    data class OnCompleteReminder(val id: String) : ReminderListEvent()
    data class OnDeleteReminder(val id: String) : ReminderListEvent()
    data class OnSnoozeReminder(val id: String) : ReminderListEvent() // Opens snooze menu or defaults
    data object OnRefresh : ReminderListEvent()
    data object OnDismissSyncStatus : ReminderListEvent()
}
