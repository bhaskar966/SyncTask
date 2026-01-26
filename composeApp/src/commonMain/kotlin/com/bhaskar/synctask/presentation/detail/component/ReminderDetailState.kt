package com.bhaskar.synctask.presentation.detail.component

import com.bhaskar.synctask.domain.model.Reminder

data class ReminderDetailState(
    val reminder: Reminder? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allReminders: List<Reminder> = emptyList()
)