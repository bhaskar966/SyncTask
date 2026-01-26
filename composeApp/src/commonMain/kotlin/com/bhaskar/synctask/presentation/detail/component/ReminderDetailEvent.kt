package com.bhaskar.synctask.presentation.detail.component

sealed class ReminderDetailEvent {
    data object OnToggleComplete : ReminderDetailEvent()
    data object OnDelete : ReminderDetailEvent()
    data object OnEdit : ReminderDetailEvent()
}
