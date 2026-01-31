package com.bhaskar.synctask

import androidx.compose.ui.window.ComposeUIViewController
import com.bhaskar.synctask.presentation.EditReminderWrapper
import com.bhaskar.synctask.presentation.SnoozeScreen

fun MainViewController() = ComposeUIViewController { App() }

fun SnoozeViewController(reminderId: String, title: String, onDismiss: () -> Unit) = ComposeUIViewController {
    SnoozeScreen(
        reminderId = reminderId,
        title = title,
        onDismiss = onDismiss
    )
}

fun EditReminderViewController(reminderId: String, onDismiss: () -> Unit) = ComposeUIViewController {
    EditReminderWrapper(reminderId = reminderId, onDismiss = onDismiss)
}