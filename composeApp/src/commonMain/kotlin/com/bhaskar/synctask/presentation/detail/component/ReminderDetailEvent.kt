package com.bhaskar.synctask.presentation.detail.component

sealed class ReminderDetailEvent {
    data object OnToggleComplete : ReminderDetailEvent()
    data class OnDelete(val reminderId: String) : ReminderDetailEvent()
    data object OnEdit : ReminderDetailEvent()
}
