package com.bhaskar.synctask.presentation.list.components

sealed interface ReminderListEvent {
    data class OnSearchQueryChanged(val query: String) : ReminderListEvent
    data class OnFilterChanged(val filter: ReminderFilter) : ReminderListEvent
    data class OnCompleteReminder(val id: String) : ReminderListEvent
    data class OnDeleteReminder(val id: String) : ReminderListEvent
    data class OnSnoozeReminder(val id: String, val minutes: Int) : ReminderListEvent
    data class OnSubtaskCheckedChange(val reminderId: String, val subtaskId: String, val isCompleted: Boolean) : ReminderListEvent
    data class OnToggleSection(val sectionId: String) : ReminderListEvent
    data class OnTogglePin(val reminder: com.bhaskar.synctask.domain.model.Reminder) : ReminderListEvent
    data object OnDismissPremiumDialog : ReminderListEvent
    data object OnRefresh : ReminderListEvent
    data object OnDismissSyncStatus : ReminderListEvent
}