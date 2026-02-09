package com.bhaskar.synctask.presentation.history.components

sealed class HistoryEvent {
    data class OnTabSelected(val tab: HistoryTab) : HistoryEvent()
    data class OnSearchQueryChanged(val query: String) : HistoryEvent()
    data class OnDeleteReminder(val reminderId: String) : HistoryEvent()
}
